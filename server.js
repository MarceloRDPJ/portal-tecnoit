// Importação dos módulos necessários
const express = require('express');
const axios = require('axios');
const cors =require('cors');
const bodyParser = require('body-parser');
const multer = require('multer');
const FormData = require('form-data');

// Inicialização do aplicativo Express
const app = express();
const port = process.env.PORT || 3001;

// Middlewares
app.use(cors());
app.use(bodyParser.json());

// Rota principal para verificar se o servidor está online
app.get('/', (req, res) => {
  res.send('Proxy TecnoiT GLPI está no ar!');
});

// Rota para iniciar uma sessão no GLPI
app.post('/api/proxy/initSession', async (req, res) => {
    const { glpiUrl, username, password, apiToken, get_full_session } = req.body;
    const appToken = process.env.GLPI_APP_TOKEN;

    if (!glpiUrl || !appToken) {
        return res.status(400).json({ message: 'URL do GLPI ou App-Token não configurado.' });
    }

    let apiUrl = `${glpiUrl}/apirest.php/initSession`;
    let headers = { 'App-Token': appToken };

    try {
        const params = new URLSearchParams();
        if (username && password) {
            params.append('login', username);
            params.append('password', password);
        }
        if (get_full_session) {
            params.append('get_full_session', 'true');
        }

        if (apiToken) {
            headers['Authorization'] = `user_token ${apiToken}`;
        } else if (!username && !password) {
            return res.status(400).json({ message: 'Credenciais são obrigatórias.' });
        }

        if (Array.from(params).length > 0) {
             apiUrl = `${apiUrl}?${params.toString()}`;
        }

        const response = await axios.get(apiUrl, { headers });
        // Return full data (session_token and session object)
        res.json(response.data);
    } catch (error) {
        res.status(error.response?.status || 500).json({
            message: 'Falha ao autenticar no GLPI.',
            error: error.response?.data || 'Erro desconhecido.'
        });
    }
});

// Rota para buscar os chamados do usuário com paginação
app.post('/api/proxy/getTickets', async (req, res) => {
    const { glpiUrl, sessionToken, range } = req.body;
    const appToken = process.env.GLPI_APP_TOKEN;
    if (!glpiUrl || !sessionToken || !appToken) return res.status(400).json({ message: 'Parâmetros obrigatórios faltando.' });

    const apiUrl = `${glpiUrl}/apirest.php/Ticket?get_my_tickets=true&range=${range || '0-49'}`;
    try {
        const response = await axios.get(apiUrl, { headers: { 'Session-Token': sessionToken, 'App-Token': appToken } });
        const totalCount = response.headers['content-range'] ? parseInt(response.headers['content-range'].split('/')[1], 10) : 0;
        res.json({ tickets: response.data, totalCount });
    } catch (error) { res.status(error.response?.status || 500).json({ message: 'Falha ao buscar chamados.' }); }
});

// Rota para buscar detalhes de um chamado
app.post('/api/proxy/getTicketDetails/:ticketId', async (req, res) => {
    const { glpiUrl, sessionToken } = req.body;
    const { ticketId } = req.params;
    const appToken = process.env.GLPI_APP_TOKEN;
    const apiUrl = `${glpiUrl}/apirest.php/Ticket/${ticketId}?with_documents=true`;

    try {
        const response = await axios.get(apiUrl, { headers: { 'Session-Token': sessionToken, 'App-Token': appToken, 'X-GLPI-Sanitized-Content': 'false' } });
        let ticketData = response.data;
        if (ticketData.content) {
            const proxyBaseUrl = "https://portal-tecnoit-glpi.onrender.com";
            ticketData.content = ticketData.content.replace(/<img src="([^"]+)"/g, (match, src) => {
                if (src.includes('/front/document.send.php')) {
                    try {
                        const url = new URL(src, glpiUrl);
                        const docId = url.searchParams.get('docid');
                        if (docId) {
                            const newSrc = `${proxyBaseUrl}/api/proxy/getDocument/${docId}?sessionToken=${sessionToken}&glpiUrl=${encodeURIComponent(glpiUrl)}`;
                            return `<img src="${newSrc}"`;
                        }
                    } catch(e) { /* Ignora */ }
                }
                return match;
            });
        }
        res.json(ticketData);
    } catch (error) { res.status(error.response?.status || 500).json({ message: 'Falha ao buscar detalhes do chamado.' }); }
});

// Rota para buscar o arquivo de um documento
app.get('/api/proxy/getDocument/:documentId', async (req, res) => {
    const { sessionToken, glpiUrl } = req.query;
    const { documentId } = req.params;
    const appToken = process.env.GLPI_APP_TOKEN;
    if (!glpiUrl || !sessionToken || !appToken || !documentId) return res.status(400).send('Parâmetros insuficientes.');

    const apiUrl = `${glpiUrl}/apirest.php/Document/${documentId}?alt=media`;
    try {
        const response = await axios.get(apiUrl, { headers: { 'Session-Token': sessionToken, 'App-Token': appToken }, responseType: 'stream' });
        res.setHeader('Content-Type', response.headers['content-type']);
        response.data.pipe(res);
    } catch (error) { res.status(error.response?.status || 500).send('Falha ao buscar o documento.'); }
});

// Rotas genéricas para sub-itens de chamados
const createSubItemRoute = (itemName, endpoint) => {
    app.post(`/api/proxy/${endpoint}/:ticketId`, async (req, res) => {
        const { glpiUrl, sessionToken } = req.body;
        const { ticketId } = req.params;
        const appToken = process.env.GLPI_APP_TOKEN;
        const apiUrl = `${glpiUrl}/apirest.php/Ticket/${ticketId}/${itemName}`;
        try {
            const response = await axios.get(apiUrl, { headers: { 'Session-Token': sessionToken, 'App-Token': appToken, 'X-GLPI-Sanitized-Content': 'false' } });
            res.json(response.data);
        } catch (error) { res.status(error.response?.status || 500).json({ message: `Falha ao buscar ${itemName}.` }); }
    });
};
createSubItemRoute('ITILFollowup', 'getTicketFollowups');
createSubItemRoute('ITILSolution', 'getTicketSolutions');
createSubItemRoute('TicketTask', 'getTicketTasks');

// Rota para buscar sessão completa (simulada via /Entity para garantir lista de entidades)
app.post('/api/proxy/getFullSession', async (req, res) => {
    const { glpiUrl, sessionToken } = req.body;
    const appToken = process.env.GLPI_APP_TOKEN;

    // GLPI API doesn't have getFullSession. We use /Entity to get the tree.
    const apiUrl = `${glpiUrl}/apirest.php/Entity`;

    try {
        // Fetch entities (recursive implies we get the tree if permissions allow)
        // We set a large range to ensure we get all relevant entities
        const response = await axios.get(apiUrl, {
            headers: { 'Session-Token': sessionToken, 'App-Token': appToken },
            params: { range: '0-1000' }
        });

        // Wrap in the structure expected by index.html: { session: { glpimy_entities: [...] } }
        // The /Entity endpoint returns an array of entity objects, which matches the expected format for glpimy_entities.
        res.json({ session: { glpimy_entities: response.data } });
    } catch (error) {
        console.error("Error in getFullSession proxy:", error.message);
        res.status(error.response?.status || 500).json({ message: 'Falha ao buscar entidades.' });
    }
});

// Rota para mudar a entidade ativa
app.post('/api/proxy/changeActiveEntity', async (req, res) => {
    const { glpiUrl, sessionToken, entities_id } = req.body;
    const appToken = process.env.GLPI_APP_TOKEN;

    if (!glpiUrl || !sessionToken || !entities_id) {
        return res.status(400).json({ message: 'Parâmetros obrigatórios faltando.' });
    }

    const apiUrl = `${glpiUrl}/apirest.php/changeActiveEntity`;
    try {
        const response = await axios.post(apiUrl,
            { entities_id: entities_id },
            { headers: { 'Session-Token': sessionToken, 'App-Token': appToken } }
        );
        res.status(200).json(response.data);
    } catch (error) {
        console.error("Error changing active entity:", error.message);
        res.status(error.response?.status || 500).json({ message: 'Falha ao mudar entidade.' });
    }
});

// Rotas para adicionar itens a um chamado
const createAddItemRoute = (itemName, endpointName) => {
    app.post(`/api/proxy/${endpointName}`, async (req, res) => {
        const { glpiUrl, sessionToken, ticketId, content } = req.body;
        const appToken = process.env.GLPI_APP_TOKEN;
        const apiUrl = `${glpiUrl}/apirest.php/${itemName}`;
        try {
            const response = await axios.post(apiUrl, { input: { content: content, items_id: ticketId, itemtype: 'Ticket' } }, { headers: { 'Session-Token': sessionToken, 'App-Token': appToken } });
            res.status(201).json(response.data);
        } catch (error) { res.status(error.response?.status || 500).json({ message: `Falha ao adicionar ${itemName}.` }); }
    });
};
createAddItemRoute('ITILFollowup', 'addFollowup');
createAddItemRoute('ITILSolution', 'addSolution');

app.post('/api/proxy/addTicketTask', async (req, res) => {
    const { glpiUrl, sessionToken, ticketId, content, actiontime } = req.body;
    const appToken = process.env.GLPI_APP_TOKEN;
    const apiUrl = `${glpiUrl}/apirest.php/TicketTask`;
    try {
        const taskData = { input: { content, tickets_id: ticketId, actiontime: actiontime * 60 } };
        const response = await axios.post(apiUrl, taskData, { headers: { 'Session-Token': sessionToken, 'App-Token': appToken } });
        res.status(201).json(response.data);
    } catch (error) { res.status(error.response?.status || 500).json({ message: 'Falha ao adicionar apontamento.' }); }
});

// Rota para atualizar um chamado
app.post('/api/proxy/updateTicket/:ticketId', async (req, res) => {
    const { glpiUrl, sessionToken, updateData } = req.body;
    const { ticketId } = req.params;
    const appToken = process.env.GLPI_APP_TOKEN;
    const apiUrl = `${glpiUrl}/apirest.php/Ticket/${ticketId}`;
    try {
        const response = await axios.put(apiUrl, { input: updateData }, { headers: { 'Session-Token': sessionToken, 'App-Token': appToken } });
        res.status(200).json(response.data);
    } catch (error) { res.status(error.response?.status || 500).json({ message: 'Falha ao atualizar o chamado.' }); }
});

// Configuração do Multer para upload de arquivos em memória
const upload = multer({ storage: multer.memoryStorage() });

// Rota para fazer upload de um documento para um chamado
app.post('/api/proxy/uploadDocument/:ticketId', upload.single('file'), async (req, res) => {
    const { glpiUrl, sessionToken } = req.body; // sessionToken will be passed in the body
    const { ticketId } = req.params;
    const appToken = process.env.GLPI_APP_TOKEN;

    if (!req.file) {
        return res.status(400).json({ message: 'Nenhum arquivo enviado.' });
    }

    const apiUrl = `${glpiUrl}/apirest.php/Document`;

    try {
        const form = new FormData();

        const manifest = {
            input: {
                name: req.file.originalname,
                tickets_id: parseInt(ticketId, 10), // Ensure ticketId is an integer
                _filename: req.file.originalname
            }
        };

        form.append('uploadManifest', JSON.stringify(manifest), { contentType: 'application/json' });
        form.append('file', req.file.buffer, { filename: req.file.originalname });

        const response = await axios.post(apiUrl, form, {
            headers: {
                ...form.getHeaders(),
                'Session-Token': sessionToken,
                'App-Token': appToken,
            }
        });

        res.status(201).json(response.data);
    } catch (error) {
        console.error('GLPI Upload Error:', error.response?.data || error.message);
        res.status(error.response?.status || 500).json({
            message: 'Falha ao fazer upload do documento.',
            error: error.response?.data || 'Erro desconhecido.'
        });
    }
});

// Rota para buscar consumíveis (getConsumables)
app.post('/api/proxy/getConsumables', async (req, res) => {
    const { glpiUrl, sessionToken } = req.body;
    const appToken = process.env.GLPI_APP_TOKEN;
    const apiUrl = `${glpiUrl}/apirest.php/ConsumItem`;

    try {
        // Add is_recursive to get parent entity items too
        const response = await axios.get(apiUrl, {
            headers: { 'Session-Token': sessionToken, 'App-Token': appToken },
            params: { range: '0-1000', is_recursive: 1 }
        });

        const stock = response.data.map(item => ({
            id: item.id,
            name: item.name,
            ref: item.ref
        }));
        res.json(stock);
    } catch (error) {
        console.error("Error fetching consumables:", error.message);
        res.json([]);
    }
});

// Rota Unificada: Responder Chamado e Consumir Itens
app.post('/api/proxy/respondTicket', async (req, res) => {
    const { glpiUrl, sessionToken, ticketId, content, status, stockItems, entities_id } = req.body;
    const appToken = process.env.GLPI_APP_TOKEN;

    if (!ticketId || !content) {
        return res.status(400).json({ message: 'Dados da resposta incompletos.' });
    }

    try {
        // 1. Add Solution or Followup based on status
        // Status 5 = Solved (Solucionado), 6 = Closed.
        // Using 5 for "Solucionado" and 2 (Assign) or 4 (Pending) for "Aguardar".
        // Let's assume frontend sends specific intent.

        const isSolution = (status === 5 || status === '5' || status === 'solucionado');
        const itemType = isSolution ? 'ITILSolution' : 'ITILFollowup';

        const addUrl = `${glpiUrl}/apirest.php/Ticket/${ticketId}/${itemType}`;
        await axios.post(addUrl, {
            input: {
                content: content,
                items_id: ticketId,
                itemtype: 'Ticket'
            }
        }, { headers: { 'Session-Token': sessionToken, 'App-Token': appToken } });

        // 2. Update Ticket Status
        if (status) {
             const updateUrl = `${glpiUrl}/apirest.php/Ticket/${ticketId}`;
             await axios.put(updateUrl, {
                 input: { status: status }
             }, { headers: { 'Session-Token': sessionToken, 'App-Token': appToken } });
        }

        // 3. Handle Stock Items (Consumables)
        if (stockItems && stockItems.length > 0) {
            for (const item of stockItems) {
                try {
                    let itemId = item.id;

                    // A. Create Item if it's custom
                    if (String(itemId).startsWith('custom-')) {
                        const createItemUrl = `${glpiUrl}/apirest.php/ConsumItem`;
                        // Ensure we have an entities_id. If not passed, might default to session's active entity.
                        // Best to pass the ticket's entity or the user's active one.
                        const payload = {
                            input: {
                                name: item.name,
                                is_recursive: 1
                            }
                        };
                        if (entities_id) payload.input.entities_id = entities_id;

                        const createRes = await axios.post(createItemUrl, payload,
                            { headers: { 'Session-Token': sessionToken, 'App-Token': appToken } }
                        );

                        if (createRes.data && createRes.data.id) {
                            itemId = createRes.data.id;
                        } else {
                            console.error("Failed to create ConsumItem:", item.name);
                            continue;
                        }
                    }

                    // B. Link to Ticket (Item_Ticket)
                    const linkUrl = `${glpiUrl}/apirest.php/Item_Ticket`;
                    await axios.post(linkUrl, {
                        input: {
                            tickets_id: ticketId,
                            itemtype: 'ConsumItem',
                            items_id: itemId,
                            amount: item.qty || 1
                        }
                    }, { headers: { 'Session-Token': sessionToken, 'App-Token': appToken } });

                } catch (innerError) {
                    console.error(`Error processing stock item ${item.name}:`, innerError.response?.data || innerError.message);
                }
            }
        }

        res.status(200).json({ message: 'Resposta enviada com sucesso.' });

    } catch (error) {
        console.error("Error responding ticket:", error.response?.data || error.message);
        res.status(error.response?.status || 500).json({
            message: 'Falha ao responder chamado.',
            error: error.response?.data || error.message
        });
    }
});

// Inicia o servidor
app.listen(port, () => {
  console.log(`Servidor proxy rodando na porta ${port}`);
});
