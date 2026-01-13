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
    const { glpiUrl, username, password, apiToken } = req.body;
    const appToken = process.env.GLPI_APP_TOKEN;

    if (!glpiUrl || !appToken) {
        return res.status(400).json({ message: 'URL do GLPI ou App-Token não configurado.' });
    }

    let apiUrl = `${glpiUrl}/apirest.php/initSession`;
    let headers = { 'App-Token': appToken };

    try {
        if (apiToken) {
            headers['Authorization'] = `user_token ${apiToken}`;
        } else if (username && password) {
            const params = new URLSearchParams({ login: username, password: password }).toString();
            apiUrl = `${apiUrl}?${params}`;
        } else {
            return res.status(400).json({ message: 'Credenciais são obrigatórias.' });
        }

        const response = await axios.get(apiUrl, { headers });
        res.json({ session_token: response.data.session_token });
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

// Rota para buscar sessão completa
app.post('/api/proxy/getFullSession', async (req, res) => {
    const { glpiUrl, sessionToken } = req.body;
    const appToken = process.env.GLPI_APP_TOKEN;
    const apiUrl = `${glpiUrl}/apirest.php/getFullSession`;
    try {
        const response = await axios.get(apiUrl, { headers: { 'Session-Token': sessionToken, 'App-Token': appToken } });
        res.json({ session: response.data.session });
    } catch (error) { res.status(error.response?.status || 500).json({ message: 'Falha ao buscar sessão.' }); }
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
    // Note: GLPI API for Consumables might vary. Assuming generic Entity restriction or all.
    // Using simple getAll for now or search
    const apiUrl = `${glpiUrl}/apirest.php/ConsumItem?range=0-100`;

    try {
        const response = await axios.get(apiUrl, { headers: { 'Session-Token': sessionToken, 'App-Token': appToken } });
        // Map to simpler format if needed, or pass through
        const stock = response.data.map(item => ({
            id: item.id,
            name: item.name,
            ref: item.ref
        }));
        res.json(stock);
    } catch (error) {
         // Fallback to empty array to not break UI if API fails or no permissions
        console.error("Error fetching consumables:", error.message);
        res.json([]);
    }
});

// Rota para criar chamado (createTicket)
app.post('/api/proxy/createTicket', async (req, res) => {
    const { glpiUrl, sessionToken, input, stockItems } = req.body;
    const appToken = process.env.GLPI_APP_TOKEN;

    if (!input || !input.name || !input.content || !input.entities_id) {
        return res.status(400).json({ message: 'Dados do chamado incompletos.' });
    }

    const ticketUrl = `${glpiUrl}/apirest.php/Ticket`;

    try {
        // 1. Create Ticket
        const ticketResponse = await axios.post(ticketUrl, { input: input }, {
            headers: { 'Session-Token': sessionToken, 'App-Token': appToken }
        });

        const ticketId = ticketResponse.data.id;

        // 2. Handle Stock Items (if any logic needed on GLPI side, e.g. decreasing stock)
        // Currently the frontend just passes them for the record in content,
        // but if we needed to consume them in GLPI, we would loop here.
        // For now, we assume the content string already contains the info.

        // However, the frontend might expect us to log this or just considers it done.
        // If we need to add a "Consumable" to the ticket, we'd use /Ticket/{id}/Item_Ticket
        // But the frontend already formatted the HTML content.

        res.status(201).json({ id: ticketId, message: 'Chamado criado com sucesso.' });

    } catch (error) {
        console.error("Error creating ticket:", error.response?.data || error.message);
        res.status(error.response?.status || 500).json({
            message: 'Falha ao criar chamado.',
            error: error.response?.data || error.message
        });
    }
});

// Inicia o servidor
app.listen(port, () => {
  console.log(`Servidor proxy rodando na porta ${port}`);
});
