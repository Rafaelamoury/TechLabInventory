package Techlab.dao;

import Techlab.exceptions.EquipamentoComChamadosException;
import Techlab.exceptions.EquipamentoNaoEncontradoException;
import Techlab.interfaces.IConexaoBD;
import Techlab.interfaces.IPersistencia;
import Techlab.negocio.AtivoTI;
import Techlab.negocio.DispositivoRede;
import Techlab.negocio.EstacaoTrabalho;
import Techlab.negocio.Servidor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/** DAO adaptado do exemplo do professor para as colunas reais do SQLite. */
public final class AtivoDAO implements IPersistencia<AtivoTI> {
    private static final String COLUNAS = "id, patrimonio, nome, categoria, detalhe_especifico, "
            + "status, data_aquisicao, ip_estatico, sistema_operacional, memoria_ram_gb, "
            + "departamento, responsavel, numero_portas";

    private final IConexaoBD conexaoBD;

    public AtivoDAO(IConexaoBD conexaoBD) {
        if (conexaoBD == null) throw new IllegalArgumentException("Conexao obrigatoria.");
        this.conexaoBD = conexaoBD;
    }

    @Override
    public void salvar(AtivoTI ativo) throws SQLException {
        validarNovo(ativo);
        String sql = "INSERT INTO ativos (patrimonio, nome, categoria, detalhe_especifico, "
                + "status, data_aquisicao, ip_estatico, sistema_operacional, memoria_ram_gb, "
                + "departamento, responsavel, numero_portas) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = conexaoBD.obterConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            preencherParametros(stmt, ativo);
            int linhas = stmt.executeUpdate();
            if (linhas != 1) throw new SQLException("Cadastro nao gravou exatamente um ativo.");
            try (Statement idStmt = conn.createStatement();
                 ResultSet rs = idStmt.executeQuery("SELECT last_insert_rowid()")) {
                if (!rs.next()) throw new SQLException("ID gerado nao encontrado.");
                ativo.setId(rs.getInt(1));
            }
        }
    }

    @Override
    public AtivoTI buscarPorId(int id) throws SQLException {
        exigirId(id);
        try (Connection conn = conexaoBD.obterConexao();
             PreparedStatement stmt = conn.prepareStatement("SELECT " + COLUNAS + " FROM ativos WHERE id = ?")) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapearCompleto(rs) : null;
            }
        }
    }

    /**
     * Devolve todos os ativos se TODOS estiverem completos. Nunca ignora
     * registros antigos, nem inventa data, IP ou outros atributos.
     */
    @Override
    public List<AtivoTI> listarTodos() throws SQLException {
        List<AtivoTI> ativos = new ArrayList<>();
        try (Connection conn = conexaoBD.obterConexao();
             PreparedStatement stmt = conn.prepareStatement("SELECT " + COLUNAS + " FROM ativos ORDER BY id");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) ativos.add(mapearCompleto(rs));
        }
        return ativos;
    }

    /** Consulta segura para exibir tambem os registros antigos incompletos. */
    public List<AtivoResumo> listarResumos() throws SQLException {
        List<AtivoResumo> resumos = new ArrayList<>();
        try (Connection conn = conexaoBD.obterConexao();
             PreparedStatement stmt = conn.prepareStatement("SELECT " + COLUNAS + " FROM ativos ORDER BY id");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                resumos.add(new AtivoResumo(rs.getInt("id"), rs.getString("patrimonio"),
                        rs.getString("nome"), rs.getString("categoria"),
                        String.join(", ", pendencias(rs))));
            }
        }
        return resumos;
    }

    @Override
    public void atualizar(AtivoTI ativo) throws SQLException {
        if (ativo == null) throw new IllegalArgumentException("Ativo obrigatorio.");
        exigirId(ativo.getId());
        String sql = "UPDATE ativos SET patrimonio=?, nome=?, categoria=?, detalhe_especifico=?, "
                + "status=?, data_aquisicao=?, ip_estatico=?, sistema_operacional=?, memoria_ram_gb=?, "
                + "departamento=?, responsavel=?, numero_portas=? WHERE id=?";
        try (Connection conn = conexaoBD.obterConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            preencherParametros(stmt, ativo);
            stmt.setInt(13, ativo.getId());
            if (stmt.executeUpdate() != 1) {
                throw new EquipamentoNaoEncontradoException(ativo.getId());
            }
        }
    }

    @Override
    public void deletar(int id) throws SQLException {
        exigirId(id);
        try (Connection conn = conexaoBD.obterConexao()) {
            try (PreparedStatement vinculos = conn.prepareStatement(
                    "SELECT COUNT(*) FROM chamados WHERE ativo_id = ?")) {
                vinculos.setInt(1, id);
                try (ResultSet rs = vinculos.executeQuery()) {
                    if (rs.next() && rs.getLong(1) > 0) {
                        throw new EquipamentoComChamadosException(id);
                    }
                }
            }
            try (PreparedStatement excluir = conn.prepareStatement("DELETE FROM ativos WHERE id = ?")) {
                excluir.setInt(1, id);
                if (excluir.executeUpdate() != 1) {
                    throw new EquipamentoNaoEncontradoException(id);
                }
            }
        }
    }

    private static void validarNovo(AtivoTI ativo) {
        if (ativo == null) throw new IllegalArgumentException("Ativo obrigatorio.");
        if (ativo.getId() != 0) {
            throw new IllegalArgumentException("Ativo ja possui ID: use atualizar em vez de salvar.");
        }
    }

    private static void exigirId(int id) {
        if (id <= 0) throw new IllegalArgumentException("ID deve ser positivo.");
    }

    private static void preencherParametros(PreparedStatement stmt, AtivoTI ativo) throws SQLException {
        String categoria = ativo.getClass().getSimpleName();
        String detalhe;
        if (ativo instanceof Servidor s) detalhe = s.getIpEstatico();
        else if (ativo instanceof EstacaoTrabalho e) detalhe = e.getDepartamento();
        else if (ativo instanceof DispositivoRede d) detalhe = Integer.toString(d.getNumeroPortas());
        else throw new IllegalArgumentException("Tipo de ativo desconhecido: " + categoria);

        stmt.setString(1, ativo.getCodigoPatrimonio());
        stmt.setString(2, ativo.getModelo());
        stmt.setString(3, categoria);
        stmt.setString(4, detalhe);
        stmt.setString(5, ativo.getStatus());
        stmt.setString(6, ativo.getDataAquisicao().toString());
        if (ativo instanceof Servidor s) {
            stmt.setString(7, s.getIpEstatico());
            stmt.setString(8, s.getSistemaOperacional());
            stmt.setInt(9, s.getMemoriaRamGb());
        } else {
            stmt.setNull(7, java.sql.Types.VARCHAR);
            stmt.setNull(8, java.sql.Types.VARCHAR);
            stmt.setNull(9, java.sql.Types.INTEGER);
        }
        if (ativo instanceof EstacaoTrabalho e) {
            stmt.setString(10, e.getDepartamento());
            stmt.setString(11, e.getResponsavel());
        } else {
            stmt.setNull(10, java.sql.Types.VARCHAR);
            stmt.setNull(11, java.sql.Types.VARCHAR);
        }
        if (ativo instanceof DispositivoRede d) stmt.setInt(12, d.getNumeroPortas());
        else stmt.setNull(12, java.sql.Types.INTEGER);
    }

    private static AtivoTI mapearCompleto(ResultSet rs) throws SQLException {
        List<String> faltas = pendencias(rs);
        int id = rs.getInt("id");
        if (!faltas.isEmpty()) {
            throw new SQLException("Registro legado incompleto: ativo ID=" + id + ", patrimonio="
                    + rs.getString("patrimonio") + ". Campos pendentes: " + String.join(", ", faltas)
                    + ". Consulte listarResumos() e complete os dados antes de usar listarTodos().");
        }
        String patrimonio = rs.getString("patrimonio");
        String nome = rs.getString("nome");
        LocalDateTime data = LocalDateTime.parse(rs.getString("data_aquisicao"));
        String status = rs.getString("status");
        switch (rs.getString("categoria")) {
            case "Servidor":
                return new Servidor(id, patrimonio, nome, data, status,
                        rs.getString("ip_estatico"), rs.getString("sistema_operacional"),
                        rs.getInt("memoria_ram_gb"));
            case "EstacaoTrabalho":
                return new EstacaoTrabalho(id, patrimonio, nome, data, status,
                        rs.getString("departamento"), rs.getString("responsavel"));
            case "DispositivoRede":
                return new DispositivoRede(id, patrimonio, nome, data, status,
                        rs.getInt("numero_portas"));
            default:
                throw new SQLException("Categoria desconhecida para ativo " + id);
        }
    }

    private static List<String> pendencias(ResultSet rs) throws SQLException {
        List<String> faltas = new ArrayList<>();
        if (vazio(rs.getString("status"))) faltas.add("status");
        String data = rs.getString("data_aquisicao");
        if (vazio(data)) faltas.add("data_aquisicao");
        else {
            try { LocalDateTime.parse(data); }
            catch (DateTimeParseException e) { faltas.add("data_aquisicao (formato invalido)"); }
        }
        String categoria = rs.getString("categoria");
        if ("Servidor".equals(categoria)) {
            if (vazio(rs.getString("ip_estatico"))) faltas.add("ip_estatico");
            if (vazio(rs.getString("sistema_operacional"))) faltas.add("sistema_operacional");
            if (rs.getObject("memoria_ram_gb") == null || rs.getInt("memoria_ram_gb") <= 0)
                faltas.add("memoria_ram_gb");
        } else if ("EstacaoTrabalho".equals(categoria)) {
            if (vazio(rs.getString("departamento"))) faltas.add("departamento");
            if (vazio(rs.getString("responsavel"))) faltas.add("responsavel");
        } else if ("DispositivoRede".equals(categoria)) {
            if (rs.getObject("numero_portas") == null || rs.getInt("numero_portas") <= 0)
                faltas.add("numero_portas");
        } else {
            faltas.add("categoria (desconhecida: " + categoria + ")");
        }
        return faltas;
    }

    private static boolean vazio(String s) { return s == null || s.isBlank(); }
}
