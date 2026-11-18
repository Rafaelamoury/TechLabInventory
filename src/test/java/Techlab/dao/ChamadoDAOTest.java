package Techlab.dao;

import Techlab.exceptions.ChamadoNaoEncontradoException;
import Techlab.negocio.ChamadoSuporte;
import Techlab.negocio.DispositivoRede;
import Techlab.negocio.PrioridadeChamado;
import Techlab.testsupport.BancoTemporario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ChamadoDAOTest {
    @TempDir Path pastaTemporaria;
    private static final LocalDateTime DATA = LocalDateTime.of(2026, 11, 17, 10, 0);

    @Test
    void salvaConsultaAtualizaEExcluiChamadoCompleto() throws SQLException {
        BancoTemporario banco = new BancoTemporario(pastaTemporaria);
        AtivoDAO ativos = new AtivoDAO(banco);
        ChamadoDAO dao = new ChamadoDAO(banco);
        DispositivoRede ativo = new DispositivoRede("RED-200", "Switch", "Ativo", DATA, 8);
        ativos.salvar(ativo);

        ChamadoSuporte novo = new ChamadoSuporte("Falha de rede", "Sem acesso a internet",
                PrioridadeChamado.MEDIA, DATA, ativo.getId());
        dao.salvar(novo);
        assertTrue(novo.getId() > 0);
        assertEquals(1, dao.listarTodos().size());
        ChamadoSuporte lido = dao.buscarPorId(novo.getId());
        assertEquals("Falha de rede", lido.getTitulo());
        assertEquals(ativo.getId(), lido.getAtivoRelacionadoId());

        lido.setTitulo("Falha corrigida");
        lido.setDescricao("Conectividade restabelecida");
        lido.setPrioridade(PrioridadeChamado.BAIXA);
        lido.setDataAbertura(DATA.plusHours(1));
        dao.atualizar(lido);
        ChamadoSuporte atualizado = dao.buscarPorId(novo.getId());
        assertEquals("Falha corrigida", atualizado.getTitulo());
        assertEquals("Conectividade restabelecida", atualizado.getDescricao());
        assertEquals(PrioridadeChamado.BAIXA, atualizado.getPrioridade());
        assertEquals(DATA.plusHours(1), atualizado.getDataAbertura());

        dao.deletar(novo.getId());
        assertNull(dao.buscarPorId(novo.getId()));
    }

    @Test
    void rejeitaChamadoAssociadoAAtivoInexistente() throws SQLException {
        ChamadoDAO dao = new ChamadoDAO(new BancoTemporario(pastaTemporaria));
        ChamadoSuporte invalido = new ChamadoSuporte("Falha", "Nao existe ativo",
                PrioridadeChamado.ALTA, DATA, 99999);
        assertThrows(SQLException.class, () -> dao.salvar(invalido));
        assertTrue(dao.listarResumos().isEmpty());
    }

    @Test
    void lançaExcecaoAoAtualizarOuExcluirChamadoAusente() throws SQLException {
        ChamadoDAO dao = new ChamadoDAO(new BancoTemporario(pastaTemporaria));
        ChamadoSuporte inexistente = new ChamadoSuporte("Falha", "Teste",
                PrioridadeChamado.BAIXA, DATA, 1);
        inexistente.setId(99999);
        assertThrows(ChamadoNaoEncontradoException.class, () -> dao.atualizar(inexistente));
        assertThrows(ChamadoNaoEncontradoException.class, () -> dao.deletar(99999));
    }
}
