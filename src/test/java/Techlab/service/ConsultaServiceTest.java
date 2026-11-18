package Techlab.service;

import Techlab.dao.AtivoResumo;
import Techlab.dao.ChamadoResumo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConsultaServiceTest {
    @Test
    void filtraAtivosSemDiferenciarMaiusculasEOrdenaPorPatrimonio() {
        List<AtivoResumo> ativos = List.of(
                new AtivoResumo(2, "ZZ-2", "Switch de rede", "DispositivoRede", ""),
                new AtivoResumo(1, "AA-1", "Switch principal", "DispositivoRede", "status"),
                new AtivoResumo(3, "BB-3", "Servidor", "Servidor", ""));
        List<AtivoResumo> filtrados = ConsultaService.filtrarEOrdenarAtivos(ativos, "SWITCH");
        assertEquals(List.of(1, 2), filtrados.stream().map(AtivoResumo::id).toList());
        assertEquals(3, ConsultaService.filtrarEOrdenarAtivos(ativos, "").size());
        assertEquals(0, ConsultaService.filtrarEOrdenarAtivos(ativos, "nao existe").size());
    }

    @Test
    void filtraChamadosOrdenaEIncluiRegistrosLegados() {
        List<ChamadoResumo> chamados = List.of(
                new ChamadoResumo(2, "Zebra na rede", 1, "titulo"),
                new ChamadoResumo(1, "Aviso na rede", 1, ""),
                new ChamadoResumo(3, "Impressora", 1, ""));
        List<ChamadoResumo> filtrados = ConsultaService.filtrarEOrdenarChamados(chamados, "REDE");
        assertEquals(List.of(1, 2), filtrados.stream().map(ChamadoResumo::id).toList());
        assertEquals(3, ConsultaService.filtrarEOrdenarChamados(chamados, "").size());
    }
}
