package proyectodp.seguridad;

/**
 * Matriz de permisos por rol (RBAC).
 *
 * El servidor es la única fuente de verdad: el frontend puede ocultar botones,
 * pero cada endpoint valida el permiso antes de ejecutar la operación.
 */
public final class Permisos {

    public enum Accion {
        VER_CITAS,
        CREAR_CITA,
        EDITAR_CITA,
        VER_ESTILISTAS,
        VER_STATS,
        VER_CAJA
    }

    private Permisos() {
    }

    public static boolean puede(String rol, Accion accion) {
        if (rol == null || accion == null) {
            return false;
        }
        String r = rol.trim().toUpperCase();
        boolean admin = "ADMIN".equals(r);
        boolean recepcion = "RECEPCIONISTA".equals(r);
        boolean estilista = "ESTILISTA".equals(r);

        return switch (accion) {
            case VER_CITAS -> admin || recepcion || estilista;
            case VER_ESTILISTAS -> admin || recepcion || estilista;
            case CREAR_CITA, EDITAR_CITA -> admin || recepcion;
            case VER_STATS, VER_CAJA -> admin || recepcion;
        };
    }
}
