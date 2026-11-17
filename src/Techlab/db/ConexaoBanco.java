package Techlab.db;

import Techlab.exceptions.FalhaConexaoBDException;
import Techlab.interfaces.IConexaoBD;

import java.io.IOException;
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
 * Repositório SQLite: inicializa automaticamente APENAS o banco novo isolado.
 * O banco legado techlab.db nunca é aberto por esta classe.
 */
public final class ConexaoBanco implements IConexaoBD {
    public static final Path CAMINHO_PADRAO = Path.of("dados", "techlab-inventory.db");
    private static final String BANCO_TESTE = "techlab-fase2-teste.db";

    private final Path banco;
    private final boolean novoIsolado;

    public ConexaoBanco(Path caminho) {
        if (caminho == null) {
            throw new IllegalArgumentException("Caminho do banco e obrigatorio.");
        }
        this.banco = caminho.toAbsolutePath().normalize();
        Path novoPermitido = CAMINHO_PADRAO.toAbsolutePath().normalize();
        Path testePermitido = Path.of(BANCO_TESTE).toAbsolutePath().normalize();
        this.novoIsolado = banco.equals(novoPermitido);
        if (!novoIsolado && !banco.equals(testePermitido)) {
            throw new IllegalArgumentException(
                    "Caminho bloqueado para proteger dados existentes. "
                    + "Use o banco padrao (sem argumentos) ou " + BANCO_TESTE + ".");
        }
        if (Files.isSymbolicLink(banco) || Files.isSymbolicLink(banco.getParent())) {
            throw new IllegalArgumentException("Nao e permitido usar links simbolicos para o banco.");
        }
        if (Files.exists(banco) && !Files.isRegularFile(banco)) {
            throw new IllegalArgumentException("O caminho do banco nao e um arquivo regular: " + banco);
        }
        if (!novoIsolado && !Files.isRegularFile(banco)) {
            throw new IllegalArgumentException("Banco de testes nao encontrado: " + banco);
        }
    }

    @Override
    public Connection obterConexao() throws SQLException {
        if (!Files.isRegularFile(banco)) {
            throw new FalhaConexaoBDException("Banco nao inicializado: " + banco,
                    new SQLException("Execute inicializarBanco() antes de acessar o SQLite."));
        }
        Connection conexao = abrirConexao();
        return conexao;
    }

    private Connection abrirConexao() throws SQLException {
        Connection conexao;
        try {
            conexao = DriverManager.getConnection("jdbc:sqlite:" + banco);
        } catch (SQLException erro) {
            throw new FalhaConexaoBDException("Falha ao abrir conexao SQLite.", erro);
        }
        try (Statement stmt = conexao.createStatement()) {
            // Obrigatorio por conexao: as FKs do SQLite nao sao globais.
            stmt.execute("PRAGMA foreign_keys = ON");
            return conexao;
        } catch (SQLException erro) {
            try { conexao.close(); } catch (SQLException fechamento) { erro.addSuppressed(fechamento); }
            throw new FalhaConexaoBDException("Falha ao habilitar chaves estrangeiras.", erro);
        }
    }

    @Override
    public void inicializarBanco() {
        try {
            if (novoIsolado) {
                // Nao modifica banco antigo nem o banco de testes.
                Files.createDirectories(banco.getParent());
                if (Files.isSymbolicLink(banco.getParent()) || Files.isSymbolicLink(banco)) {
                    throw new IllegalStateException("Diretorio/arquivo do banco nao pode ser link simbolico.");
                }
            }
            if (!novoIsolado && !Files.isRegularFile(banco)) {
                throw new IllegalStateException("Banco de testes nao encontrado: " + banco);
            }
            // A criacao implicita pelo SQLite so e permitida aqui, no caminho isolado.
            try (Connection conexao = abrirConexao()) {
                if (novoIsolado) criarEsquemaSeNecessario(conexao);
                validarEsquema(conexao);
                validarIntegridade(conexao);
            }
        } catch (SQLException | IOException erro) {
            throw new IllegalStateException("Falha na inicializacao do SQLite: " + erro.getMessage(), erro);
        }
    }

    private static void criarEsquemaSeNecessario(Connection conexao) throws SQLException {
        conexao.setAutoCommit(false);
        try (Statement stmt = conexao.createStatement()) {
            stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS ativos (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        patrimonio TEXT NOT NULL UNIQUE,
                        nome TEXT NOT NULL,
                        categoria TEXT NOT NULL,
                        detalhe_especifico TEXT,
                        status TEXT,
                        data_aquisicao TEXT,
                        ip_estatico TEXT,
                        sistema_operacional TEXT,
                        memoria_ram_gb INTEGER,
                        departamento TEXT,
                        responsavel TEXT,
                        numero_portas INTEGER
                    )
                    """);
            stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS chamados (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        descricao TEXT NOT NULL,
                        ativo_id INTEGER NOT NULL,
                        titulo TEXT,
                        prioridade TEXT,
                        data_abertura TEXT,
                        FOREIGN KEY (ativo_id) REFERENCES ativos(id) ON DELETE RESTRICT
                    )
                    """);
            conexao.commit();
        } catch (SQLException | RuntimeException erro) {
            try { conexao.rollback(); } catch (SQLException falhaRollback) { erro.addSuppressed(falhaRollback); }
            throw erro;
        } finally {
            conexao.setAutoCommit(true);
        }
    }

    private static void validarEsquema(Connection conexao) throws SQLException {
        exigirColunas(conexao, "ativos", new String[]{
                "id", "patrimonio", "nome", "categoria", "detalhe_especifico",
                "status", "data_aquisicao", "ip_estatico", "sistema_operacional",
                "memoria_ram_gb", "departamento", "responsavel", "numero_portas"});
        exigirColunas(conexao, "chamados", new String[]{
                "id", "descricao", "ativo_id", "titulo", "prioridade", "data_abertura"});
    }

    private static void exigirColunas(Connection conexao, String tabela, String[] obrigatorias)
            throws SQLException {
        Set<String> existentes = new HashSet<>();
        // Nome de tabela e constante no codigo, nunca recebido do usuario.
        try (Statement stmt = conexao.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + tabela + ")")) {
            while (rs.next()) existentes.add(rs.getString("name"));
        }
        for (String coluna : obrigatorias) {
            if (!existentes.contains(coluna)) {
                throw new IllegalStateException("Estrutura incompleta: " + tabela + "." + coluna
                        + ". Nao modifique um banco antigo sem backup e migracao apropriada.");
            }
        }
    }

    private static void validarIntegridade(Connection conexao) throws SQLException {
        try (Statement stmt = conexao.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA integrity_check")) {
            if (!rs.next() || !"ok".equals(rs.getString(1))) {
                throw new IllegalStateException("SQLite informou falha de integridade.");
            }
        }
        try (Statement stmt = conexao.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA foreign_key_check")) {
            if (rs.next()) {
                throw new IllegalStateException("Banco contem relacionamento invalido (foreign_key_check).");
            }
        }
    }
}
