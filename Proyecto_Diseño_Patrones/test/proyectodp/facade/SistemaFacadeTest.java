package proyectodp.facade;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import proyectodp.builder.CitaBuilder;
import proyectodp.dao.CitaDAO;
import proyectodp.dao.UsuarioDAO;
import proyectodp.modelo.CitasMontalvo;

@DisplayName("Facade: reglas de negocio de la agenda")
class SistemaFacadeTest {

    private FakeCitaDAO dao;
    private SistemaFacade facade;

    @BeforeEach
    void setUp() {
        dao = new FakeCitaDAO();
        facade = new SistemaFacade(dao, new UsuarioDAO());
    }

    // ---------- Registro ----------

    @Test
    void registraUnaCitaConPrecioDelCatalogoYEstadoPendiente() {
        CitasMontalvo cita = facade.registrarCita("Ana Torres", "2026-11-10", "10:00", "CORTE", "Carlos Mendoza");

        assertEquals("Ana Torres", cita.getNombreCliente());
        assertEquals(20.00, cita.getPrecio(), 0.001);
        assertEquals("Pendiente", cita.getEstado().obtenerNombreEstado());
        assertEquals(1, dao.citas.size());
    }

    @Test
    void rechazaClienteFechaOHoraVacios() {
        assertThrows(IllegalArgumentException.class,
                () -> facade.registrarCita("  ", "2026-11-10", "10:00", "CORTE", "Carlos"));
        assertThrows(IllegalArgumentException.class,
                () -> facade.registrarCita("Ana", " ", "10:00", "CORTE", "Carlos"));
        assertThrows(IllegalArgumentException.class,
                () -> facade.registrarCita("Ana", "2026-11-10", null, "CORTE", "Carlos"));
    }

    @Test
    void usaEstilistaPorDefectoSiVieneVacio() {
        CitasMontalvo cita = facade.registrarCita("Ana", "2026-11-10", "10:00", "CORTE", null);
        assertEquals("Carlos Mendoza", cita.getEstilista());
    }

    @Test
    void rechazaServicioInexistente() {
        assertThrows(IllegalArgumentException.class,
                () -> facade.registrarCita("Ana", "2026-11-10", "10:00", "TATUAJE", "Carlos"));
        assertTrue(dao.citas.isEmpty(), "No debe persistirse una cita inválida");
    }

    // ---------- Solapamiento ----------

    @Test
    void detectaConflictoDeHorarioDelMismoEstilista() {
        facade.registrarCita("Ana", "2026-11-10", "10:00", "CORTE", "Carlos Mendoza");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> facade.registrarCita("Luis", "2026-11-10", "10:00", "MASAJE", "Carlos Mendoza"));

        assertTrue(ex.getMessage().contains("Conflicto"));
        assertEquals(1, dao.citas.size());
    }

    @Test
    void permiteElMismoHorarioConDistintoEstilista() {
        facade.registrarCita("Ana", "2026-11-10", "10:00", "CORTE", "Carlos Mendoza");
        facade.registrarCita("Luis", "2026-11-10", "10:00", "MASAJE", "Sofia Silva");

        assertEquals(2, dao.citas.size());
    }

    @Test
    void permiteElMismoHorarioEnOtraFecha() {
        facade.registrarCita("Ana", "2026-11-10", "10:00", "CORTE", "Carlos Mendoza");
        facade.registrarCita("Luis", "2026-11-11", "10:00", "CORTE", "Carlos Mendoza");

        assertEquals(2, dao.citas.size());
    }

    @Test
    void unaCitaCanceladaLiberaElHorario() {
        CitasMontalvo cita = facade.registrarCita("Ana", "2026-11-10", "10:00", "CORTE", "Carlos Mendoza");
        facade.cancelarCita(cita.getId());

        assertDoesNotThrow(() -> facade.registrarCita("Luis", "2026-11-10", "10:00", "CORTE", "Carlos Mendoza"));
    }

    @Test
    void unaCitaFinalizadaNoBloqueaElHorario() {
        CitasMontalvo cita = facade.registrarCita("Ana", "2026-11-10", "10:00", "CORTE", "Carlos Mendoza");
        facade.atenderCita(cita.getId());
        facade.finalizarCita(cita.getId());

        assertDoesNotThrow(() -> facade.registrarCita("Luis", "2026-11-10", "10:00", "CORTE", "Carlos Mendoza"));
    }

    // ---------- Transiciones ----------

    @Test
    void atenderYFinalizarPersistenElEstado() {
        CitasMontalvo cita = facade.registrarCita("Ana", "2026-11-10", "10:00", "CORTE", "Carlos Mendoza");

        facade.atenderCita(cita.getId());
        assertEquals("Atendida", cita.getEstado().obtenerNombreEstado());

        facade.finalizarCita(cita.getId());
        assertEquals("Finalizada", cita.getEstado().obtenerNombreEstado());
    }

    @Test
    void noSePuedeAtenderUnaCitaCancelada() {
        CitasMontalvo cita = facade.registrarCita("Ana", "2026-11-10", "10:00", "CORTE", "Carlos Mendoza");
        facade.cancelarCita(cita.getId());

        facade.atenderCita(cita.getId());
        assertEquals("Cancelada", cita.getEstado().obtenerNombreEstado());
    }

    @Test
    void fallaSiLaCitaNoExiste() {
        assertThrows(IllegalArgumentException.class, () -> facade.atenderCita("no-existe"));
        assertThrows(IllegalArgumentException.class, () -> facade.cancelarCita("no-existe"));
        assertThrows(IllegalArgumentException.class, () -> facade.finalizarCita("no-existe"));
        assertThrows(IllegalArgumentException.class, () -> facade.reprogramarCita("no-existe", "2026-11-12", "09:00"));
    }

    @Test
    void reprogramarValidaConflictoYActualizaLaCita() {
        CitasMontalvo cita = facade.registrarCita("Ana", "2026-11-10", "10:00", "CORTE", "Carlos Mendoza");
        facade.registrarCita("Luis", "2026-11-10", "11:00", "CORTE", "Carlos Mendoza");

        assertThrows(IllegalStateException.class,
                () -> facade.reprogramarCita(cita.getId(), "2026-11-10", "11:00"));

        facade.reprogramarCita(cita.getId(), "2026-11-10", "12:00");
        assertEquals("12:00", cita.getHora());
        assertEquals("Reprogramada", cita.getEstado().obtenerNombreEstado());
    }

    // ---------- Caja ----------

    @Test
    void laCajaSoloSumaCitasAtendidasOFinalizadas() {
        CitasMontalvo a = facade.registrarCita("Ana", "2026-11-10", "10:00", "CORTE", "Carlos");
        CitasMontalvo b = facade.registrarCita("Luis", "2026-11-10", "11:00", "MASAJE", "Sofia");
        facade.registrarCita("Eva", "2026-11-10", "12:00", "CORTE", "Carlos");

        facade.atenderCita(a.getId());
        facade.atenderCita(b.getId());
        facade.finalizarCita(b.getId());

        String json = facade.obtenerDetalleCajaJson("2026-11-10", "2026-11-10");

        assertTrue(json.contains("\"cantidadServicios\":2"), json);
        assertTrue(json.contains("\"fechaInicio\":\"2026-11-10\""), json);
        assertTrue(json.contains("Ana"), json);
    }

    // ---------- Doble de prueba ----------

    /**
     * DAO en memoria: evita cualquier conexión real a PostgreSQL/Supabase.
     */
    static class FakeCitaDAO extends CitaDAO {

        final List<CitasMontalvo> citas = new ArrayList<>();

        @Override
        public void insertar(CitasMontalvo cita) {
            citas.add(cita);
        }

        @Override
        public void actualizarEstado(String id, String nuevoEstado) {
            CitasMontalvo c = buscarPorId(id);
            if (c != null) c.setEstado(CitaDAO.parseEstado(nuevoEstado));
        }

        @Override
        public void actualizarReprogramacion(String id, String nuevaFecha, String nuevaHora) {
            CitasMontalvo c = buscarPorId(id);
            if (c != null) {
                c.setFecha(nuevaFecha);
                c.setHora(nuevaHora);
            }
        }

        @Override
        public List<CitasMontalvo> listarTodas() {
            return new ArrayList<>(citas);
        }

        @Override
        public List<CitasMontalvo> listarActivas(String estilista, String fecha) {
            return citas.stream()
                    .filter(c -> fecha == null || c.getFecha().equals(fecha))
                    .filter(c -> estilista == null || c.getEstilista().equalsIgnoreCase(estilista))
                    .filter(c -> !"Finalizada".equals(c.getEstado().obtenerNombreEstado()))
                    .toList();
        }

        @Override
        public List<CitasMontalvo> listarPorRangoFechas(String inicio, String fin, String estilista, boolean incluirFinalizadas) {
            return citas.stream()
                    .filter(c -> c.getFecha().compareTo(inicio) >= 0 && c.getFecha().compareTo(fin) <= 0)
                    .filter(c -> estilista == null || c.getEstilista().equalsIgnoreCase(estilista))
                    .filter(c -> incluirFinalizadas || !"Finalizada".equals(c.getEstado().obtenerNombreEstado()))
                    .toList();
        }

        @Override
        public CitasMontalvo buscarPorId(String id) {
            return citas.stream()
                    .filter(c -> c.getId().equalsIgnoreCase(id))
                    .findFirst()
                    .orElse(null);
        }
    }
}
