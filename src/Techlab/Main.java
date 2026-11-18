package Techlab;

import Techlab.dao.AtivoDAO;
import Techlab.dao.AtivoResumo;
import Techlab.dao.ChamadoDAO;
import Techlab.dao.ChamadoResumo;
import Techlab.db.ConexaoBanco;
import Techlab.dto.AtivoCsvRegistro;
import Techlab.repository.AtivoArquivoRepository;
import Techlab.service.ComplementacaoLegadosService;
import Techlab.service.ChamadoService;
import Techlab.service.ConsultaService;
import Techlab.service.NotificadorConsole;
import Techlab.negocio.AtivoTI;
import Techlab.negocio.ChamadoSuporte;
import Techlab.negocio.DispositivoRede;
import Techlab.negocio.EstacaoTrabalho;
import Techlab.negocio.PrioridadeChamado;
import Techlab.negocio.Servidor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Scanner;

/**
 * Menu da aplicacao TechLab, usando os DAOs e o modelo da disciplina.
 * Por padrao utiliza banco novo isolado em dados/techlab-inventory.db.
 * Opcionalmente permite o banco de testes ja migrado.
 */
public final class Main {
    private final Scanner entrada;
    private final AtivoDAO ativos;
    private final ChamadoDAO chamados;
    private final AtivoArquivoRepository arquivos;
    private final ComplementacaoLegadosService legados;
    private final ChamadoService servicoChamados;

    private Main(Scanner entrada, AtivoDAO ativos, ChamadoDAO chamados,
                 AtivoArquivoRepository arquivos,
                 ComplementacaoLegadosService legados, ChamadoService servicoChamados) {
        this.entrada = entrada;
        this.ativos = ativos;
        this.chamados = chamados;
        this.arquivos = arquivos;
        this.legados = legados;
        this.servicoChamados = servicoChamados;
    }

    public static void main(String[] args) {
        if (args.length > 1) {
            System.err.println("Uso: java Techlab.Main [techlab-fase2-teste.db]");
            System.exit(2);
        }
        try {
            Path caminho = args.length == 0 ? ConexaoBanco.CAMINHO_PADRAO : Path.of(args[0]);
            ConexaoBanco banco = new ConexaoBanco(caminho);
            banco.inicializarBanco();
            System.out.println("Banco em uso: " + caminho.toAbsolutePath().normalize());
            try (Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8)) {
                ChamadoDAO chamadoDAO = new ChamadoDAO(banco);
                new Main(scanner, new AtivoDAO(banco), chamadoDAO,
                        new AtivoArquivoRepository(banco),
                        new ComplementacaoLegadosService(banco),
                        new ChamadoService(chamadoDAO, new NotificadorConsole())).executar();
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.err.println("Falha ao iniciar: " + e.getMessage());
            System.exit(1);
        }
    }

    private void executar() {
        while (true) {
            exibirMenu();
            if (!entrada.hasNextLine()) {
                System.out.println("\nEntrada encerrada. Ate logo!");
                return;
            }
            String opcao = entrada.nextLine().trim();
            if ("0".equals(opcao)) {
                System.out.println("Encerrando TechLab. Ate logo!");
                return;
            }
            try {
                executarOpcao(opcao);
            } catch (SQLException | IOException | IllegalArgumentException | DateTimeException e) {
                System.out.println("[ERRO] " + e.getMessage());
            } catch (IllegalStateException e) {
                System.out.println("[ERRO] " + e.getMessage());
                if (!entrada.hasNextLine()) return;
            }
        }
    }

    private static void exibirMenu() {
        System.out.println("\n========== TECHLAB INVENTORY & SERVICE DESK ==========");
        System.out.println("1  - Listar equipamentos");
        System.out.println("2  - Cadastrar equipamento");
        System.out.println("3  - Consultar equipamento por ID");
        System.out.println("4  - Atualizar equipamento completo");
        System.out.println("5  - Excluir equipamento");
        System.out.println("6  - Listar chamados");
        System.out.println("7  - Abrir chamado");
        System.out.println("8  - Consultar chamado por ID");
        System.out.println("9  - Atualizar chamado completo");
        System.out.println("10 - Excluir chamado");
        System.out.println("11 - Exportar equipamentos para CSV");
        System.out.println("12 - Importar equipamentos novos de CSV");
        System.out.println("13 - Completar cadastro antigo de equipamento");
        System.out.println("14 - Completar cadastro antigo de chamado");
        System.out.println("15 - Filtrar equipamentos e ordenar por patrimonio");
        System.out.println("16 - Filtrar chamados e ordenar por descricao");
        System.out.println("0  - Sair");
        System.out.print("Opcao: ");
    }

    private void executarOpcao(String opcao) throws SQLException, IOException {
        switch (opcao) {
            case "1" -> listarAtivos();
            case "2" -> cadastrarAtivo();
            case "3" -> consultarAtivo();
            case "4" -> atualizarAtivo();
            case "5" -> excluirAtivo();
            case "6" -> listarChamados();
            case "7" -> cadastrarChamado();
            case "8" -> consultarChamado();
            case "9" -> atualizarChamado();
            case "10" -> excluirChamado();
            case "11" -> exportarCsv();
            case "12" -> importarCsv();
            case "13" -> completarAtivoAntigo();
            case "14" -> completarChamadoAntigo();
            case "15" -> filtrarAtivos();
            case "16" -> filtrarChamados();
            default -> System.out.println("Opcao invalida. Escolha um numero do menu.");
        }
    }

    private void listarAtivos() throws SQLException {
        List<AtivoResumo> lista = ativos.listarResumos();
        System.out.println("\nEQUIPAMENTOS (" + lista.size() + "):");
        if (lista.isEmpty()) System.out.println("Nenhum equipamento cadastrado.");
        for (AtivoResumo r : lista) exibirResumoAtivo(r);
    }

    private static void exibirResumoAtivo(AtivoResumo r) {
        System.out.printf("#%d | %s | %s | %s%n", r.id(), r.patrimonio(), r.categoria(), r.modelo());
        if (!r.completo()) System.out.println("   Cadastro antigo incompleto: " + r.pendencias());
    }

    private void cadastrarAtivo() throws SQLException {
        System.out.println("\nTipo: 1-Servidor, 2-EstacaoTrabalho, 3-DispositivoRede");
        int tipo = lerInteiro("Tipo: ");
        if (tipo < 1 || tipo > 3) throw new IllegalArgumentException("Tipo de equipamento invalido.");
        String patrimonio = lerObrigatorio("Codigo do patrimonio: ");
        String modelo = lerObrigatorio("Modelo: ");
        String status = lerObrigatorio("Status (ex.: Ativo): ");
        LocalDateTime data = lerData("Data de aquisicao (AAAA-MM-DD): ");
        AtivoTI ativo;
        switch (tipo) {
            case 1 -> ativo = new Servidor(patrimonio, modelo, status, data,
                    lerObrigatorio("IP estatico: "),
                    lerObrigatorio("Sistema operacional: "),
                    lerInteiroPositivo("Memoria RAM em GB: "));
            case 2 -> ativo = new EstacaoTrabalho(patrimonio, modelo, status, data,
                    lerObrigatorio("Departamento: "), lerObrigatorio("Responsavel: "));
            case 3 -> ativo = new DispositivoRede(patrimonio, modelo, status, data,
                    lerInteiroPositivo("Numero de portas: "));
            default -> throw new IllegalStateException("Tipo nao reconhecido.");
        }
        ativos.salvar(ativo);
        System.out.println("Equipamento cadastrado com ID " + ativo.getId());
    }

    private void consultarAtivo() throws SQLException {
        int id = lerInteiroPositivo("ID do equipamento: ");
        AtivoResumo resumo = resumoAtivo(id);
        if (resumo == null) { System.out.println("Equipamento nao encontrado."); return; }
        exibirResumoAtivo(resumo);
        if (!resumo.completo()) return;
        AtivoTI a = ativos.buscarPorId(id);
        if (a != null) exibirAtivoCompleto(a);
    }

    private static void exibirAtivoCompleto(AtivoTI a) {
        System.out.println("   " + a + " | aquisicao=" + a.getDataAquisicao());
        if (a instanceof Servidor s) {
            System.out.println("   IP=" + s.getIpEstatico() + ", SO=" + s.getSistemaOperacional()
                    + ", RAM=" + s.getMemoriaRamGb() + "GB");
        } else if (a instanceof EstacaoTrabalho e) {
            System.out.println("   Departamento=" + e.getDepartamento() + ", responsavel=" + e.getResponsavel());
        } else if (a instanceof DispositivoRede r) {
            System.out.println("   Numero de portas=" + r.getNumeroPortas());
        }
    }

    private void atualizarAtivo() throws SQLException {
        int id = lerInteiroPositivo("ID do equipamento para atualizar: ");
        AtivoResumo resumo = resumoAtivo(id);
        if (resumo == null) { System.out.println("Equipamento nao encontrado."); return; }
        if (!resumo.completo()) {
            System.out.println("Cadastro legado incompleto. Primeiro precisamos preencher: " + resumo.pendencias());
            System.out.println("Nenhuma alteracao feita.");
            return;
        }
        AtivoTI ativo = ativos.buscarPorId(id);
        ativo.setModelo(lerOpcional("Novo modelo [Enter mantem]: ", ativo.getModelo()));
        ativo.setStatus(lerOpcional("Novo status [Enter mantem]: ", ativo.getStatus()));
        if (ativo instanceof Servidor s) {
            s.setMemoriaRamGb(lerInteiroOpcional("RAM em GB [Enter mantem]: ", s.getMemoriaRamGb()));
        } else if (ativo instanceof EstacaoTrabalho e) {
            e.setDepartamento(lerOpcional("Departamento [Enter mantem]: ", e.getDepartamento()));
            e.setResponsavel(lerOpcional("Responsavel [Enter mantem]: ", e.getResponsavel()));
        } else if (ativo instanceof DispositivoRede d) {
            d.setNumeroPortas(lerInteiroOpcional("Portas [Enter mantem]: ", d.getNumeroPortas()));
        }
        ativos.atualizar(ativo);
        System.out.println("Equipamento atualizado: " + id);
    }

    private void excluirAtivo() throws SQLException {
        int id = lerInteiroPositivo("ID do equipamento para excluir: ");
        AtivoResumo resumo = resumoAtivo(id);
        if (resumo == null) { System.out.println("Equipamento nao encontrado."); return; }
        exibirResumoAtivo(resumo);
        if (!confirmarExclusao()) { System.out.println("Exclusao cancelada."); return; }
        ativos.deletar(id);  // O DAO impede exclusao se houver chamados vinculados.
        System.out.println("Equipamento excluido: " + id);
    }

    private AtivoResumo resumoAtivo(int id) throws SQLException {
        for (AtivoResumo r : ativos.listarResumos()) if (r.id() == id) return r;
        return null;
    }

    private void listarChamados() throws SQLException {
        List<ChamadoResumo> lista = chamados.listarResumos();
        System.out.println("\nCHAMADOS (" + lista.size() + "):");
        if (lista.isEmpty()) System.out.println("Nenhum chamado cadastrado.");
        for (ChamadoResumo r : lista) exibirResumoChamado(r);
    }

    private static void exibirResumoChamado(ChamadoResumo r) {
        System.out.printf("#%d | ativo #%d | %s%n", r.id(), r.ativoId(), r.descricao());
        if (!r.completo()) System.out.println("   Cadastro antigo incompleto: " + r.pendencias());
    }

    private void cadastrarChamado() throws SQLException {
        listarAtivos();
        int ativoId = lerInteiroPositivo("ID do equipamento relacionado: ");
        if (resumoAtivo(ativoId) == null) {
            System.out.println("Equipamento inexistente. Chamado nao cadastrado.");
            return;
        }
        ChamadoSuporte chamado = new ChamadoSuporte(
                lerObrigatorio("Titulo: "), lerObrigatorio("Descricao: "),
                lerPrioridade(), LocalDateTime.now(), ativoId);
        servicoChamados.abrir(chamado);
        System.out.println("Chamado aberto com ID " + chamado.getId());
    }

    private void consultarChamado() throws SQLException {
        int id = lerInteiroPositivo("ID do chamado: ");
        ChamadoResumo resumo = resumoChamado(id);
        if (resumo == null) { System.out.println("Chamado nao encontrado."); return; }
        exibirResumoChamado(resumo);
        if (!resumo.completo()) return;
        ChamadoSuporte c = chamados.buscarPorId(id);
        if (c != null) System.out.println("   " + c + " | abertura=" + c.getDataAbertura());
    }

    private void atualizarChamado() throws SQLException {
        int id = lerInteiroPositivo("ID do chamado para atualizar: ");
        ChamadoResumo resumo = resumoChamado(id);
        if (resumo == null) { System.out.println("Chamado nao encontrado."); return; }
        if (!resumo.completo()) {
            System.out.println("Chamado legado incompleto: " + resumo.pendencias());
            System.out.println("Nenhuma alteracao feita.");
            return;
        }
        ChamadoSuporte c = chamados.buscarPorId(id);
        c.setTitulo(lerOpcional("Novo titulo [Enter mantem]: ", c.getTitulo()));
        c.setDescricao(lerOpcional("Nova descricao [Enter mantem]: ", c.getDescricao()));
        System.out.print("Nova prioridade (BAIXA/MEDIA/ALTA/CRITICA) [Enter mantem]: ");
        String prioridade = lerLinha().trim();
        if (!prioridade.isEmpty()) c.setPrioridade(PrioridadeChamado.valueOf(prioridade.toUpperCase(Locale.ROOT)));
        servicoChamados.atualizar(c);
        System.out.println("Chamado atualizado: " + id);
    }

    private void excluirChamado() throws SQLException {
        int id = lerInteiroPositivo("ID do chamado para excluir: ");
        ChamadoResumo resumo = resumoChamado(id);
        if (resumo == null) { System.out.println("Chamado nao encontrado."); return; }
        exibirResumoChamado(resumo);
        if (!confirmarExclusao()) { System.out.println("Exclusao cancelada."); return; }
        chamados.deletar(id);
        System.out.println("Chamado excluido: " + id);
    }

    private ChamadoResumo resumoChamado(int id) throws SQLException {
        for (ChamadoResumo r : chamados.listarResumos()) if (r.id() == id) return r;
        return null;
    }

    /** Demonstra filtragem e ordenacao de DTOs em colecoes Java (inclusive legados). */
    private void filtrarAtivos() throws SQLException {
        System.out.print("Buscar patrimonio, modelo ou categoria [Enter lista todos]: ");
        String termo = lerLinha().trim();
        List<AtivoResumo> resultado = ConsultaService.filtrarEOrdenarAtivos(ativos.listarResumos(), termo);
        System.out.println("\nEQUIPAMENTOS ENCONTRADOS (" + resultado.size() + "):");
        if (resultado.isEmpty()) System.out.println("Nenhum equipamento encontrado.");
        for (AtivoResumo r : resultado) exibirResumoAtivo(r);
    }

    private void filtrarChamados() throws SQLException {
        System.out.print("Buscar na descricao do chamado [Enter lista todos]: ");
        String termo = lerLinha().trim();
        List<ChamadoResumo> resultado = ConsultaService.filtrarEOrdenarChamados(chamados.listarResumos(), termo);
        System.out.println("\nCHAMADOS ENCONTRADOS (" + resultado.size() + "):");
        if (resultado.isEmpty()) System.out.println("Nenhum chamado encontrado.");
        for (ChamadoResumo r : resultado) exibirResumoChamado(r);
    }

    private void exportarCsv() throws SQLException, IOException {
        Path destino = Path.of("..", "TechLabCSV-exportacoes", "ativos-" + System.currentTimeMillis() + ".csv");
        int total = arquivos.exportarBanco(destino);
        System.out.println("CSV criado: " + destino.toAbsolutePath().normalize());
        System.out.println("Equipamentos exportados: " + total
                + " (inclui registros antigos incompletos, sem inventar valores).");
    }

    private void importarCsv() throws SQLException, IOException {
        Path origem = Path.of(lerObrigatorio("Caminho do arquivo CSV: "));
        List<AtivoCsvRegistro> registros = arquivos.carregarTodos(origem);
        System.out.println("Registros validos encontrados: " + registros.size());
        for (AtivoCsvRegistro r : registros) {
            System.out.println("   " + r.patrimonio() + " | " + r.categoria()
                    + (r.completo() ? "" : " | INCOMPLETO: " + String.join(", ", r.pendencias())));
        }
        System.out.println("IDs do CSV nao serao reutilizados. Registros com patrimonio ja existente serao ignorados.");
        System.out.print("Para importar SOMENTE novos, digite IMPORTAR: ");
        if (!"IMPORTAR".equalsIgnoreCase(lerLinha().trim())) {
            System.out.println("Importacao cancelada.");
            return;
        }
        AtivoArquivoRepository.ResultadoImportacao resultado = arquivos.importarNovos(origem);
        System.out.println("Importacao concluida: " + resultado.importados()
                + " novos, " + resultado.jaExistentes() + " ja cadastrados (preservados).");
    }

    /** Preenche apenas valores ausentes; os demais campos e IDs ficam intactos. */
    private void completarAtivoAntigo() throws SQLException {
        listarAtivos();
        int id = lerInteiroPositivo("ID do equipamento antigo: ");
        List<String> campos = legados.camposPendentesAtivo(id);
        if (campos.isEmpty()) {
            System.out.println("Este equipamento ja esta completo.");
            return;
        }
        System.out.println("PENDENCIAS: " + String.join(", ", campos));
        System.out.println("Informe somente dados conhecidos e confirmados. Enter cancela sem gravar.");
        Map<String, String> valores = coletarCampos(campos);
        System.out.print("Digite COMPLETAR para confirmar [ou Enter para cancelar]: ");
        if (!"COMPLETAR".equalsIgnoreCase(lerLinha().trim())) {
            System.out.println("Complementacao cancelada. Nenhum dado alterado.");
            return;
        }
        legados.completarAtivo(id, valores);
        System.out.println("Cadastro do equipamento " + id + " completado, preservando ID e patrimonio.");
    }

    private void completarChamadoAntigo() throws SQLException {
        listarChamados();
        int id = lerInteiroPositivo("ID do chamado antigo: ");
        List<String> campos = legados.camposPendentesChamado(id);
        if (campos.isEmpty()) {
            System.out.println("Este chamado ja esta completo.");
            return;
        }
        System.out.println("PENDENCIAS: " + String.join(", ", campos));
        System.out.println("Nao invente data de abertura historica. Enter cancela sem gravar.");
        Map<String, String> valores = coletarCampos(campos);
        System.out.print("Digite COMPLETAR para confirmar [ou Enter para cancelar]: ");
        if (!"COMPLETAR".equalsIgnoreCase(lerLinha().trim())) {
            System.out.println("Complementacao cancelada. Nenhum dado alterado.");
            return;
        }
        legados.completarChamado(id, valores);
        System.out.println("Cadastro do chamado " + id + " completado, preservando descricao e vinculo.");
    }

    private Map<String, String> coletarCampos(List<String> campos) {
        Map<String, String> valores = new LinkedHashMap<>();
        for (String campo : campos) {
            System.out.print(rotuloComplementacao(campo) + ": ");
            String valor = lerLinha().trim();
            if (valor.isBlank()) {
                throw new IllegalStateException("Cancelado: campo " + campo
                        + " sem informacao. Nenhuma alteracao feita.");
            }
            valores.put(campo, valor);
        }
        return valores;
    }

    private static String rotuloComplementacao(String campo) {
        return switch (campo) {
            case "status" -> "Status (ex.: Ativo)";
            case "data_aquisicao" -> "Data REAL de aquisicao (AAAA-MM-DD)";
            case "ip_estatico" -> "IP estatico";
            case "sistema_operacional" -> "Sistema operacional";
            case "memoria_ram_gb" -> "Memoria RAM em GB";
            case "departamento" -> "Departamento";
            case "responsavel" -> "Responsavel";
            case "numero_portas" -> "Numero de portas";
            case "titulo" -> "Titulo";
            case "prioridade" -> "Prioridade (BAIXA/MEDIA/ALTA/CRITICA)";
            case "data_abertura" -> "Data/hora REAL de abertura (AAAA-MM-DDTHH:MM)";
            default -> campo;
        };
    }

    private boolean confirmarExclusao() {
        System.out.print("Para confirmar, digite EXCLUIR (qualquer outra coisa cancela): ");
        return "EXCLUIR".equalsIgnoreCase(lerLinha().trim());
    }

    private String lerObrigatorio(String rotulo) {
        System.out.print(rotulo);
        String texto = lerLinha().trim();
        if (texto.isEmpty()) throw new IllegalArgumentException("Este campo e obrigatorio: " + rotulo);
        return texto;
    }

    private String lerOpcional(String rotulo, String anterior) {
        System.out.print(rotulo);
        String texto = lerLinha().trim();
        return texto.isEmpty() ? anterior : texto;
    }

    private int lerInteiro(String rotulo) {
        String texto = lerObrigatorio(rotulo);
        try {
            return Integer.parseInt(texto);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Informe um numero inteiro valido para: " + rotulo.trim());
        }
    }

    private int lerInteiroPositivo(String rotulo) {
        int numero = lerInteiro(rotulo);
        if (numero <= 0) throw new IllegalArgumentException("Informe numero inteiro positivo.");
        return numero;
    }

    private int lerInteiroOpcional(String rotulo, int anterior) {
        System.out.print(rotulo);
        String texto = lerLinha().trim();
        if (texto.isEmpty()) return anterior;
        try {
            return Integer.parseInt(texto);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Informe um numero inteiro valido para: " + rotulo.trim());
        }
    }

    private LocalDateTime lerData(String rotulo) {
        return LocalDate.parse(lerObrigatorio(rotulo)).atStartOfDay();
    }

    private PrioridadeChamado lerPrioridade() {
        String texto = lerObrigatorio("Prioridade (BAIXA/MEDIA/ALTA/CRITICA): ");
        return PrioridadeChamado.valueOf(texto.toUpperCase(Locale.ROOT));
    }

    private String lerLinha() {
        if (!entrada.hasNextLine()) throw new IllegalStateException("Entrada encerrada antes do fim da operacao.");
        return entrada.nextLine();
    }
}
