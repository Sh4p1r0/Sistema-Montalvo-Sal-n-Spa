package proyectodp.seguridad;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import proyectodp.seguridad.Permisos.Accion;

@DisplayName("RBAC: matriz de permisos por rol")
class PermisosTest {

    @Test
    void elAdminPuedeTodo() {
        for (Accion accion : Accion.values()) {
            assertTrue(Permisos.puede("ADMIN", accion), "ADMIN debería poder " + accion);
        }
    }

    @Test
    void elRecepcionistaPuedeTodoElFlujoDeRecepcion() {
        for (Accion accion : Accion.values()) {
            assertTrue(Permisos.puede("RECEPCIONISTA", accion), "RECEPCIONISTA debería poder " + accion);
        }
    }

    @Test
    void elEstilistaSoloVeSuAgenda() {
        assertTrue(Permisos.puede("ESTILISTA", Accion.VER_CITAS));
        assertTrue(Permisos.puede("ESTILISTA", Accion.VER_ESTILISTAS));
    }

    @Test
    void elEstilistaNoCreaNiEditaNiVeDinero() {
        assertFalse(Permisos.puede("ESTILISTA", Accion.CREAR_CITA));
        assertFalse(Permisos.puede("ESTILISTA", Accion.EDITAR_CITA));
        assertFalse(Permisos.puede("ESTILISTA", Accion.VER_STATS));
        assertFalse(Permisos.puede("ESTILISTA", Accion.VER_CAJA));
    }

    @Test
    void rolesNulosODesconocidosNoTienenPermisos() {
        for (Accion accion : Accion.values()) {
            assertFalse(Permisos.puede(null, accion));
            assertFalse(Permisos.puede("", accion));
            assertFalse(Permisos.puede("INVITADO", accion));
        }
    }

    @Test
    void ignoraMayusculasYEspacios() {
        assertTrue(Permisos.puede("  admin ", Accion.VER_STATS));
        assertTrue(Permisos.puede("recepcionista", Accion.CREAR_CITA));
    }
}
