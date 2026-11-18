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
}
