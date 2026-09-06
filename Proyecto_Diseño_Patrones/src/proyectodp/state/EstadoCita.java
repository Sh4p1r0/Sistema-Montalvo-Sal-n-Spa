package proyectodp.state;

import proyectodp.modelo.CitasMontalvo;

/**
 * Patrón de Diseño: STATE
 * Interfaz que define las operaciones que dependen del estado actual de una cita.
 */
public interface EstadoCita {

    void atender(CitasMontalvo cita);

    void cancelar(CitasMontalvo cita);

    void reprogramar(CitasMontalvo cita, String nuevaFecha, String nuevaHora);

    default void finalizar(CitasMontalvo cita) {
        throw new IllegalStateException("Solo una cita en estado 'Atendida' puede ser finalizada y archivada.");
    }

    String obtenerNombreEstado();
}
