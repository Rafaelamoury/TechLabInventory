package Techlab.repository;

import Techlab.dao.AtivoDAO;
import Techlab.negocio.DispositivoRede;
import Techlab.testsupport.BancoTemporario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AtivoArquivoRepositoryTest {
    @TempDir Path pastaTemporaria;
    private static final LocalDateTime DATA = LocalDateTime.of(2026, 11, 17, 10, 0);

    @Test
    void exportaEImportaCsvSemDuplicarPatrimonios() throws IOException, SQLException {
        Path origem = pastaTemporaria.resolve("origem");
        Path destino = pastaTemporaria.resolve("destino");
        Files.createDirectories(origem);
        Files.createDirectories(destino);
        BancoTemporario bdOrigem = new BancoTemporario(origem);
        AtivoDAO ativos = new AtivoDAO(bdOrigem);
        ativos.salvar(new DispositivoRede("RED-CSV-01", "Switch; 24 portas", "Ativo", DATA, 24));
        AtivoArquivoRepository exportador = new AtivoArquivoRepository(bdOrigem);
        Path arquivo = pastaTemporaria.resolve("exportacao.csv");
        assertEquals(1, exportador.exportarBanco(arquivo));
        assertTrue(Files.size(arquivo) > 0);

        BancoTemporario bdDestino = new BancoTemporario(destino);
        AtivoArquivoRepository importador = new AtivoArquivoRepository(bdDestino);
        AtivoArquivoRepository.ResultadoImportacao resultado = importador.importarNovos(arquivo);
        assertEquals(1, resultado.importados());
        assertEquals(0, resultado.jaExistentes());
        assertEquals("Switch; 24 portas", new AtivoDAO(bdDestino).listarTodos().get(0).getModelo());

        AtivoArquivoRepository.ResultadoImportacao repetido = importador.importarNovos(arquivo);
        assertEquals(0, repetido.importados());
        assertEquals(1, repetido.jaExistentes());
        assertEquals(1, new AtivoDAO(bdDestino).listarTodos().size());
    }

    @Test
    void naoSobrescreveCsvExistente() throws IOException, SQLException {
        BancoTemporario bd = new BancoTemporario(pastaTemporaria);
        AtivoArquivoRepository repo = new AtivoArquivoRepository(bd);
        Path arquivo = pastaTemporaria.resolve("saida.csv");
        assertEquals(0, repo.exportarBanco(arquivo));
        assertThrows(IOException.class, () -> repo.exportarBanco(arquivo));
    }

    @Test
    void naoDeixaArquivoIncompletoQuandoExportacaoFalha()
            throws IOException, SQLException {

        BancoTemporario bd = new BancoTemporario(pastaTemporaria);
        AtivoDAO ativos = new AtivoDAO(bd);

        ativos.salvar(new DispositivoRede(
                "RED-001", "Switch valido", "Ativo", DATA, 24));

        ativos.salvar(new DispositivoRede(
                "RED-002", "Switch\nModelo invalido", "Ativo", DATA, 8));

        AtivoArquivoRepository repo = new AtivoArquivoRepository(bd);

        Path pastaExportacao = pastaTemporaria.resolve("exportacoes");
        Path arquivo = pastaExportacao.resolve("saida.csv");

        assertThrows(IllegalArgumentException.class,
                () -> repo.exportarBanco(arquivo));

        assertFalse(Files.exists(arquivo));

        try (var arquivos = Files.list(pastaExportacao)) {
            assertEquals(0L, arquivos.count());
        }
    }

    @Test
    void preservaConteudoDeCsvExistente()
            throws IOException, SQLException {

        BancoTemporario bd = new BancoTemporario(pastaTemporaria);
        AtivoArquivoRepository repo = new AtivoArquivoRepository(bd);

        Path arquivo = pastaTemporaria.resolve("existente.csv");
        String conteudoOriginal = "Conteudo original preservado";

        Files.writeString(arquivo, conteudoOriginal);

        assertThrows(IOException.class,
                () -> repo.exportarBanco(arquivo));

        assertEquals(conteudoOriginal, Files.readString(arquivo));
    }

    @Test
    void exportacaoBemSucedidaNaoDeixaArquivosTemporarios()
            throws IOException, SQLException {

        BancoTemporario bd = new BancoTemporario(pastaTemporaria);
        AtivoDAO ativos = new AtivoDAO(bd);

        ativos.salvar(new DispositivoRede(
                "RED-003", "Switch", "Ativo", DATA, 24));

        AtivoArquivoRepository repo = new AtivoArquivoRepository(bd);

        Path pastaExportacao = pastaTemporaria.resolve("exportacoes");
        Path arquivo = pastaExportacao.resolve("saida.csv");

        assertEquals(1, repo.exportarBanco(arquivo));
        assertTrue(Files.exists(arquivo));
        assertEquals(1, repo.carregarTodos(arquivo).size());

        try (var arquivos = Files.list(pastaExportacao)) {
            assertEquals(1L, arquivos.count());
        }
    }
}
