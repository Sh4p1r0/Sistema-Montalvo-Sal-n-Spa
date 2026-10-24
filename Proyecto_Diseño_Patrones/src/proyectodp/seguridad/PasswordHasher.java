package proyectodp.seguridad;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Hash de contraseñas con PBKDF2-HMAC-SHA256 y sal aleatoria.
 *
 * Formato almacenado:
 *   pbkdf2$sha256$<iteraciones>$<salBase64>$<hashBase64>
 *
 * Compatibilidad: si el valor almacenado NO tiene el prefijo, se compara como
 * texto plano y se marca para migración (ver {@link #necesitaMigracion(String)}).
 */
public final class PasswordHasher {

    private static final String PREFIJO = "pbkdf2";
    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final int ITERACIONES = 120_000;
    private static final int LONGITUD_CLAVE_BITS = 256;
    private static final int LONGITUD_SAL_BYTES = 16;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder B64_ENCODE = Base64.getEncoder().withoutPadding();
    private static final Base64.Decoder B64_DECODE = Base64.getDecoder();

    private PasswordHasher() {
    }

    /** Genera un hash nuevo con sal aleatoria. */
    public static String hash(String password) {
        if (password == null) {
            throw new IllegalArgumentException("La contraseña no puede ser nula.");
        }
        byte[] sal = new byte[LONGITUD_SAL_BYTES];
        RANDOM.nextBytes(sal);
        byte[] derivada = derivar(password, sal, ITERACIONES, LONGITUD_CLAVE_BITS);
        return PREFIJO + "$sha256$" + ITERACIONES + "$"
                + B64_ENCODE.encodeToString(sal) + "$"
                + B64_ENCODE.encodeToString(derivada);
    }

    /**
     * Verifica una contraseña contra un valor almacenado (hash o texto plano legado).
     * Compara en tiempo constante para evitar ataques de temporización.
     */
    public static boolean verificar(String password, String almacenado) {
        if (password == null || almacenado == null || almacenado.isEmpty()) {
            return false;
        }
        if (necesitaMigracion(almacenado)) {
            // Legado: contraseña en texto plano
            return compararSeguro(password, almacenado);
        }
        try {
            String[] partes = almacenado.split("\\$");
            if (partes.length != 5) {
                return false;
            }
            int iteraciones = Integer.parseInt(partes[2]);
            byte[] sal = B64_DECODE.decode(partes[3]);
            byte[] esperado = B64_DECODE.decode(partes[4]);
            byte[] calculado = derivar(password, sal, iteraciones, esperado.length * 8);
            return MessageDigest.isEqual(esperado, calculado);
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** True si el valor guardado es texto plano y debería reemplazarse por un hash. */
    public static boolean necesitaMigracion(String almacenado) {
        return almacenado != null && !almacenado.startsWith(PREFIJO + "$");
    }

    private static byte[] derivar(String password, byte[] sal, int iteraciones, int longitudBits) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), sal, iteraciones, longitudBits);
        try {
            return SecretKeyFactory.getInstance(ALGORITMO).generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo calcular el hash de la contraseña.", e);
        } finally {
            spec.clearPassword();
        }
    }

    private static boolean compararSeguro(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }
}
