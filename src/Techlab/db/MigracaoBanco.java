package Techlab.db;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Etapa 2: amplia SOMENTE a copia techlab-fase2-teste.db.
 * Nao preenche valores desconhecidos nem recria tabelas.
 * Transacao, verificacao de integridade e reexecucao segura.
 */
public final class MigracaoBanco {
    private static final Map<String, String> NOVAS_COLUNAS = new LinkedHashMap<>();

    static {
        NOVAS_COLUNAS.put("status", "TEXT");
        NOVAS_COLUNAS.put("data_aquisicao", "TEXT");
        NOVAS_COLUNAS.put("ip_estatico", "TEXT");
        NOVAS_COLUNAS.put("sistema_operacional", "TEXT");
        NOVAS_COLUNAS.put("memoria_ram_gb", "INTEGER");
        NOVAS_COLUNAS.put("departamento", "TEXT");
        NOVAS_COLUNAS.put("responsavel", "TEXT");
        NOVAS_COLUNAS.put("numero_portas", "INTEGER");
    }

    private MigracaoBanco() { }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Uso: java Techlab.db.MigracaoBanco techlab-fase2-teste.db");
            System.exit(2);
        }
        Path caminho = Path.of(args[0]).toAbsolutePath().normalize();
        if (!"techlab-fase2-teste.db".equals(caminho.getFileName().toString()) || !Files.isRegularFile(caminho)) {
            System.err.println("SEGURANCA: esta ferramenta so aceita um arquivo existente chamado techlab-fase2-teste.db.");
            System.exit(2);
        }

        System.out.println("TECHLAB - MIGRACAO SEGURA DE ESQUEMA (APENAS COPIA)");
        System.out.println("Banco: " + caminho);
        try (Connection conexao = DriverManager.getConnection("jdbc:sqlite:" + caminho)) {
            try (Statement stmt = conexao.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
            }
            exigirIntegridade(conexao);
            exigirSemVinculosOrfaos(conexao);
            exigirEstruturaLegada(conexao);
            long ativosAntes = contar(conexao, "ativos");
            long chamadosAntes = contar(conexao, "chamados");

            conexao.setAutoCommit(false);
            try {
                Set<String> colunasExistentes = colunas(conexao, "ativos");
                for (Map.Entry<String, String> coluna : NOVAS_COLUNAS.entrySet()) {
                    if (colunasExistentes.contains(coluna.getKey())) {
                        System.out.println("JA EXISTE: ativos." + coluna.getKey());
                        continue;
                    }
                    // Todos os nomes/tipos sao constantes definidas neste codigo, nao entrada do usuario.
                    String sql = "ALTER TABLE ativos ADD COLUMN " + coluna.getKey() + " " + coluna.getValue();
                    try (Statement stmt = conexao.createStatement()) {
                        stmt.executeUpdate(sql);
                    }
                    System.out.println("ADICIONADA: ativos." + coluna.getKey());
                }
                if (contar(conexao, "ativos") != ativosAntes || contar(conexao, "chamados") != chamadosAntes) {
                    throw new SQLException("A quantidade de registros mudou. Migracao cancelada.");
                }
                exigirIntegridade(conexao);
                exigirSemVinculosOrfaos(conexao);
                conexao.commit();
            } catch (SQLException | RuntimeException erro) {
                conexao.rollback();
                throw erro;
            } finally {
                conexao.setAutoCommit(true);
            }

            System.out.println("\nCONFIRMACAO:");
            System.out.println("Ativos preservados: " + contar(conexao, "ativos"));
            System.out.println("Chamados preservados: " + contar(conexao, "chamados"));
            System.out.println("Integridade SQLite: " + integridade(conexao));
            System.out.println("Vinculos orfaos: " + contarVinculosOrfaos(conexao));
            System.out.println("Ativos sem data de aquisicao: " + contarPendencia(conexao, "data_aquisicao"));
            System.out.println("Ativos sem status: " + contarPendencia(conexao, "status"));
            System.out.println("MIGRACAO CONCLUIDA. Dados existentes nao foram modificados.");
            System.out.println("O banco ORIGINAL techlab.db nao foi acessado por esta ferramenta.");
        } catch (Exception e) {
            System.err.println("FALHA NA MIGRACAO: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void exigirEstruturaLegada(Connection c) throws SQLException {
        Set<String> ativos = colunas(c, "ativos");
        Set<String> chamados = colunas(c, "chamados");
        for (String nome : new String[]{"id", "patrimonio", "nome", "categoria", "detalhe_especifico"}) {
            if (!ativos.contains(nome)) throw new SQLException("Falta a coluna legada ativos." + nome);
        }
        for (String nome : new String[]{"id", "descricao", "ativo_id"}) {
            if (!chamados.contains(nome)) throw new SQLException("Falta a coluna legada chamados." + nome);
        }
    }

    private static Set<String> colunas(Connection c, String tabela) throws SQLException {
        Set<String> resultado = new LinkedHashSet<>();
        try (Statement stmt = c.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + tabela + ")")) {
            while (rs.next()) resultado.add(rs.getString("name"));
        }
        return resultado;
    }

    private static long contar(Connection c, String tabela) throws SQLException {
        try (Statement stmt = c.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tabela)) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    private static long contarPendencia(Connection c, String coluna) throws SQLException {
        try (Statement stmt = c.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM ativos WHERE " + coluna + " IS NULL")) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    private static String integridade(Connection c) throws SQLException {
        try (Statement stmt = c.createStatement(); ResultSet rs = stmt.executeQuery("PRAGMA integrity_check")) {
            return rs.next() ? rs.getString(1) : "sem resultado";
        }
    }

    private static void exigirIntegridade(Connection c) throws SQLException {
        if (!"ok".equals(integridade(c))) throw new SQLException("Banco nao passou no PRAGMA integrity_check.");
    }

    private static long contarVinculosOrfaos(Connection c) throws SQLException {
        try (Statement stmt = c.createStatement(); ResultSet rs = stmt.executeQuery("PRAGMA foreign_key_check")) {
            long quantidade = 0;
            while (rs.next()) quantidade++;
            return quantidade;
        }
    }

    private static void exigirSemVinculosOrfaos(Connection c) throws SQLException {
        long orfaos = contarVinculosOrfaos(c);
        if (orfaos > 0) throw new SQLException("Existem " + orfaos + " vinculos orfaos. Corrija antes de migrar.");
    }
}
