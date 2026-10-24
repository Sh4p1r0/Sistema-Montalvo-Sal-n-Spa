package proyectodp.seguridad;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import proyectodp.modelo.Usuario;

@DisplayName("Seguridad: gestión de sesiones con token")
class SesionManagerTest {

    private Usuario usuario() {
        return new Usuario(1, "recep", "hash", "RECEPCIONISTA", "Recepción Principal", "Caja");
    }

    @Test
    void creaYValidaUnToken() {
        SesionManager sesiones = new SesionManager();
        String token = sesiones.crear(usuario());

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertEquals("recep", sesiones.validar(token).getUsername());
        assertEquals(1, sesiones.activas());
    }

    @Test
    void cadaSesionTieneUnTokenDistinto() {
        SesionManager sesiones = new SesionManager();

        assertNotEquals(sesiones.crear(usuario()), sesiones.crear(usuario()));
        assertEquals(2, sesiones.activas());
    }

    @Test
    void unTokenInvalidoNoDevuelveUsuario() {
        SesionManager sesiones = new SesionManager();

        assertNull(sesiones.validar("no-existe"));
        assertNull(sesiones.validar(null));
        assertNull(sesiones.validar("  "));
    }

    @Test
    void cerrarSesionInvalidaElToken() {
        SesionManager sesiones = new SesionManager();
        String token = sesiones.crear(usuario());

        sesiones.cerrar(token);

        assertNull(sesiones.validar(token));
        assertEquals(0, sesiones.activas());
    }

    @Test
    void elTokenExpira() throws InterruptedException {
        SesionManager sesiones = new SesionManager(Duration.ofMillis(30));
        String token = sesiones.crear(usuario());

        assertNotNull(sesiones.validar(token));
        Thread.sleep(80);

        assertNull(sesiones.validar(token));
    }

    @Test
    void rechazaConfiguracionInvalida() {
        assertThrows(IllegalArgumentException.class, () -> new SesionManager(null));
        assertThrows(IllegalArgumentException.class, () -> new SesionManager(Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new SesionManager(Duration.ofMinutes(-1)));
        assertThrows(IllegalArgumentException.class, () -> new SesionManager().crear(null));
    }
}
