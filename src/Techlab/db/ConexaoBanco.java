package Techlab.db;

import Techlab.interfaces.IConexaoBD;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

/**
 * Conexao com a COPIA ja migrada do banco do projeto.
 * Nesta fase nao abre ou cria o banco original techlab.db.
 */
public final class ConexaoBanco implements IConexaoBD {
    private final Path banco;

    public ConexaoBanco(Path banco) {
        if (banco == null) {
            throw new IllegalArgumentException("Caminho do banco e obrigatorio.");
        }
        this.banco = banco.toAbsolutePath().normalize();
        if (!"techlab-fase2-teste.db".equals(this.banco.getFileName().toString())) {
            throw new IllegalArgumentException(
                    "Por seguranca, nesta etapa use somente techlab-fase2-teste.db.");
        }
        if (!Files.isRegularFile(this.banco)) {
            throw new IllegalArgumentException("Banco de testes nao encontrado: " + this.banco);
        }
    }

    @Override
    public Connection obterConexao() throws SQLException {
        // Nao permitir criacao implicita caso a copia tenha sido removida.
        if (!Files.isRegularFile(this.banco)) {
            throw new SQLException("Banco de testes nao encontrado: " + this.banco);
        }
        Connection conexao = DriverManager.getConnection("jdbc:sqlite:" + this.banco);
        try (Statement stmt = conexao.createStatement()) {
            // PRAGMA foreign_keys e configurado por conexao no SQLite.
            stmt.execute("PRAGMA foreign_keys = ON");
            return conexao;
        } catch (SQLException e) {
            try { conexao.close(); } catch (SQLException fechamento) { e.addSuppressed(fechamento); }
            throw e;
        }
    }

    /**
     * Nesta fase a migracao ja foi executada: validar colunas em vez de criar
     * ou alterar tabelas silenciosamente.
     */
    @Override
    public void inicializarBanco() {
        Set<String> colunas = new HashSet<>();
        try (Connection conexao = obterConexao();
             Statement stmt = conexao.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(ativos)")) {
            while (rs.next()) colunas.add(rs.getString("name"));
        } catch (SQLException e) {
            throw new IllegalStateException("Nao foi possivel verificar o esquema do banco.", e);
        }
        for (String coluna : new String[]{"id", "patrimonio", "nome", "categoria",
                "detalhe_especifico", "status", "data_aquisicao", "ip_estatico",
                "sistema_operacional", "memoria_ram_gb", "departamento",
                "responsavel", "numero_portas"}) {
            if (!colunas.contains(coluna)) {
                throw new IllegalStateException("Coluna ausente: ativos." + coluna
                        + ". Execute a migracao na COPIA de testes antes.");
            }
        }
    }
}
