package proyectodp.singleton;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import proyectodp.builder.CitaBuilder;
import proyectodp.modelo.CitasMontalvo;

@DisplayName("Patrón Singleton: GestionMemoria")
class GestionMemoriaTest {

    private CitasMontalvo cita(String cliente) {
        return new CitaBuilder()
                .setId(UUID.randomUUID().toString().substring(0, 8))
                .setCliente(cliente)
                .setHora("10:00")
                .build();
    }

    @Test
    void siempreDevuelveLaMismaInstancia() {
        assertSame(GestionMemoria.getInstancia(), GestionMemoria.getInstancia());
    }

    @Test
    void agregaYBuscaPorIdSinImportarMayusculas() {
        GestionMemoria memoria = GestionMemoria.getInstancia();
        CitasMontalvo c = cita("Ana");
        memoria.agregarCita(c);

        assertSame(c, memoria.buscarPorId(c.getId().toUpperCase()));
    }

    @Test
    void devuelveCopiaDeLaLista() {
        GestionMemoria memoria = GestionMemoria.getInstancia();
        memoria.agregarCita(cita("Luis"));

        List<CitasMontalvo> copia = memoria.listarCitas();
        int tamano = copia.size();
        copia.clear();

        assertEquals(tamano, memoria.listarCitas().size(), "listarCitas debe devolver una copia defensiva");
    }

    @Test
    void eliminaPorId() {
        GestionMemoria memoria = GestionMemoria.getInstancia();
        CitasMontalvo c = cita("Pedro");
        memoria.agregarCita(c);

        assertTrue(memoria.eliminarCita(c.getId()));
        assertNull(memoria.buscarPorId(c.getId()));
    }

    @Test
    void eliminarIdInexistenteDevuelveFalse() {
        assertFalse(GestionMemoria.getInstancia().eliminarCita("no-existe"));
    }
}
