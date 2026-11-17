package Techlab.dao;

import Techlab.exceptions.ChamadoNaoEncontradoException;
import Techlab.interfaces.IConexaoBD;
import Techlab.interfaces.IPersistencia;
import Techlab.negocio.ChamadoSuporte;
import Techlab.negocio.PrioridadeChamado;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/** Adapta o ChamadoDAO original ao contrato IPersistencia do professor. */
public final class ChamadoDAO implements IPersistencia<ChamadoSuporte> {
    private final IConexaoBD banco;
    private static final String CAMPOS = "id, descricao, ativo_id, titulo, prioridade, data_abertura";

    public ChamadoDAO(IConexaoBD banco) {
        if (banco == null) throw new IllegalArgumentException("Conexao obrigatoria.");
        this.banco = banco;
    }

    @Override
    public void salvar(ChamadoSuporte chamado) throws SQLException {
        if (chamado == null) throw new IllegalArgumentException("Chamado obrigatorio.");
        if (chamado.getId() != 0) throw new IllegalArgumentException("Chamado ja salvo; use atualizar.");
        String sql = "INSERT INTO chamados (descricao, ativo_id, titulo, prioridade, data_abertura) "
                   + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = banco.obterConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            preencher(stmt, chamado);
            if (stmt.executeUpdate() != 1) throw new SQLException("Nao foi possivel cadastrar chamado.");
            try (Statement idStmt = conn.createStatement();
                 ResultSet rs = idStmt.executeQuery("SELECT last_insert_rowid()")) {
                if (!rs.next()) throw new SQLException("ID do chamado nao retornado.");
                chamado.setId(rs.getInt(1));
            }
        }
    }

    @Override
    public ChamadoSuporte buscarPorId(int id) throws SQLException {
        exigirId(id);
        try (Connection conn = banco.obterConexao();
             PreparedStatement stmt = conn.prepareStatement("SELECT " + CAMPOS + " FROM chamados WHERE id=?")) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapearCompleto(rs) : null;
            }
        }
    }

    @Override
    public List<ChamadoSuporte> listarTodos() throws SQLException {
        List<ChamadoSuporte> chamados = new ArrayList<>();
        try (Connection conn = banco.obterConexao();
             PreparedStatement stmt = conn.prepareStatement("SELECT " + CAMPOS + " FROM chamados ORDER BY id");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) chamados.add(mapearCompleto(rs));
        }
        return chamados;
    }

    /** Listagem que inclui chamados anteriores a integracao, mesmo incompletos. */
    public List<ChamadoResumo> listarResumos() throws SQLException {
        List<ChamadoResumo> resumos = new ArrayList<>();
        try (Connection conn = banco.obterConexao();
             PreparedStatement stmt = conn.prepareStatement("SELECT " + CAMPOS + " FROM chamados ORDER BY id");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                resumos.add(new ChamadoResumo(rs.getInt("id"), rs.getString("descricao"),
                        rs.getInt("ativo_id"), String.join(", ", pendencias(rs))));
            }
        }
        return resumos;
    }

    @Override
    public void atualizar(ChamadoSuporte chamado) throws SQLException {
        if (chamado == null) throw new IllegalArgumentException("Chamado obrigatorio.");
        exigirId(chamado.getId());
        String sql = "UPDATE chamados SET descricao=?, ativo_id=?, titulo=?, prioridade=?, data_abertura=? WHERE id=?";
        try (Connection conn = banco.obterConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            preencher(stmt, chamado);
            stmt.setInt(6, chamado.getId());
            if (stmt.executeUpdate() != 1) throw new ChamadoNaoEncontradoException(chamado.getId());
        }
    }

    @Override
    public void deletar(int id) throws SQLException {
        exigirId(id);
        try (Connection conn = banco.obterConexao();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM chamados WHERE id=?")) {
            stmt.setInt(1, id);
            if (stmt.executeUpdate() != 1) throw new ChamadoNaoEncontradoException(id);
        }
    }

    private static void preencher(PreparedStatement stmt, ChamadoSuporte chamado) throws SQLException {
        stmt.setString(1, chamado.getDescricao());
        stmt.setInt(2, chamado.getAtivoRelacionadoId());
        stmt.setString(3, chamado.getTitulo());
        stmt.setString(4, chamado.getPrioridade().name());
        stmt.setString(5, chamado.getDataAbertura().toString());
    }

    private static ChamadoSuporte mapearCompleto(ResultSet rs) throws SQLException {
        List<String> faltas = pendencias(rs);
        int id = rs.getInt("id");
        if (!faltas.isEmpty()) {
            throw new SQLException("Chamado legado incompleto ID=" + id + ": " + String.join(", ", faltas)
                    + ". Use listarResumos() para exibir chamados antigos.");
        }
        try {
            return new ChamadoSuporte(id, rs.getString("titulo"), rs.getString("descricao"),
                    PrioridadeChamado.valueOf(rs.getString("prioridade")),
                    LocalDateTime.parse(rs.getString("data_abertura")), rs.getInt("ativo_id"));
        } catch (IllegalArgumentException erro) {
            throw new SQLException("Chamado ID=" + id + " contem dados invalidos.", erro);
        }
    }

    private static List<String> pendencias(ResultSet rs) throws SQLException {
        List<String> faltas = new ArrayList<>();
        if (vazio(rs.getString("titulo"))) faltas.add("titulo");
        if (vazio(rs.getString("descricao"))) faltas.add("descricao");
        String prioridade = rs.getString("prioridade");
        if (vazio(prioridade)) faltas.add("prioridade");
        else {
            try { PrioridadeChamado.valueOf(prioridade); }
            catch (IllegalArgumentException e) { faltas.add("prioridade invalida"); }
        }
        String data = rs.getString("data_abertura");
        if (vazio(data)) faltas.add("data_abertura");
        else {
            try { LocalDateTime.parse(data); }
            catch (DateTimeParseException e) { faltas.add("data_abertura invalida"); }
        }
        if (rs.getInt("ativo_id") <= 0) faltas.add("ativo_id");
        return faltas;
    }

    private static boolean vazio(String texto) { return texto == null || texto.isBlank(); }
    private static void exigirId(int id) {
        if (id <= 0) throw new IllegalArgumentException("ID deve ser positivo.");
    }
}
