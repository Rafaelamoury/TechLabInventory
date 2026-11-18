package Techlab.db;

import org.junit.jupiter.api.Test;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ConexaoBancoTest {
    @Test
    void bloqueiaBancoLegadoSemAbriLo() {
        assertThrows(IllegalArgumentException.class,
                () -> new ConexaoBanco(Path.of("techlab.db")));
    }

    @Test
    void bloqueiaOutrosCaminhosNaoAutorizados() {
        assertThrows(IllegalArgumentException.class,
                () -> new ConexaoBanco(Path.of("dados", "outro-banco.db")));
        assertThrows(IllegalArgumentException.class,
                () -> new ConexaoBanco(null));
    }
}
