package proyectodp.seguridad;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Seguridad: hash de contraseñas PBKDF2")
class PasswordHasherTest {

    @Test
    void generaUnHashConFormatoEsperado() {
        String hash = PasswordHasher.hash("montalvo");

        assertTrue(hash.startsWith("pbkdf2$sha256$120000$"), hash);
        assertEquals(5, hash.split("\\$").length);
        assertFalse(PasswordHasher.necesitaMigracion(hash));
    }

    @Test
    void dosHashesDeLaMismaClaveSonDistintosPorLaSal() {
        String a = PasswordHasher.hash("montalvo");
        String b = PasswordHasher.hash("montalvo");

        assertNotEquals(a, b);
        assertTrue(PasswordHasher.verificar("montalvo", a));
        assertTrue(PasswordHasher.verificar("montalvo", b));
    }

    @Test
    void verificaCorrectamenteClaveValidaEInvalida() {
        String hash = PasswordHasher.hash("Secreta123");

        assertTrue(PasswordHasher.verificar("Secreta123", hash));
        assertFalse(PasswordHasher.verificar("secreta123", hash));
        assertFalse(PasswordHasher.verificar("otra", hash));
    }

    @Test
    void aceptaTextoPlanoLegadoYLoMarcaParaMigracion() {
        assertTrue(PasswordHasher.necesitaMigracion("montalvo"));
        assertTrue(PasswordHasher.verificar("montalvo", "montalvo"));
        assertFalse(PasswordHasher.verificar("incorrecta", "montalvo"));
    }

    @Test
    void rechazaValoresNulosVaciosOMalformados() {
        assertFalse(PasswordHasher.verificar(null, "montalvo"));
        assertFalse(PasswordHasher.verificar("montalvo", null));
        assertFalse(PasswordHasher.verificar("montalvo", ""));
        assertFalse(PasswordHasher.verificar("montalvo", "pbkdf2$sha256$abc$sal$hash"));
        assertFalse(PasswordHasher.verificar("montalvo", "pbkdf2$solo-dos-partes"));
        assertThrows(IllegalArgumentException.class, () -> PasswordHasher.hash(null));
    }
}
