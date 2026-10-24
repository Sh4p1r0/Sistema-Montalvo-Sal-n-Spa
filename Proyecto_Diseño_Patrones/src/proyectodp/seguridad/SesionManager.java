package proyectodp.seguridad;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import proyectodp.modelo.Usuario;

/**
 * Patrón de Diseño: SINGLETON (una instancia por servidor)
 * Gestiona tokens de sesión en memoria con expiración deslizante.
 *
 * El token es opaco (32 bytes aleatorios en Base64 URL-safe) y nunca contiene
 * datos del usuario: solo sirve como clave del mapa sesiones.
 */
public class SesionManager {

    public static final Duration DURACION_POR_DEFECTO = Duration.ofHours(8);

    private final Map<String, Sesion> sesiones = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final Duration duracion;

    public SesionManager() {
        this(DURACION_POR_DEFECTO);
    }

    public SesionManager(Duration duracion) {
        if (duracion == null || duracion.isZero() || duracion.isNegative()) {
            throw new IllegalArgumentException("La duración de sesión debe ser positiva.");
        }
        this.duracion = duracion;
    }

    /** Crea un token nuevo para el usuario autenticado. */
    public String crear(Usuario usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException("No se puede crear una sesión sin usuario.");
        }
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sesiones.put(token, new Sesion(usuario, Instant.now().plus(duracion)));
        return token;
    }

    /**
     * Devuelve el usuario si el token existe y no expiró.
     * Renueva la expiración en cada uso (sliding expiration).
     */
    public Usuario validar(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        Sesion sesion = sesiones.get(token);
        if (sesion == null) {
            return null;
        }
        if (sesion.expira.isBefore(Instant.now())) {
            sesiones.remove(token);
            return null;
        }
        sesiones.put(token, new Sesion(sesion.usuario, Instant.now().plus(duracion)));
        return sesion.usuario;
    }

    /** Invalida el token (logout). */
    public void cerrar(String token) {
        if (token != null) {
            sesiones.remove(token);
        }
    }

    /** Número de sesiones activas, descartando las expiradas. */
    public int activas() {
        limpiarExpiradas();
        return sesiones.size();
    }

    private void limpiarExpiradas() {
        Instant ahora = Instant.now();
        sesiones.entrySet().removeIf(e -> e.getValue().expira.isBefore(ahora));
    }

    private record Sesion(Usuario usuario, Instant expira) {
    }
}
