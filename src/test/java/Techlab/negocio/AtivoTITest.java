package Techlab.negocio;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AtivoTITest {
    private static final LocalDateTime DATA = LocalDateTime.of(2026, 11, 17, 10, 0);

    @Test
    void polimorfismoCalculaCustoDeCadaSubclasse() {
        AtivoTI servidor = new Servidor("SRV-J-001", "Servidor", "Ativo", DATA,
                "192.0.2.10", "Linux", 16);
        AtivoTI estacao = new EstacaoTrabalho("EST-J-001", "Estacao", "Ativo", DATA,
                "TI", "Equipe");
        AtivoTI rede = new DispositivoRede("RED-J-001", "Switch", "Ativo", DATA, 8);

        assertEquals(180.0, servidor.calcularCustoManutencaoMensal(), 0.001);
        assertEquals(80.0, estacao.calcularCustoManutencaoMensal(), 0.001);
        assertEquals(1600.0, rede.calcularCustoManutencaoMensal(), 0.001);
        assertEquals("Servidor", servidor.getClass().getSimpleName());
    }

    @Test
    void impedeCamposObrigatoriosVazios() {
        assertThrows(IllegalArgumentException.class,
                () -> new DispositivoRede(" ", "Switch", "Ativo", DATA, 8));
        assertThrows(IllegalArgumentException.class,
                () -> new EstacaoTrabalho("EST-1", "Estacao", "Ativo", DATA, "TI", " "));
        assertThrows(IllegalArgumentException.class,
                () -> new Servidor("SRV-1", "Servidor", "Ativo", DATA, "IP", "Linux", 0));
    }

    @Test
    void validaIdEPropriedades() {
        DispositivoRede rede = new DispositivoRede(" RED-001 ", "Switch", "Ativo", DATA, 4);
        assertEquals("RED-001", rede.getCodigoPatrimonio());
        assertEquals(0, rede.getId());
        assertThrows(IllegalArgumentException.class, () -> rede.setId(0));
        assertThrows(IllegalArgumentException.class, () -> rede.setNumeroPortas(-1));
        assertThrows(IllegalArgumentException.class, () -> rede.setDataAquisicao(null));
        rede.setId(10);
        assertEquals(10, rede.getId());
    }
}
