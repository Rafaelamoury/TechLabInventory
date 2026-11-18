package Techlab.testsupport;

import Techlab.interfaces.IConexaoBD;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Implementacao de conexao APENAS PARA TESTES JUNIT.
 * O arquivo SQLite fica sob @TempDir do JUnit e nao usa ConexaoBanco
 * nem abre techlab.db, techlab-fase2-teste.db ou dados/techlab-inventory.db.
 */
public final class BancoTemporario implements IConexaoBD {
    private final Path arquivo;

    public BancoTemporario(Path diretorioTemporario) {
        this.arquivo = diretorioTemporario.resolve("teste-junit.db").toAbsolutePath().normalize();
        inicializarBanco();
    }

    @Override
    public Connection obterConexao() throws SQLException {
        Connection conexao = DriverManager.getConnection("jdbc:sqlite:" + arquivo);
        try (Statement statement = conexao.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            return conexao;
        } catch (SQLException erro) {
            try { conexao.close(); } catch (SQLException e) { erro.addSuppressed(e); }
            throw erro;
        }
    }

    @Override
    public void inicializarBanco() {
        try (Connection conexao = obterConexao(); Statement statement = conexao.createStatement()) {
            // Esquema minimo equivalente ao banco novo da aplicacao.
            statement.executeUpdate("""
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
            statement.executeUpdate("""
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
        } catch (SQLException e) {
            throw new IllegalStateException("Falha na preparacao do banco temporario de testes", e);
        }
    }
}
