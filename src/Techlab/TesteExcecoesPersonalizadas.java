package Techlab;

import Techlab.dao.AtivoDAO;
import Techlab.dao.ChamadoDAO;
import Techlab.db.ConexaoBanco;
import Techlab.exceptions.ChamadoNaoEncontradoException;
import Techlab.exceptions.EquipamentoComChamadosException;
import Techlab.exceptions.EquipamentoNaoEncontradoException;
import Techlab.exceptions.FalhaConexaoBDException;
import Techlab.negocio.ChamadoSuporte;
import Techlab.negocio.PrioridadeChamado;
import Techlab.negocio.Servidor;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.time.LocalDateTime;

/** Verifica erros de negocio com consultas e operacoes que nao mudam registros. */
public final class TesteExcecoesPersonalizadas {
    private TesteExcecoesPersonalizadas() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Informe techlab-fase2-teste.db");
        ConexaoBanco banco = new ConexaoBanco(Path.of(args[0]));
        banco.inicializarBanco();
        AtivoDAO ativos = new AtivoDAO(banco);
        ChamadoDAO chamados = new ChamadoDAO(banco);

        long antesAtivos = contar(banco, "ativos");
        long antesChamados = contar(banco, "chamados");
        // Registro inexistente; id=MAX_VALUE nao foi gerado pela aplicacao.
        int naoExistente = Integer.MAX_VALUE;

        verificar(EquipamentoNaoEncontradoException.class, () -> ativos.deletar(naoExistente));
        Servidor servidor = new Servidor("SRV-EXCECOES", "Nao salvar", "Ativo",
                LocalDateTime.of(2026, 11, 17, 0, 0), "192.0.2.1", "Linux", 16);
        servidor.setId(naoExistente);
        verificar(EquipamentoNaoEncontradoException.class, () -> ativos.atualizar(servidor));
        System.out.println("EQUIPAMENTO NAO ENCONTRADO: OK");

        ChamadoSuporte chamado = new ChamadoSuporte("Nao salvar", "Nao salvar",
                PrioridadeChamado.MEDIA, LocalDateTime.of(2026, 11, 17, 0, 0), 1);
        chamado.setId(naoExistente);
        verificar(ChamadoNaoEncontradoException.class, () -> chamados.atualizar(chamado));
        verificar(ChamadoNaoEncontradoException.class, () -> chamados.deletar(naoExistente));
        System.out.println("CHAMADO NAO ENCONTRADO: OK");

        // Procura ativo com chamados, sem assumir que sera sempre ID=1.
        int ativoComChamado = buscarAtivoComChamado(banco);
        if (ativoComChamado > 0) {
            verificar(EquipamentoComChamadosException.class, () -> ativos.deletar(ativoComChamado));
            System.out.println("ATIVO COM CHAMADOS: exclusao bloqueada corretamente.");
        } else {
            System.out.println("ATIVO COM CHAMADOS: teste nao aplicavel (nenhum vinculo no banco).");
        }

        // O construtor deve preservar a causa de erro SQL.
        SQLException causa = new SQLException("Erro simulado", "08001", 7);
        FalhaConexaoBDException falha = new FalhaConexaoBDException("Falha simulada", causa);
        exigir(falha.getCause() == causa && "08001".equals(falha.getSQLState()),
                "FalhaConexaoBDException perdeu a causa.");
        System.out.println("FALHA CONEXAO: causa SQL preservada.");

        exigir(contar(banco, "ativos") == antesAtivos, "Contagem de equipamentos mudou");
        exigir(contar(banco, "chamados") == antesChamados, "Contagem de chamados mudou");
        System.out.printf("CONTAGENS PRESERVADAS: ativos=%d, chamados=%d%n", antesAtivos, antesChamados);
        System.out.println("TESTE DE EXCECOES CONCLUIDO. Banco original nao acessado.");
    }

    private static int buscarAtivoComChamado(ConexaoBanco banco) throws SQLException {
        try (Connection conexao = banco.obterConexao();
             Statement sql = conexao.createStatement();
             ResultSet registros = sql.executeQuery("SELECT ativo_id FROM chamados GROUP BY ativo_id LIMIT 1")) {
            return registros.next() ? registros.getInt(1) : 0;
        }
    }

    private static long contar(ConexaoBanco banco, String tabela) throws SQLException {
        // O nome da tabela e constante e conhecido, nao vem do usuario.
        try (Connection conexao = banco.obterConexao();
             Statement sql = conexao.createStatement();
             ResultSet registros = sql.executeQuery("SELECT COUNT(*) FROM " + tabela)) {
            return registros.next() ? registros.getLong(1) : 0;
        }
    }

    @FunctionalInterface
    private interface AcaoSql { void executar() throws SQLException; }

    private static void verificar(Class<? extends SQLException> tipo, AcaoSql acao) throws SQLException {
        try {
            acao.executar();
        } catch (SQLException falha) {
            if (tipo.isInstance(falha)) return;
            throw new IllegalStateException("Esperava " + tipo.getSimpleName()
                    + ", mas recebeu " + falha.getClass().getSimpleName(), falha);
        }
        throw new IllegalStateException("Esperava erro " + tipo.getSimpleName() + ", mas nao ocorreu.");
    }

    private static void exigir(boolean ok, String mensagem) {
        if (!ok) throw new IllegalStateException(mensagem);
    }
}
