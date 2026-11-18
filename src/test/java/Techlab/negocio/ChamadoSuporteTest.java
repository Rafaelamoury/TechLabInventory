package Techlab.negocio;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ChamadoSuporteTest {
    private static final LocalDateTime DATA = LocalDateTime.of(2026, 11, 17, 10, 0);

    @Test
    void criaChamadoComRelacionamentoEData() {
        ChamadoSuporte chamado = new ChamadoSuporte("Teste", "Descricao",
                PrioridadeChamado.ALTA, DATA, 7);
        assertEquals(0, chamado.getId());
        assertEquals(7, chamado.getAtivoRelacionadoId());
        assertEquals(PrioridadeChamado.ALTA, chamado.getPrioridade());
        assertEquals(DATA, chamado.getDataAbertura());
    }

    @Test
    void exigeDadosValidos() {
        assertThrows(IllegalArgumentException.class,
                () -> new ChamadoSuporte(" ", "Descricao", PrioridadeChamado.MEDIA, DATA, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new ChamadoSuporte("Teste", "Descricao", null, DATA, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new ChamadoSuporte("Teste", "Descricao", PrioridadeChamado.MEDIA, DATA, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new ChamadoSuporte("Teste", "Descricao", PrioridadeChamado.MEDIA, null, 1));
    }
}
