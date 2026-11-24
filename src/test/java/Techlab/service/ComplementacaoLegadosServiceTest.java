package Techlab.service;

import Techlab.dao.AtivoDAO;
import Techlab.dao.ChamadoDAO;
import Techlab.negocio.DispositivoRede;
import Techlab.negocio.ChamadoSuporte;
import Techlab.negocio.PrioridadeChamado;
import Techlab.testsupport.BancoTemporario;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ComplementacaoLegadosServiceTest {

    @TempDir
    Path pastaTemporaria;

    private BancoTemporario banco;
    private ComplementacaoLegadosService service;

    @BeforeEach
    void configurar() throws SQLException {
        banco = new BancoTemporario(pastaTemporaria);
        service = new ComplementacaoLegadosService(banco);

        executarSql("""
                INSERT INTO ativos
                (id, patrimonio, nome, categoria, detalhe_especifico,
                 status, data_aquisicao, numero_portas)
                VALUES
                (1, 'RED-LEGADO', 'Switch', 'DispositivoRede',
                 '24', NULL, NULL, 24)
                """);

        executarSql("""
                INSERT INTO chamados
                (id, descricao, ativo_id, titulo, prioridade, data_abertura)
                VALUES
                (1, 'Falha de rede', 1, NULL, NULL, NULL)
                """);
    }

    @Test
    void completaAtivoComDadosValidos() throws SQLException {
        service.completarAtivo(1, dadosAtivo());

        DispositivoRede ativo =
                (DispositivoRede) new AtivoDAO(banco).buscarPorId(1);

        assertNotNull(ativo);
        assertEquals(1, ativo.getId());
        assertEquals("RED-LEGADO", ativo.getCodigoPatrimonio());
        assertEquals("Switch", ativo.getModelo());
        assertEquals("Ativo", ativo.getStatus());
        assertEquals(24, ativo.getNumeroPortas());
        assertTrue(service.camposPendentesAtivo(1).isEmpty());
    }

    @Test
    void completaChamadoComDadosValidos() throws SQLException {
        service.completarChamado(1, dadosChamado());

        ChamadoSuporte chamado =
                new ChamadoDAO(banco).buscarPorId(1);

        assertNotNull(chamado);
        assertEquals(1, chamado.getId());
        assertEquals("Incidente de rede", chamado.getTitulo());
        assertEquals("Falha de rede", chamado.getDescricao());
        assertEquals(PrioridadeChamado.ALTA, chamado.getPrioridade());
        assertEquals(1, chamado.getAtivoRelacionadoId());
        assertTrue(service.camposPendentesChamado(1).isEmpty());
    }

    @Test
    void rejeitaDataInvalidaSemAlterarRegistro() throws SQLException {
        assertThrows(IllegalArgumentException.class,
                () -> service.completarAtivo(1, Map.of(
                        "status", "Ativo",
                        "data_aquisicao", "data-invalida")));

        assertNull(consultarTexto(
                "SELECT status FROM ativos WHERE id = 1"));
    }

    @Test
    void rejeitaCampoNaoPermitido() throws SQLException {
        assertThrows(IllegalArgumentException.class,
                () -> service.completarAtivo(1, Map.of(
                        "status", "Ativo",
                        "data_aquisicao", "2026-11-24",
                        "nome", "Nome alterado")));

        assertEquals("Switch", consultarTexto(
                "SELECT nome FROM ativos WHERE id = 1"));
        assertNull(consultarTexto(
                "SELECT status FROM ativos WHERE id = 1"));
    }

    @Test
    void desfazComplementacaoAtivoQuandoVerificacaoFalha()
            throws SQLException {

        executarSql("""
                CREATE TRIGGER provocar_falha_ativo
                AFTER UPDATE ON ativos
                BEGIN
                    UPDATE ativos
                    SET numero_portas = 0
                    WHERE id = NEW.id;
                END
                """);

        assertThrows(SQLException.class,
                () -> service.completarAtivo(1, dadosAtivo()));

        assertNull(consultarTexto(
                "SELECT status FROM ativos WHERE id = 1"));

        assertNull(consultarTexto(
                "SELECT data_aquisicao FROM ativos WHERE id = 1"));

        assertEquals("24", consultarTexto(
                "SELECT numero_portas FROM ativos WHERE id = 1"));

        assertEquals("Switch", consultarTexto(
                "SELECT nome FROM ativos WHERE id = 1"));
    }

    @Test
    void desfazComplementacaoChamadoQuandoVerificacaoFalha()
            throws SQLException {

        executarSql("""
                CREATE TRIGGER provocar_falha_chamado
                AFTER UPDATE ON chamados
                BEGIN
                    UPDATE chamados
                    SET prioridade = 'INVALIDA'
                    WHERE id = NEW.id;
                END
                """);

        assertThrows(SQLException.class,
                () -> service.completarChamado(1, dadosChamado()));

        assertNull(consultarTexto(
                "SELECT titulo FROM chamados WHERE id = 1"));

        assertNull(consultarTexto(
                "SELECT prioridade FROM chamados WHERE id = 1"));

        assertNull(consultarTexto(
                "SELECT data_abertura FROM chamados WHERE id = 1"));

        assertEquals("Falha de rede", consultarTexto(
                "SELECT descricao FROM chamados WHERE id = 1"));
    }

    private static Map<String, String> dadosAtivo() {
        return Map.of(
                "status", "Ativo",
                "data_aquisicao", "2026-11-24");
    }

    private static Map<String, String> dadosChamado() {
        return Map.of(
                "titulo", "Incidente de rede",
                "prioridade", "ALTA",
                "data_abertura", "2026-11-24T09:00");
    }

    private void executarSql(String sql) throws SQLException {
        try (Connection conexao = banco.obterConexao();
             Statement stmt = conexao.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    private String consultarTexto(String sql) throws SQLException {
        try (Connection conexao = banco.obterConexao();
             Statement stmt = conexao.createStatement();
             ResultSet resultado = stmt.executeQuery(sql)) {

            assertTrue(resultado.next());
            return resultado.getString(1);
        }
    }
}
