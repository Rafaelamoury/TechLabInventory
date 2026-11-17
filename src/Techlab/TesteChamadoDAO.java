package Techlab;

import Techlab.dao.ChamadoDAO;
import Techlab.dao.ChamadoResumo;
import Techlab.db.ConexaoBanco;
import Techlab.negocio.ChamadoSuporte;
import Techlab.negocio.PrioridadeChamado;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/** Testa CRUD de chamados somente na copia, sem alterar registros antigos. */
public final class TesteChamadoDAO {
    private TesteChamadoDAO() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Informe techlab-fase2-teste.db.");
        ConexaoBanco bd = new ConexaoBanco(Path.of(args[0])); // ja impede banco original
        bd.inicializarBanco();
        ChamadoDAO dao = new ChamadoDAO(bd);
        long ativosAntes = contar(bd, "ativos");
        long chamadosAntes = contar(bd, "chamados");
        List<ChamadoResumo> historicos = dao.listarResumos();
        System.out.printf("ANTES: ativos=%d, chamados=%d%n", ativosAntes, chamadosAntes);
        for (ChamadoResumo resumo : historicos) {
            System.out.printf("CHAMADO %d -> ATIVO %d | %s%n", resumo.id(), resumo.ativoId(),
                    resumo.completo() ? "COMPLETO" : "PENDENCIAS: " + resumo.pendencias());
        }
        // Escolhe um ativo ja existente; nenhum ativo e cadastrado neste teste.
        int ativoExistente = primeiroAtivoId(bd);
        ChamadoSuporte chamado = new ChamadoSuporte("Teste de suporte", "Verificar conectividade",
                PrioridadeChamado.MEDIA, LocalDateTime.of(2026, 11, 17, 14, 0), ativoExistente);
        int idTeste = 0;
        try {
            dao.salvar(chamado);
            idTeste = chamado.getId();
            exigir(idTeste > 0, "ID nao retornado");
            System.out.println("CREATE: chamado ID=" + idTeste);
            ChamadoDAO outraSessao = new ChamadoDAO(new ConexaoBanco(Path.of(args[0])));
            ChamadoSuporte lido = outraSessao.buscarPorId(idTeste);
            exigir(lido != null && lido.getAtivoRelacionadoId() == ativoExistente, "READ falhou");
            exigir(lido.getPrioridade() == PrioridadeChamado.MEDIA, "Prioridade nao persistiu");
            System.out.println("READ: chamado recuperado e vinculo conferido.");
            lido.setPrioridade(PrioridadeChamado.ALTA);
            lido.setTitulo("Teste atualizado");
            dao.atualizar(lido);
            ChamadoSuporte atualizado = outraSessao.buscarPorId(idTeste);
            exigir("Teste atualizado".equals(atualizado.getTitulo())
                    && atualizado.getPrioridade() == PrioridadeChamado.ALTA, "UPDATE falhou");
            System.out.println("UPDATE: titulo e prioridade atualizados.");
            // Valida restricao de chave estrangeira sem modificar outros chamados.
            try {
                ChamadoSuporte invalido = new ChamadoSuporte("Teste invalido", "FK",
                        PrioridadeChamado.BAIXA, LocalDateTime.now(), Integer.MAX_VALUE);
                dao.salvar(invalido);
                throw new IllegalStateException("Falhou: chamado com ativo inexistente foi aceito.");
            } catch (SQLException esperado) {
                System.out.println("FK: bloqueou chamado associado a ativo inexistente.");
            }
        } finally {
            if (idTeste > 0) {
                dao.deletar(idTeste);
                exigir(dao.buscarPorId(idTeste) == null, "DELETE falhou");
                System.out.println("DELETE: somente chamado temporario removido.");
            }
        }
        exigir(contar(bd, "ativos") == ativosAntes, "Quantidade de ativos mudou");
        exigir(contar(bd, "chamados") == chamadosAntes, "Quantidade de chamados antigos mudou");
        try (Connection c = bd.obterConexao();
             PreparedStatement p = c.prepareStatement("PRAGMA foreign_key_check");
             ResultSet r = p.executeQuery()) { exigir(!r.next(), "Violacao de FK"); }
        System.out.printf("DEPOIS: ativos=%d, chamados=%d%n", contar(bd, "ativos"), contar(bd, "chamados"));
        System.out.println("TESTE CHAMADOS CONCLUIDO COM SUCESSO. Original nao acessado.");
    }

    private static int primeiroAtivoId(ConexaoBanco banco) throws SQLException {
        try (Connection c = banco.obterConexao();
             PreparedStatement p = c.prepareStatement("SELECT id FROM ativos ORDER BY id LIMIT 1");
             ResultSet r = p.executeQuery()) {
            if (!r.next()) throw new SQLException("Sem ativo para vincular chamado de teste.");
            return r.getInt(1);
        }
    }
    private static long contar(ConexaoBanco banco, String tabela) throws SQLException {
        try (Connection c = banco.obterConexao();
             PreparedStatement p = c.prepareStatement("SELECT COUNT(*) FROM " + tabela);
             ResultSet r = p.executeQuery()) {
            return r.next() ? r.getLong(1) : 0;
        }
    }
    private static void exigir(boolean condicao, String erro) {
        if (!condicao) throw new IllegalStateException(erro);
    }
}
