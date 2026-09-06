package proyectodp.state;

import proyectodp.modelo.CitasMontalvo;

public class EstadoCancelada implements EstadoCita {

    @Override
    public void atender(CitasMontalvo cita) {
        System.out.println("No se puede atender una cita que ha sido cancelada.");
    }

    @Override
    public void cancelar(CitasMontalvo cita) {
        System.out.println("La cita ya se encontraba cancelada previamente.");
    }

    @Override
    public void reprogramar(CitasMontalvo cita, String nuevaFecha, String nuevaHora) {
        throw new IllegalStateException("Regla de negocio: No se puede reprogramar una cita que ha sido cancelada.");
    }

    @Override
    public String obtenerNombreEstado() {
        return "Cancelada";
    }
}
