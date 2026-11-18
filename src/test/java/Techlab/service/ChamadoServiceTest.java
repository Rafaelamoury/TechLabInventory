package Techlab.service;

import Techlab.interfaces.INotificavel;
import Techlab.interfaces.IPersistencia;
import Techlab.negocio.ChamadoSuporte;
import Techlab.negocio.PrioridadeChamado;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ChamadoServiceTest {
    @Test
    void emiteNotificacaoAposSalvarEAtualizar() throws SQLException {
        RepositorioFalso repositorio = new RepositorioFalso();
        AtomicReference<String> mensagem = new AtomicReference<>();
        INotificavel notificador = mensagem::set;
        ChamadoService service = new ChamadoService(repositorio, notificador);
        ChamadoSuporte chamado = chamado();
        service.abrir(chamado);
        assertEquals(12, chamado.getId());
        assertTrue(mensagem.get().contains("aberto"));
        mensagem.set(null);
        chamado.setPrioridade(PrioridadeChamado.ALTA);
        service.atualizar(chamado);
        assertTrue(mensagem.get().contains("atualizado"));
        assertTrue(mensagem.get().contains("ALTA"));
    }

    @Test
    void naoNotificaQuandoPersistenciaFalha() {
        AtomicReference<String> mensagem = new AtomicReference<>();
        RepositorioFalso repositorio = new RepositorioFalso();
        repositorio.falhar = true;
        ChamadoService service = new ChamadoService(repositorio, mensagem::set);
        assertThrows(SQLException.class, () -> service.abrir(chamado()));
        assertNull(mensagem.get());
    }

    private static ChamadoSuporte chamado() {
        return new ChamadoSuporte("Falha", "Rede sem sinal", PrioridadeChamado.MEDIA,
                LocalDateTime.of(2026, 11, 18, 8, 0), 1);
    }

    private static final class RepositorioFalso implements IPersistencia<ChamadoSuporte> {
        boolean falhar;
        public void salvar(ChamadoSuporte c) throws SQLException {
            if (falhar) throw new SQLException("Falha simulada");
            c.setId(12);
        }
        public void atualizar(ChamadoSuporte c) throws SQLException {
            if (falhar) throw new SQLException("Falha simulada");
        }
        public ChamadoSuporte buscarPorId(int id) { return null; }
        public List<ChamadoSuporte> listarTodos() { return List.of(); }
        public void deletar(int id) { }
    }
}
