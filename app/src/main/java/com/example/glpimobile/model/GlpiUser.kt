package com.example.glpimobile.model

data class GlpiUser(
    val name: String,
    val email: String,
    val department: String,
    val title: String,
    val phone: String,
    val city: String
) {
    /** Iniciais para avatar */
    val initials: String get() {
        val parts = name.trim().split(" ")
        return when {
            parts.size >= 2 -> "${parts[0].firstOrNull() ?: ""}${parts[1].firstOrNull() ?: ""}".uppercase()
            parts.size == 1 -> parts[0].take(2).uppercase()
            else -> "?"
        }
    }
}

object UserRepository {
    val allUsers: List<GlpiUser> = listOf(
        GlpiUser("Abelardo Neto","abelardo.neto@tecnoit.com.br","Obras","Supervisor de Campo","(11) 98603-9859",""),
        GlpiUser("Alberto Silva","alberto.silva@tecnoit.com.br","Engenharia","Supervisor de Campo","62981825550","Goiânia"),
        GlpiUser("Alerta Tecno it","alerta@tecnoit.com.br","","","",""),
        GlpiUser("Alexandre Gomes","alexandre.gomes@tecnoit.com.br","Comercial","Analista de Pre Vendas","75 8828-5057",""),
        GlpiUser("Alexandre Lopes dos Santos","alexandre.lopes@tecnoit.com.br","Engenharia","Técnico de cabeamento estruturado","31 8832-0677",""),
        GlpiUser("Allisson Dillan do Nascimento Melo","Allisson.melo@tecnoit.com.br","TI","Coordenador de Suporte","61 98418-1367",""),
        GlpiUser("Allyne Muniz da Conceição","Allyne.Muniz@tecnoit.com.br","Financeiro","Analista financeira","62 9291-0933",""),
        GlpiUser("Ana Luiza Dias","ana.dias@tecnoit.com.br","Administrativo CEO","Assessora de Assuntos Executivos e Comunicação Interna","(62) 99460-4902","Goiânia"),
        GlpiUser("Anderson Gomes da Silva","anderson.gomes@tecnoit.com.br","Engenharia","Técnico de Segurança do Trabalho","",""),
        GlpiUser("Anderson Pereira Nogueira","Anderson.nogueira@tecnoit.com.br","TI","Analista de TI","61 9928-2472",""),
        GlpiUser("Andre Felipe Rodrigues do Carmo","Andre.carmo@tecnoit.com.br","Administrativo financeiro","Analista de compras","62 9319-3450",""),
        GlpiUser("Ariane Ferreira","ariane.ferreira@tecnoit.com.br","Supply Chain","Analista de Compras","(62)  99231-8102","Goiânia"),
        GlpiUser("Bianca Cristine de Oliveira Costa","bianca.costa@tecnoit.com.br","RECURSOS HUMANOS","GERENTE RECURSOS HUMANOS","62999591505",""),
        GlpiUser("Cadastro Fornecedor","cadastrofornecedores@tecnoit.com.br","","","",""),
        GlpiUser("Camila Oliveira","camila.oliveira@tecnoit.com.br","PMO","Coordenador de Projetos","(31) 99347-8263","Belo Horizonte"),
        GlpiUser("Camila Peleja","camila.peleja@tecnoit.com.br","Recursos Humanos","Analista de Recursos Humanos","(62) 99513-1458","Goiânia"),
        GlpiUser("Carlos Antônio Silva dos Santos","carlos.santos@tecnoit.com.br","","","",""),
        GlpiUser("Carlos Roberto Sena","carlos.sena@tecnoit.com.br","Engenharia","Técnico de cabeamento estruturado","62 9651-7273","Goiania"),
        GlpiUser("Cesar Pinheiro dos Santos","CESAR.SANTOS@tecnoit.com.br","Engenharia","Técnico de cabeamento estruturado","61 9313-5042",""),
        GlpiUser("Chamados - Tecno IT","chamados@tecnoit.com.br","Suporte","","",""),
        GlpiUser("Chamados Tecnoit","chamados.tecnoit@tecnoit.com.br","","","",""),
        GlpiUser("Claudinei Dias Martins","claudinei.martins@tecnoit.com.br","","TECNICO DE CABEAMENTO ESTRUTURADO","","Goiania"),
        GlpiUser("Cleberson Silva","cleberson.silva@tecnoit.com.br","Engenharia","Coordenador de Obras","6299268-5395","Goiania"),
        GlpiUser("Comercial Cotações","comercialcotacoes@tecnoit.com.br","Comercial","","",""),
        GlpiUser("compliance Tecno IT","compliance@tecnoit.com.br","Compliance","Compliance","",""),
        GlpiUser("Danielly Santos","danielly.santos@tecnoit.com.br","Engenharia","Projetista","( 62 ) 98483-0901","Goiânia"),
        GlpiUser("Dashboard Compras","dashboardcompras@tecnoit.com.br","Supply Chain","Perfil criado para o compras","",""),
        GlpiUser("Denúncia","denuncia@tecnoit.com.br","","","",""),
        GlpiUser("Departamento de Inovação","DepartamentodeInovao@tecnoit.com.br","","","",""),
        GlpiUser("Diego Silva","diego.silva@tecnoit.com.br","Service Desk","Técnico de Cabeamento Estruturado","6298176-8138","Goiânia"),
        GlpiUser("Diogo Ferreira da Silva","diogo.silva@tecnoit.com.br","","","",""),
        GlpiUser("Douglas Santos Nunes Ribeiro","Douglas.Ribeiro@tecnoit.com.br","Engenharia","Auxiliar técnico de cabeamento","61 9299-2167",""),
        GlpiUser("Edson Carlos Souza Silva","Edson.silva@tecnoit.com.br","","Instalador de infraestrutura","",""),
        GlpiUser("Edson Fernando Lima","Edson.lima@tecnoit.com.br","Engenharia","Auxiliar técnico de cabeamento","",""),
        GlpiUser("Edson Monteiro","edson.monteiro@tecnoit.com.br","Obras","Supervisor de Campo","(62) 9 8286-3602","Goiânia"),
        GlpiUser("Eduardo Paes","eduardo.paes@tecnoit.com.br","Operações","Planejador","(62)99606-1199",""),
        GlpiUser("Edvanilton Nascimento Santos","Edvanilton.santos@tecnoit.com.br","Engenharia","Auxiliar técnico de cabeamento","79 9642-4290",""),
        GlpiUser("Élem Souza","elem.souza@tecnoit.com.br","Financeiro","Analista Financeiro","(62) 99460-4880","Goiânia"),
        GlpiUser("Emanuel Luiz Damascena","Emanuel.damascena@tecnoit.com.br","Engenharia","Instalador de infraestrutura","",""),
        GlpiUser("Espedito Soares Santos","espedito.santos@tecnoit.com.br","Engenharia","Técnico de cabeamento estruturado","31 9223-3288",""),
        GlpiUser("Ester Piccirilli","ester.franca@tecnoit.com.br","Comercial","Coordenadora Comercial","62 8328-0154","Goiânia"),
        GlpiUser("Felipe Castro de Oliveira","felipe.oliveira@tecnoit.com.br","","Analista de pre Vendas","",""),
        GlpiUser("Gabriel Cabral","gabriel.cabral@tecnoit.com.br","Engenharia","Analista de Implantação","(37) 99928-1245","Nova Lima"),
        GlpiUser("Gabriel Soares","gabriel.soares@tecnoit.com.br","Pré Vendas","Analista de Pré Vendas","61 9 9157-8891","Goiânia"),
        GlpiUser("Giovanna Faleiro","giovanna.faleiro@tecnoit.com.br","Comercial","Assistente Administrativo","","Goiânia"),
        GlpiUser("Giselly Maria Pereira Barbosa","Giselly.barbosa@tecnoit.com.br","Administrativo Financeiro","Analista de faturamento","62 98315-1881",""),
        GlpiUser("Ibrahim Boufleur","ibra@tecnoit.com.br","Diretoria","CEO","+55 62 9 8254-8000","Goiânia"),
        GlpiUser("Jacques Gomes Barbosa","jacques.barbosa@tecnoit.com.br","","TECNICO DE CABEAMENTO ESTRUTURADO","","Paulo Afonso"),
        GlpiUser("Jair Prado de Medeiros Filho","jair.filho@tecnoit.com.br","Engenharia","Técnico de Cabeamento Estruturado","31 9847-0766",""),
        GlpiUser("Jean Santana","jean.santana@tecnoit.com.br","Operação","Supervisor de Campo","","Brasilia"),
        GlpiUser("Jefte Ruben De Oliveira","Jefte.oliveira@tecnoit.com.br","","Supervisor de campo BSB","",""),
        GlpiUser("Jesuslino Jose Freire","jesuslino.freire@tecnoit.com.br","","Auxiliar de Estoque","",""),
        GlpiUser("Jhonathan Felipe Neves","jhonathan.neves@tecnoit.com.br","Engenharia","Técnico de cabeamento estruturado","31 8321-1010",""),
        GlpiUser("Jhones Beckman de Sousa","Jhones.sousa@tecnoit.com.br","Engenharia","Técnico de cabeamento estruturado","",""),
        GlpiUser("João Paulo Gomes de Jesus","joao.jesus@tecnoit.com.br","Supply Chain","Auxiliar de Logística","","Goiânia"),
        GlpiUser("Jonas João da Silva","Jonas.joao@tecnoit.com.br","Engenharia","Instalador de Infraestrutura","75 98807-2572",""),
        GlpiUser("Jose Leite de Souza","jose.souza@tecnoit.com.br","Engenharia","Auxiliar Tecnico","79 8811-4537",""),
        GlpiUser("Joseildo Elias da Silva","joseildo.silva@tecnoit.com.br","Engenharia","Auxiliar Técnico","75 98852-0055","Paulo Afonso"),
        GlpiUser("Júlio Junior","julio.junior@tecnoit.com.br","Service Desk","Técnico de cabeamento estruturado","31 9725-4824","Minas Gerais"),
        GlpiUser("Leilson Silva","leilson.silva@tecnoit.com.br","Engenharia","Técnico de cabeamento estruturado","",""),
        GlpiUser("Leonardo Prado","leonardo.prado@tecnoit.com.br","ENGENHARIA","ENGENHEIRO DE REDES - MG","35 8808-1348","MG"),
        GlpiUser("Lucas Pereira Cardoso","lucas.cardoso@tecnoit.com.br","Tecnologia da Informação","Analista de Suporte","",""),
        GlpiUser("Lucas Rodrigues Martins","Lucas.martins@tecnoit.com.br","Engenharia","Auxiliar técnico de cabeamento","61 9194-2239",""),
        GlpiUser("Luizmar Almeida","luizmar.almeida@tecnoit.com.br","Comercial","Gerente de Canais","62 9 8328-0174","Goiânia"),
        GlpiUser("Marcelo Rodrigues dos Passo Junior","marcelo.junior@tecnoit.com.br","","Analista de Suporte","",""),
        GlpiUser("Marcos Santos","marcos.santos@tecnoit.com.br","","","",""),
        GlpiUser("Mario Luiz Dos Santos","Mario.Santos@tecnoit.com.br","","Instalador de infraestrutura","75 99140-2348",""),
        GlpiUser("Matheus Sousa","matheus.sousa@tecnoit.com.br","Obras","Técnico de Cabeamento Estruturado","61 9 9558-0618","Goiânia"),
        GlpiUser("Matheus Vilela Torres","Matheus.torres@tecnoit.com.br","Engenharia","Instalador de infraestrutura","",""),
        GlpiUser("Milton Alves dos Santos","milton.santos@tecnoit.com.br","Engenharia","Auxiliar Técnico","48 8455-0621",""),
        GlpiUser("Murillo França","murillo.franca@tecnoit.com.br","Engenharia","Coordenador de TI","",""),
        GlpiUser("Nasser Mourad Filho","nasser.filho@tecnoit.com.br","","","",""),
        GlpiUser("Pablo Oliveira","pablo.oliveira@tecnoit.com.br","Service Desk","Analista de Suporte","62 9 9432-1879","Goiânia"),
        GlpiUser("Paulo Cesar Soares","Paulo.Soares@tecnoit.com.br","Administrativo financeiro","Assistente administrativo","62 98513-0346",""),
        GlpiUser("Paulo Gerson da Cunha Gonçalves","Paulo.gerson@tecnoit.com.br","Engenharia","Auxiliar técnico de cabeamento","61 9164-5212",""),
        GlpiUser("Pedro Henrique Gomes Barbosa dos Santos","Pedro.Santos@tecnoit.com.br","Engenharia","Auxiliar técnico de cabeamento","61 9370-3086",""),
        GlpiUser("Pedro Nascimento","pedro.nascimento@tecnoit.com.br","Service Desk","Técnico de Cabeamento","",""),
        GlpiUser("Poliana Amorim","poliana.silva@tecnoit.com.br","Supply Chain","Gerente de Supply Chain","62 9 8123-5038","Goiânia"),
        GlpiUser("Rafael Ximenes Barcelos Faria","Rafael.faria@tecnoit.com.br","","Coordenador de obras BSB","",""),
        GlpiUser("Rodrigo Dos Santos","rodrigo.santos@tecnoit.com.br","","","",""),
        GlpiUser("Rodrigo Henrique de Oliveira","rodrigo.henrique@tecnoit.com.br","Engenharia","Auxiliar Técnico de Cabeamento","",""),
        GlpiUser("Rodrigo Muzzi","rodrigo.muzzi@tecnoit.com.br","Engenharia e TI","Gerente de Engenharia e Tecnologia","61 98299-6362",""),
        GlpiUser("Rodrigo Silva","rodrigo.silva@tecnoit.com.br","Obras","Supervisior de Campo","(31) 98647-9242",""),
        GlpiUser("Ronaldo Cavalcante Dos Santos Junior","Ronaldo.junior@tecnoit.com.br","Engenharia","Instalador de infraestrutura","",""),
        GlpiUser("Samuel Teixeira Souza","samuel.souza@tecnoit.com.br","","TECNICO DE CABEAMENTO ESTRUTURADO","","Paulo Afonso"),
        GlpiUser("Sheila Lins de Melo e Silva","sheila.silva@tecnoit.com.br","Comercial - Privado","Gerente de Negócios GO e SP","62 99343-4245","Goiania"),
        GlpiUser("Sophia Viana Pedrosa","Sophia.Pedrosa@tecnoit.com.br","","Jovem aprendiz","62 99936-7788",""),
        GlpiUser("Tarcísio Silva","tarcisio.silva@tecnoit.com.br","Engenharia","Supervisor de Campo","(31) 988156055",""),
        GlpiUser("Thiago Varjão Dias","thiago.dias@tecnoit.com.br","","TECNICO DE CABEAMENTO ESTRUTURADO","","Paulo Afonso"),
        GlpiUser("Thiago Coelho","thiago.coelho@tecnoit.com.br","RH","Técnico de Segurança do Trabalho","",""),
        GlpiUser("Thiago de Morais Gonçalves","thiago.goncalves@tecnoit.com.br","Engenharia","Auxiliar Técnico","75 98345-5742",""),
        GlpiUser("Tiago de Souza","tiago.souza@tecnoit.com.br","","","",""),
        GlpiUser("Valdemir Lima","valdemir.lima@tecnoit.com.br","Obras","Técnico de Cabeamento Estruturado","62 9 8434-6054","Goiânia"),
        GlpiUser("Valéria da Silva","Valeria.silva@tecnoit.com.br","Administrativo financeiro","Analista financeira","62 99553-6106",""),
        GlpiUser("Victor Lima","victor.lima@tecnoit.com.br","Obras","Supervisor de Campo","62 9 9229-6343",""),
        GlpiUser("Victor Teles","victor.teles@tecnoit.com.br","PMO","Analista Administrativo","62984002312","Goiânia"),
        GlpiUser("Victor Venâncio da Silva","victor.silva@tecnoit.com.br","Engenharia","Analista de projetos","62 992115451","Goiania"),
        GlpiUser("Vinnicius Campagnuci","vinnicius.oliveira@tecnoit.com.br","Inovação","Desenvolvedor Backend","62 9 8339-2112","Goiânia"),
        GlpiUser("Wanderson Bruno Alves de Andrade","Wanderson.andrade@tecnoit.com.br","Engenharia","TECNICO DE CABEAMENTO ESTRUTURADO","",""),
        GlpiUser("Wellinton do Nascimento da Silva","Wellinton.Silva@tecnoit.com.br","Supply Chain","Assistente de estoque - Igarapé/MG","31 9060-7335","")
    )

    fun search(query: String): List<GlpiUser> {
        if (query.isBlank()) return allUsers
        val q = query.trim().lowercase()
        return allUsers.filter {
            it.name.lowercase().contains(q) ||
            it.email.lowercase().contains(q) ||
            it.department.lowercase().contains(q) ||
            it.title.lowercase().contains(q)
        }
    }
}
