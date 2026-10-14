import java.sql.SQLException;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        System.out.println("============================================");
        System.out.println("=== TECHLAB INVENTORY - AULA 08 ===");
        System.out.println("============================================");

        // 1. Inicializar o banco de dados
        ConexaoBanco.inicializarBanco();

        // 2. Criar o DAO dos equipamentos
        AtivoDAO ativoDAO = new AtivoDAO();

        // 3. Criar os equipamentos
        AtivoTI servidor = new AtivoTI(
            "SRV-001",
            "Servidor de Banco de Dados MySQL",
            "Servidor"
        );

        AtivoTI workstation = new AtivoTI(
            "EST-042",
            "Estacao de Trabalho Dev-Pro",
            "EstacaoTrabalho"
        );

        // 4. Registrar equipamentos
        System.out.println("\n--- REGISTRANDO ATIVOS NO BANCO ---");

        try {

            if (ativoDAO.buscarPorPatrimonio("SRV-001") == null) {

                ativoDAO.salvar(servidor);

            } else {

                System.out.println("[INFO] Servidor ja cadastrado.");
            }

            if (ativoDAO.buscarPorPatrimonio("EST-042") == null) {

                ativoDAO.salvar(workstation);

            } else {

                System.out.println("[INFO] Estacao de trabalho ja cadastrada.");
            }

        } catch (SQLException e) {

            System.err.println(
                "Erro ao registrar equipamentos: " + e.getMessage()
            );
        }

        // 5. Listar equipamentos cadastrados
        System.out.println("\n--- RELATORIO DE ATIVOS ---");

        try {

            List<AtivoTI> ativos = ativoDAO.listarTodos();

            if (ativos.isEmpty()) {

                System.out.println("Nenhum ativo cadastrado.");

            } else {

                for (AtivoTI ativo : ativos) {

                    System.out.println(ativo);
                }
            }

        } catch (SQLException e) {

            System.err.println(
                "Erro ao consultar ativos: " + e.getMessage()
            );
        }

        // 6. Buscar equipamento por patrimonio
        System.out.println("\n--- BUSCA POR PATRIMONIO ---");

        try {

            String patrimonioBusca = "SRV-001";

            AtivoTI ativoEncontrado =
                ativoDAO.buscarPorPatrimonio(patrimonioBusca);

            if (ativoEncontrado != null) {

                System.out.println("Equipamento encontrado:");
                System.out.println(ativoEncontrado);

            } else {

                System.out.println("Nenhum equipamento encontrado.");
            }

        } catch (SQLException e) {

            System.err.println(
                "Erro ao buscar equipamento: " + e.getMessage()
            );
        }

        // 7. Registrar chamado de suporte
        System.out.println("\n--- REGISTRANDO CHAMADO ---");

        try {

            AtivoTI ativoChamado =
                ativoDAO.buscarPorPatrimonio("SRV-001");

            if (ativoChamado != null) {

                Chamado chamado = new Chamado(
                    "Servidor apresentando lentidao no banco de dados",
                    ativoChamado.getId()
                );

                ChamadoDAO chamadoDAO = new ChamadoDAO();

                chamadoDAO.salvar(chamado);

            } else {

                System.out.println(
                    "Nao foi possivel registrar o chamado: equipamento nao encontrado."
                );
            }

        } catch (SQLException e) {

            System.err.println(
                "Erro ao registrar chamado: " + e.getMessage()
            );
        }

        // 8. Consultar chamados cadastrados
        System.out.println("\n--- RELATORIO DE CHAMADOS ---");

        try {

            ChamadoDAO chamadoDAO = new ChamadoDAO();

            List<Chamado> chamados = chamadoDAO.listarTodos();

            if (chamados.isEmpty()) {

                System.out.println("Nenhum chamado cadastrado.");

            } else {

                for (Chamado chamado : chamados) {

                    System.out.println(chamado);
                }
            }

        } catch (SQLException e) {

            System.err.println(
                "Erro ao consultar chamados: " + e.getMessage()
            );
        }

        // 9. Finalizar programa
        System.out.println("\n============================================");
        System.out.println("=== EXECUCAO FINALIZADA ===");
        System.out.println("============================================");
    }
}