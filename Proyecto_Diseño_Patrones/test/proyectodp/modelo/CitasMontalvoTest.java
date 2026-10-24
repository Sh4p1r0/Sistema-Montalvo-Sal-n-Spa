package proyectodp.modelo;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Modelo: CitasMontalvo")
class CitasMontalvoTest {

    @Test
    void completaValoresPorDefecto() {
        CitasMontalvo cita = new CitasMontalvo(null, "Ana", null, "10:00", "Corte", 20, null, 0, null);

        assertNotNull(cita.getId());
        assertFalse(cita.getId().isBlank());
        assertEquals(LocalDate.now().toString(), cita.getFecha());
        assertEquals("Especialista Montalvo", cita.getEstilista());
        assertEquals(45, cita.getDuracionMinutos());
        assertEquals("Pendiente", cita.getEstado().obtenerNombreEstado());
    }

    @Test
    void generaUnJsonConTodosLosCampos() {
        CitasMontalvo cita = new CitasMontalvo("id1", "Ana", "2026-11-10", "10:00", "Corte", 20.5, "Carlos", 45, null);
        String json = cita.toJson();

        assertTrue(json.contains("\"id\":\"id1\""));
        assertTrue(json.contains("\"nombreCliente\":\"Ana\""));
        assertTrue(json.contains("\"fecha\":\"2026-11-10\""));
        assertTrue(json.contains("\"precio\":20.50"));
        assertTrue(json.contains("\"estado\":\"Pendiente\""));
    }

    @Test
    void escapaComillasEnElJson() {
        CitasMontalvo cita = new CitasMontalvo("id1", "Ana \"La Jefa\"", "2026-11-10", "10:00", "Corte", 20, "Carlos", 45, null);

        assertTrue(cita.toJson().contains("Ana \\\"La Jefa\\\""));
    }
}
