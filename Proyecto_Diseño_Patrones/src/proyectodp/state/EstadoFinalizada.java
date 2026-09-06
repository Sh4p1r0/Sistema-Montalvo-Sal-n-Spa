package proyectodp.state;

import proyectodp.modelo.CitasMontalvo;

/**
 * Patrón de Diseño: STATE
 * Representa el estado "Finalizada".
 * Una cita que ha sido atendida y posteriormente finalizada queda archivada,
 * por lo que desaparece de las agendas activas de los estilistas y del calendario activo.
 */
public class EstadoFinalizada implements EstadoCita {

    @Override
    public void atender(CitasMontalvo cita) {
        System.out.println("La cita de " + cita.getNombreCliente() + " ya se encuentra finalizada.");
    }

    @Override
    public void cancelar(CitasMontalvo cita) {
        throw new IllegalStateException("Regla de negocio: No se puede cancelar una cita que ya fue finalizada y cobrada.");
    }

    @Override
    public void reprogramar(CitasMontalvo cita, String nuevaFecha, String nuevaHora) {
        throw new IllegalStateException("Regla de negocio: No se puede reprogramar una cita ya finalizada.");
    }

    @Override
    public void finalizar(CitasMontalvo cita) {
        System.out.println("La cita ya se encuentra en estado Finalizada.");
    }

    @Override
    public String obtenerNombreEstado() {
        return "Finalizada";
    }
}
