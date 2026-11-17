package Techlab;

import Techlab.dao.AtivoDAO;
import Techlab.dao.AtivoResumo;
import Techlab.dao.ChamadoDAO;
import Techlab.dao.ChamadoResumo;
import Techlab.db.ConexaoBanco;
import Techlab.service.ComplementacaoLegadosService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Testa a complementacao em copia temporaria da COPIA, nunca no banco original. */
public final class TesteComplementacaoLegados {
    private TesteComplementacaoLegados() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !"techlab-fase2-teste.db".equals(Path.of(args[0]).getFileName().toString())) {
            throw new IllegalArgumentException("Uso: Techlab.TesteComplementacaoLegados techlab-fase2-teste.db");
        }
        Path origem = Path.of(args[0]).toAbsolutePath().normalize();
        if (!Files.isRegularFile(origem)) throw new IllegalArgumentException("Copia de testes nao encontrada.");
        byte[] antes = resumoHash(origem);
        Path temporario = Files.createTempDirectory("techlab-legados-qa-");
        Path destino = temporario.resolve("techlab-fase2-teste.db");
        try {
            Files.copy(origem, destino, StandardCopyOption.REPLACE_EXISTING);
            ConexaoBanco banco = new ConexaoBanco(destino);
            banco.inicializarBanco();
            AtivoDAO ativos = new AtivoDAO(banco);
            ChamadoDAO chamados = new ChamadoDAO(banco);
            ComplementacaoLegadosService svc = new ComplementacaoLegadosService(banco);
            int totalAtivos = ativos.listarResumos().size();
            int totalChamados = chamados.listarResumos().size();

            int ativosCompletados = 0;
            for (AtivoResumo ativo : ativos.listarResumos()) {
                if (ativo.completo()) continue;
                List<String> campos = svc.camposPendentesAtivo(ativo.id());
                Map<String, String> dados = new LinkedHashMap<>();
                for (String campo : campos) dados.put(campo, valorDeExemplo(campo));
                if (campos.contains("data_aquisicao")) {
                    // Sem preenchimento parcial: uma data invalida deve rejeitar a operacao inteira.
                    Map<String, String> invalidos = new LinkedHashMap<>(dados);
                    invalidos.put("data_aquisicao", "nao-e-data");
                    boolean falhou = false;
                    try { svc.completarAtivo(ativo.id(), invalidos); }
                    catch (IllegalArgumentException esperado) { falhou = true; }
                    verificar(falhou, "Data invalida foi aceita.");
                    verificar(!svc.camposPendentesAtivo(ativo.id()).isEmpty(), "Data invalida alterou cadastro.");
                }
                svc.completarAtivo(ativo.id(), dados);
                verificar(svc.camposPendentesAtivo(ativo.id()).isEmpty(), "Ativo segue incompleto.");
                verificar(ativos.buscarPorId(ativo.id()).getId() == ativo.id(), "ID do ativo alterado.");
                ativosCompletados++;
            }

            int chamadosCompletados = 0;
            for (ChamadoResumo chamado : chamados.listarResumos()) {
                if (chamado.completo()) continue;
                List<String> campos = svc.camposPendentesChamado(chamado.id());
                Map<String, String> dados = new LinkedHashMap<>();
                for (String campo : campos) dados.put(campo, valorDeExemplo(campo));
                svc.completarChamado(chamado.id(), dados);
                verificar(chamados.buscarPorId(chamado.id()).getAtivoRelacionadoId() == chamado.ativoId(),
                        "Vinculo de chamado foi alterado.");
                chamadosCompletados++;
            }
            verificar(ativos.listarResumos().size() == totalAtivos, "Quantidade de ativos mudou.");
            verificar(chamados.listarResumos().size() == totalChamados, "Quantidade de chamados mudou.");
            try (Connection conn = banco.obterConexao(); Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("PRAGMA foreign_key_check")) {
                verificar(!rs.next(), "Vinculos inconsistentes.");
            }
            verificar(MessageDigest.isEqual(antes, resumoHash(origem)), "Banco de testes original foi alterado.");
            System.out.printf("ATIVOS COMPLETADOS NA COPIA TEMPORARIA: %d%n", ativosCompletados);
            System.out.printf("CHAMADOS COMPLETADOS NA COPIA TEMPORARIA: %d%n", chamadosCompletados);
            System.out.printf("CONTAGENS PRESERVADAS: ativos=%d, chamados=%d%n", totalAtivos, totalChamados);
            System.out.println("VALIDACOES DE DATA E INTEGRIDADE: OK");
            System.out.println("TESTE COMPLEMENTACAO CONCLUIDO. Bancos reais e de testes originais preservados.");
        } finally {
            Files.deleteIfExists(destino);
            Files.deleteIfExists(temporario);
        }
    }

    private static String valorDeExemplo(String campo) {
        return switch (campo) {
            case "status" -> "Ativo";
            case "data_aquisicao" -> "2000-01-01";
            case "ip_estatico" -> "192.0.2.10";
            case "sistema_operacional" -> "Linux (teste)";
            case "memoria_ram_gb" -> "16";
            case "departamento" -> "Setor de teste";
            case "responsavel" -> "Responsavel ficticio";
            case "numero_portas" -> "24";
            case "titulo" -> "Titulo de teste";
            case "prioridade" -> "MEDIA";
            case "data_abertura" -> "2000-01-02T10:00";
            default -> throw new IllegalArgumentException("Campo desconhecido: " + campo);
        };
    }

    private static byte[] resumoHash(Path caminho) throws Exception {
        return MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(caminho));
    }

    private static void verificar(boolean ok, String mensagem) {
        if (!ok) throw new IllegalStateException(mensagem);
    }
}
