package Techlab.db;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

/** Amplia SOMENTE o banco de testes, preservando os registros antigos. */
public final class MigracaoChamados {
    private MigracaoChamados() { }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Uso: java Techlab.db.MigracaoChamados techlab-fase2-teste.db");
            System.exit(2);
        }
        Path caminho = Path.of(args[0]).toAbsolutePath().normalize();
        if (!"techlab-fase2-teste.db".equals(caminho.getFileName().toString())
                || !Files.isRegularFile(caminho)) {
            System.err.println("SEGURANCA: use apenas a copia existente techlab-fase2-teste.db.");
            System.exit(2);
        }
        try (Connection conexao = DriverManager.getConnection("jdbc:sqlite:" + caminho)) {
            try (Statement stmt = conexao.createStatement()) { stmt.execute("PRAGMA foreign_keys=ON"); }
            exigirIntegridade(conexao);
            long ativosAntes = contar(conexao, "ativos");
            long chamadosAntes = contar(conexao, "chamados");
            Set<String> colunas = colunas(conexao);
            for (String nome : new String[]{"id", "descricao", "ativo_id"}) {
                if (!colunas.contains(nome)) throw new SQLException("Coluna legada ausente: chamados." + nome);
            }
            conexao.setAutoCommit(false);
            try {
                for (String nome : new String[]{"titulo", "prioridade", "data_abertura"}) {
                    if (colunas.contains(nome)) {
                        System.out.println("JA EXISTE: chamados." + nome);
                    } else {
                        try (Statement stmt = conexao.createStatement()) {
                            stmt.executeUpdate("ALTER TABLE chamados ADD COLUMN " + nome + " TEXT");
                        }
                        System.out.println("ADICIONADA: chamados." + nome);
                    }
                }
                if (contar(conexao, "ativos") != ativosAntes || contar(conexao, "chamados") != chamadosAntes) {
                    throw new SQLException("Quantidade de registros foi alterada; migracao cancelada.");
                }
                exigirIntegridade(conexao);
                conexao.commit();
            } catch (Exception e) {
                conexao.rollback();
                throw e;
            } finally {
                conexao.setAutoCommit(true);
            }
            System.out.printf("ATIVOS PRESERVADOS: %d%n", contar(conexao, "ativos"));
            System.out.printf("CHAMADOS PRESERVADOS: %d%n", contar(conexao, "chamados"));
            System.out.println("INTEGRIDADE: ok");
            System.out.println("MIGRACAO CHAMADOS CONCLUIDA. Original techlab.db nao acessado.");
        } catch (Exception e) {
            System.err.println("FALHA: " + e.getMessage());
            System.exit(1);
        }
    }

    private static Set<String> colunas(Connection c) throws SQLException {
        Set<String> nomes = new HashSet<>();
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("PRAGMA table_info(chamados)")) {
            while (r.next()) nomes.add(r.getString("name"));
        }
        return nomes;
    }

    private static long contar(Connection c, String tabela) throws SQLException {
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT COUNT(*) FROM " + tabela)) {
            return r.next() ? r.getLong(1) : 0;
        }
    }

    private static void exigirIntegridade(Connection c) throws SQLException {
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("PRAGMA integrity_check")) {
            if (!r.next() || !"ok".equals(r.getString(1))) throw new SQLException("integrity_check falhou.");
        }
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("PRAGMA foreign_key_check")) {
            if (r.next()) throw new SQLException("foreign_key_check detectou vinculo invalido.");
        }
    }
}
