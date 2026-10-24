package proyectodp.adapter;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import proyectodp.modelo.CitasMontalvo;

@DisplayName("Patrón Adapter: reserva externa a cita interna")
class ReservaAdapterTest {

    @Test
    void convierteUnaReservaExterna() {
        ReservaExterna externa = new ReservaExterna("Ana Torres", "10:00", "CORTE");
        CitasMontalvo cita = new ReservaAdapter().convertir(externa);

        assertEquals("Ana Torres", cita.getNombreCliente());
        assertEquals("10:00", cita.getHora());
        assertNotNull(cita.getTipoServicio());
        assertTrue(cita.getPrecio() > 0);
        assertEquals("Pendiente", cita.getEstado().obtenerNombreEstado());
        assertEquals(LocalDate.now().toString(), cita.getFecha());
    }

    @Test
    void propagaServicioInvalido() {
        ReservaExterna externa = new ReservaExterna("Ana", "10:00", "NO-EXISTE");
        assertThrows(IllegalArgumentException.class, () -> new ReservaAdapter().convertir(externa));
    }
}
