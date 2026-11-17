package Techlab;

import Techlab.dao.AtivoDAO;
import Techlab.dao.AtivoResumo;
import Techlab.db.ConexaoBanco;
import Techlab.negocio.AtivoTI;
import Techlab.negocio.Servidor;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Testa CRUD da nova camada, exclusivamente sobre techlab-fase2-teste.db.
 * Insere e remove somente um registro de teste gerado por esta execucao.
 * Nao altera os registros antigos e nao apaga chamados.
 */
public final class TestePersistenciaDAO {
    private TestePersistenciaDAO() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException(
                    "Uso: java Techlab.TestePersistenciaDAO techlab-fase2-teste.db");
        }
        ConexaoBanco conexao = new ConexaoBanco(Path.of(args[0]));
        conexao.inicializarBanco();
        AtivoDAO dao = new AtivoDAO(conexao);

        long ativosAntes = contar(conexao, "ativos");
        long chamadosAntes = contar(conexao, "chamados");
        System.out.printf("ANTES: ativos=%d, chamados=%d%n", ativosAntes, chamadosAntes);
        List<AtivoResumo> resumos = dao.listarResumos();
        for (AtivoResumo resumo : resumos) {
            System.out.printf("ATIVO ID=%d | patrimonio=%s | categoria=%s | %s%n",
                    resumo.id(), resumo.patrimonio(), resumo.categoria(),
                    resumo.completo() ? "COMPLETO" : "PENDENCIAS: " + resumo.pendencias());
        }

        // Nao assumir que os dados antigos foram completados pela migracao.
        try {
            dao.listarTodos();
            System.out.println("Listagem tipada: todos os registros estao completos.");
        } catch (SQLException registroLegado) {
            System.out.println("Listagem tipada temporariamente indisponivel: "
                    + registroLegado.getMessage());
        }

        String patrimonioTeste = "SRV-TEST-" + System.nanoTime();
        Servidor novo = new Servidor(patrimonioTeste, "Servidor Teste DAO", "Ativo",
                LocalDateTime.of(2026, 11, 17, 12, 0), "192.0.2.10", "Linux", 16);
        int id = 0;
        try {
            dao.salvar(novo);
            id = novo.getId();
            exigir(id > 0, "Nao recebeu ID do banco");
            System.out.println("CREATE: ativo de teste ID=" + id);

            // Nova instancia do DAO simula consulta apos fechar a sessao anterior.
            AtivoDAO outraSessao = new AtivoDAO(new ConexaoBanco(Path.of(args[0])));
            AtivoTI recuperado = outraSessao.buscarPorId(id);
            exigir(recuperado instanceof Servidor, "Ativo recuperado nao e um Servidor");
            Servidor servidor = (Servidor) recuperado;
            exigir(servidor.getMemoriaRamGb() == 16, "RAM nao persistiu");
            exigir("Linux".equals(servidor.getSistemaOperacional()), "SO nao persistiu");
            System.out.println("READ: servidor recarregado do SQLite com todos os campos.");

            servidor.setStatus("Manutencao");
            servidor.setMemoriaRamGb(32);
            dao.atualizar(servidor);
            Servidor atualizado = (Servidor) outraSessao.buscarPorId(id);
            exigir("Manutencao".equals(atualizado.getStatus()), "Status nao atualizado");
            exigir(atualizado.getMemoriaRamGb() == 32, "RAM nao atualizada");
            System.out.println("UPDATE: status e RAM conferidos.");
        } finally {
            if (id > 0) {
                // Remove apenas o ativo de teste criado nesta execucao.
                dao.deletar(id);
                exigir(dao.buscarPorId(id) == null, "Ativo de teste nao foi excluido");
                System.out.println("DELETE: ativo temporario removido.");
            }
        }

        exigir(contar(conexao, "ativos") == ativosAntes, "Quantidade de ativos antigos mudou");
        exigir(contar(conexao, "chamados") == chamadosAntes, "Quantidade de chamados mudou");
        try (Connection c = conexao.obterConexao();
             PreparedStatement ps = c.prepareStatement("PRAGMA foreign_key_check");
             ResultSet rs = ps.executeQuery()) {
            exigir(!rs.next(), "Ha violacao de chave estrangeira no banco");
        }
        System.out.printf("DEPOIS: ativos=%d, chamados=%d%n", ativosAntes, chamadosAntes);
        System.out.println("TESTE DAO CONCLUIDO COM SUCESSO. Banco original nao acessado.");
    }

    private static long contar(ConexaoBanco bd, String tabela) throws SQLException {
        // Nomes de tabelas constantes neste programa, nao fornecidos pelo usuario.
        try (Connection c = bd.obterConexao();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM " + tabela);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) throw new SQLException("Nao foi possivel contar " + tabela);
            return rs.getLong(1);
        }
    }

    private static void exigir(boolean condicao, String erro) {
        if (!condicao) throw new IllegalStateException(erro);
    }
}
