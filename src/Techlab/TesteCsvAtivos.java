package Techlab;

import Techlab.db.ConexaoBanco;
import Techlab.dto.AtivoCsvRegistro;
import Techlab.repository.AtivoArquivoRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Teste integrado usando APENAS a copia do SQLite. */
public final class TesteCsvAtivos {
    private static int contar(ConexaoBanco db, String tabela) throws Exception {
        try (Connection conn = db.obterConexao();
             PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM " + tabela);
             ResultSet rs = stmt.executeQuery()) { return rs.next() ? rs.getInt(1) : -1; }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Informe techlab-fase2-teste.db");
        ConexaoBanco db = new ConexaoBanco(Path.of(args[0]));
        db.inicializarBanco();
        AtivoArquivoRepository repo = new AtivoArquivoRepository(db);
        int antesAtivos = contar(db, "ativos"), antesChamados = contar(db, "chamados");
        Path pasta = Files.createTempDirectory("techlab-csv-teste-");
        Path exportacao = pasta.resolve("ativos.csv");
        int exportados = repo.exportarBanco(exportacao);
        List<AtivoCsvRegistro> lidos = repo.carregarTodos(exportacao);
        if (exportados != antesAtivos || lidos.size() != antesAtivos)
            throw new AssertionError("Exportacao/importacao perdeu ativos.");
        System.out.println("CSV EXPORTADO: " + exportados + " ativos; " + lidos.size() + " lidos.");
        int incompletos = (int) lidos.stream().filter(r -> !r.completo()).count();
        System.out.println("LEGADOS INCOMPLETOS PRESERVADOS: " + incompletos);

        AtivoArquivoRepository.ResultadoImportacao existentes = repo.importarNovos(exportacao);
        if (existentes.importados() != 0 || existentes.jaExistentes() != exportados)
            throw new AssertionError("Reimportacao tentou duplicar registros existentes.");
        System.out.println("REIMPORTACAO: zero duplicatas, existentes preservados.");

        // CSV especial para provar aspas e ponto-e-virgula em campos textuais.
        Path extra = pasta.resolve("novos.csv");
        String patrimonio = "CSV-TESTE-" + System.nanoTime();
        List<AtivoCsvRegistro> dados = new ArrayList<>();
        dados.add(new AtivoCsvRegistro(999, patrimonio, "Switch \"Auditoria\"; 48P",
                "DispositivoRede", "48", "Ativo", LocalDateTime.now().toString(),
                null, null, null, null, null, 48));
        repo.salvarTodos(dados, extra);
        List<AtivoCsvRegistro> recuperados = repo.carregarTodos(extra);
        if (!recuperados.get(0).modelo().equals("Switch \"Auditoria\"; 48P"))
            throw new AssertionError("CSV nao preservou aspas e ponto-e-virgula.");
        System.out.println("CSV COM ASPAS E PONTO-E-VIRGULA: OK");

        try {
            AtivoArquivoRepository.ResultadoImportacao novos = repo.importarNovos(extra);
            if (novos.importados() != 1) throw new AssertionError("Importacao de novo ativo falhou.");
            System.out.println("IMPORTACAO NOVO ATIVO: OK");
        } finally {
            try (Connection conn = db.obterConexao();
                 PreparedStatement stmt = conn.prepareStatement("DELETE FROM ativos WHERE patrimonio = ?")) {
                stmt.setString(1, patrimonio);
                stmt.executeUpdate();
            }
        }
        // Se um arquivo contem uma linha nova valida e outra incompleta,
        // a importacao deve ser TOTALMENTE revertida (sem cadastro parcial).
        Path invalido = pasta.resolve("importacao-parcial.csv");
        String patrimonioRollback = "CSV-ROLLBACK-" + System.nanoTime();
        List<AtivoCsvRegistro> parcialmenteValidos = List.of(
                new AtivoCsvRegistro(1000, patrimonioRollback, "Switch de teste", "DispositivoRede",
                        "8", "Ativo", LocalDateTime.now().toString(), null, null, null, null, null, 8),
                new AtivoCsvRegistro(1001, "CSV-INCOMPLETO-" + System.nanoTime(), "Switch sem data",
                        "DispositivoRede", null, null, null, null, null, null, null, null, null));
        repo.salvarTodos(parcialmenteValidos, invalido);
        try {
            repo.importarNovos(invalido);
            throw new AssertionError("Deveria rejeitar importacao com ativo novo incompleto.");
        } catch (java.io.IOException esperado) {
            if (!esperado.getMessage().contains("INCOMPLETO")) throw esperado;
        }
        if (contar(db, "ativos") != antesAtivos)
            throw new AssertionError("Importacao parcial deveria ter sido revertida.");
        System.out.println("ROLLBACK DE IMPORTACAO INCOMPLETA: OK");

        int depoisAtivos = contar(db, "ativos"), depoisChamados = contar(db, "chamados");
        if (depoisAtivos != antesAtivos || depoisChamados != antesChamados)
            throw new AssertionError("Quantidade de registros mudou apos a limpeza de teste.");
        System.out.printf("ANTES: %d ativos, %d chamados | DEPOIS: %d ativos, %d chamados%n",
                antesAtivos, antesChamados, depoisAtivos, depoisChamados);
        System.out.println("TESTE CSV CONCLUIDO COM SUCESSO. Banco original nao acessado.");
        System.out.println("Arquivos CSV temporarios: " + pasta);
    }
}
