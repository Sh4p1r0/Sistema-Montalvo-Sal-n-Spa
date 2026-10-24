package proyectodp.state;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import proyectodp.builder.CitaBuilder;
import proyectodp.modelo.CitasMontalvo;

@DisplayName("Patrón State: transiciones de estado de una cita")
class EstadoCitaTest {

    private CitasMontalvo nuevaCita() {
        return new CitaBuilder()
                .setCliente("Ana Torres")
                .setFecha("2026-11-10")
                .setHora("10:00")
                .setServicio("Corte de Cabello")
                .setPrecio(20.0)
                .setEstilista("Carlos Mendoza")
                .build();
    }

    @Nested
    @DisplayName("Estado Pendiente")
    class Pendiente {

        @Test
        void atenderCambiaAAtendida() {
            CitasMontalvo cita = nuevaCita();
            cita.atenderCita();
            assertEquals("Atendida", cita.getEstado().obtenerNombreEstado());
        }

        @Test
        void cancelarCambiaACancelada() {
            CitasMontalvo cita = nuevaCita();
            cita.cancelarCita();
            assertEquals("Cancelada", cita.getEstado().obtenerNombreEstado());
        }

        @Test
        void reprogramarCambiaAReprogramadaYActualizaDatos() {
            CitasMontalvo cita = nuevaCita();
            cita.reprogramarCita("2026-11-12", "15:30");
            assertEquals("Reprogramada", cita.getEstado().obtenerNombreEstado());
            assertEquals("2026-11-12", cita.getFecha());
            assertEquals("15:30", cita.getHora());
        }

        @Test
        void noSePuedeFinalizarSinAtender() {
            CitasMontalvo cita = nuevaCita();
            assertThrows(IllegalStateException.class, cita::finalizarCita);
            assertEquals("Pendiente", cita.getEstado().obtenerNombreEstado());
        }
    }

    @Nested
    @DisplayName("Estado Atendida")
    class Atendida {

        private CitasMontalvo atendida() {
            CitasMontalvo cita = nuevaCita();
            cita.atenderCita();
            return cita;
        }

        @Test
        void finalizarCambiaAFinalizada() {
            CitasMontalvo cita = atendida();
            cita.finalizarCita();
            assertEquals("Finalizada", cita.getEstado().obtenerNombreEstado());
        }

        @Test
        void noSePuedeReprogramarUnaCitaAtendida() {
            CitasMontalvo cita = atendida();
            assertThrows(IllegalStateException.class,
                    () -> cita.reprogramarCita("2026-12-01", "09:00"));
        }

        @Test
        void cancelarNoCambiaElEstado() {
            CitasMontalvo cita = atendida();
            cita.cancelarCita();
            assertEquals("Atendida", cita.getEstado().obtenerNombreEstado());
        }
    }

    @Nested
    @DisplayName("Estado Reprogramada")
    class Reprogramada {

        @Test
        void atenderCambiaAAtendida() {
            CitasMontalvo cita = nuevaCita();
            cita.reprogramarCita("2026-11-12", "15:30");
            cita.atenderCita();
            assertEquals("Atendida", cita.getEstado().obtenerNombreEstado());
        }

        @Test
        void sePuedeReprogramarVariasVeces() {
            CitasMontalvo cita = nuevaCita();
            cita.reprogramarCita("2026-11-12", "15:30");
            cita.reprogramarCita("2026-11-13", "16:00");
            assertEquals("Reprogramada", cita.getEstado().obtenerNombreEstado());
            assertEquals("2026-11-13", cita.getFecha());
            assertEquals("16:00", cita.getHora());
        }
    }

    @Nested
    @DisplayName("Estado Cancelada")
    class Cancelada {

        private CitasMontalvo cancelada() {
            CitasMontalvo cita = nuevaCita();
            cita.cancelarCita();
            return cita;
        }

        @Test
        void noSePuedeReprogramar() {
            assertThrows(IllegalStateException.class,
                    () -> cancelada().reprogramarCita("2026-12-01", "09:00"));
        }

        @Test
        void atenderNoCambiaElEstado() {
            CitasMontalvo cita = cancelada();
            cita.atenderCita();
            assertEquals("Cancelada", cita.getEstado().obtenerNombreEstado());
        }
    }

    @Nested
    @DisplayName("Estado Finalizada")
    class Finalizada {

        private CitasMontalvo finalizada() {
            CitasMontalvo cita = nuevaCita();
            cita.atenderCita();
            cita.finalizarCita();
            return cita;
        }

        @Test
        void noSePuedeCancelar() {
            assertThrows(IllegalStateException.class, () -> finalizada().cancelarCita());
        }

        @Test
        void noSePuedeReprogramar() {
            assertThrows(IllegalStateException.class,
                    () -> finalizada().reprogramarCita("2026-12-01", "09:00"));
        }

        @Test
        void esIdempotenteAlFinalizar() {
            CitasMontalvo cita = finalizada();
            cita.finalizarCita();
            assertEquals("Finalizada", cita.getEstado().obtenerNombreEstado());
        }
    }
}
