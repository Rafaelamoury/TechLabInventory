package Techlab.dao;

import Techlab.exceptions.EquipamentoComChamadosException;
import Techlab.exceptions.EquipamentoNaoEncontradoException;
import Techlab.negocio.AtivoTI;
import Techlab.negocio.ChamadoSuporte;
import Techlab.negocio.DispositivoRede;
import Techlab.negocio.EstacaoTrabalho;
import Techlab.negocio.PrioridadeChamado;
import Techlab.negocio.Servidor;
import Techlab.testsupport.BancoTemporario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AtivoDAOTest {
    @TempDir Path pastaTemporaria;
    private static final LocalDateTime DATA = LocalDateTime.of(2026, 11, 17, 10, 0);

    @Test
    void salvaConsultaAtualizaEExcluiServidor() throws SQLException {
        AtivoDAO dao = new AtivoDAO(new BancoTemporario(pastaTemporaria));
        Servidor novo = new Servidor("SRV-101", "Servidor de Aplicacao", "Ativo", DATA,
                "192.0.2.101", "Linux", 16);
        dao.salvar(novo);
        assertTrue(novo.getId() > 0);
        assertEquals(1, dao.listarTodos().size());

        AtivoTI encontrado = dao.buscarPorId(novo.getId());
        assertInstanceOf(Servidor.class, encontrado);
        Servidor recuperado = (Servidor) encontrado;
        assertEquals(16, recuperado.getMemoriaRamGb());
        assertEquals("Linux", recuperado.getSistemaOperacional());

        recuperado.setStatus("Manutencao");
        recuperado.setMemoriaRamGb(32);
        recuperado.setIpEstatico("192.0.2.102");
        dao.atualizar(recuperado);
        Servidor atualizado = (Servidor) dao.buscarPorId(novo.getId());
        assertEquals("Manutencao", atualizado.getStatus());
        assertEquals(32, atualizado.getMemoriaRamGb());
        assertEquals("192.0.2.102", atualizado.getIpEstatico());

        dao.deletar(novo.getId());
        assertNull(dao.buscarPorId(novo.getId()));
        assertTrue(dao.listarResumos().isEmpty());
    }

    @Test
    void persisteEstacaoEDispositivoDeRede() throws SQLException {
        AtivoDAO dao = new AtivoDAO(new BancoTemporario(pastaTemporaria));
        EstacaoTrabalho estacao = new EstacaoTrabalho("EST-101", "Notebook", "Ativo", DATA,
                "TI", "Rafael");
        DispositivoRede rede = new DispositivoRede("RED-101", "Switch", "Ativo", DATA, 24);
        dao.salvar(estacao);
        dao.salvar(rede);

        EstacaoTrabalho estacaoLida = (EstacaoTrabalho) dao.buscarPorId(estacao.getId());
        DispositivoRede redeLida = (DispositivoRede) dao.buscarPorId(rede.getId());
        assertEquals("TI", estacaoLida.getDepartamento());
        assertEquals("Rafael", estacaoLida.getResponsavel());
        assertEquals(24, redeLida.getNumeroPortas());
    }

    @Test
    void impedePatrimonioDuplicado() throws SQLException {
        AtivoDAO dao = new AtivoDAO(new BancoTemporario(pastaTemporaria));
        dao.salvar(new DispositivoRede("RED-101", "Switch 1", "Ativo", DATA, 8));
        assertThrows(SQLException.class,
                () -> dao.salvar(new DispositivoRede("RED-101", "Switch 2", "Ativo", DATA, 12)));
        assertEquals(1, dao.listarResumos().size());
    }

    @Test
    void bloqueiaExclusaoSeHouverChamadoVinculado() throws SQLException {
        BancoTemporario banco = new BancoTemporario(pastaTemporaria);
        AtivoDAO ativos = new AtivoDAO(banco);
        ChamadoDAO chamados = new ChamadoDAO(banco);
        DispositivoRede ativo = new DispositivoRede("RED-102", "Switch", "Ativo", DATA, 8);
        ativos.salvar(ativo);
        ChamadoSuporte chamado = new ChamadoSuporte("Incidente", "Sem rede",
                PrioridadeChamado.ALTA, DATA, ativo.getId());
        chamados.salvar(chamado);
        assertThrows(EquipamentoComChamadosException.class, () -> ativos.deletar(ativo.getId()));
        assertNotNull(ativos.buscarPorId(ativo.getId()));
        chamados.deletar(chamado.getId());
        ativos.deletar(ativo.getId());
        assertNull(ativos.buscarPorId(ativo.getId()));
    }

    @Test
    void informaExcecaoAoAtualizarOuExcluirAtivoAusente() throws SQLException {
        AtivoDAO dao = new AtivoDAO(new BancoTemporario(pastaTemporaria));
        DispositivoRede inexistente = new DispositivoRede("RED-999", "Switch", "Ativo", DATA, 8);
        inexistente.setId(99999);
        assertThrows(EquipamentoNaoEncontradoException.class, () -> dao.atualizar(inexistente));
        assertThrows(EquipamentoNaoEncontradoException.class, () -> dao.deletar(99999));
    }
}
