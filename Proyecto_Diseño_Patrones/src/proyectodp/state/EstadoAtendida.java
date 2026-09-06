package proyectodp.state;

import proyectodp.modelo.CitasMontalvo;

/**
 * Patrón de Diseño: STATE
 * Representa el estado "Atendida".
 * Desde aquí, el estilista o administrador puede marcarla como "Finalizada"
 * para archivarla y retirarla de la vista activa.
 */
public class EstadoAtendida implements EstadoCita {

    @Override
    public void atender(CitasMontalvo cita) {
        System.out.println("La cita ya fue marcada como atendida previamente.");
    }

    @Override
    public void cancelar(CitasMontalvo cita) {
        System.out.println("No se puede cancelar. El servicio ya se realizó.");
    }

    @Override
    public void reprogramar(CitasMontalvo cita, String nuevaFecha, String nuevaHora) {
        throw new IllegalStateException("Regla de negocio: No se puede reprogramar una cita que ya fue atendida.");
    }

    @Override
    public void finalizar(CitasMontalvo cita) {
        System.out.println("La cita de " + cita.getNombreCliente() + " ha sido finalizada y archivada.");
        cita.setEstado(new EstadoFinalizada());
    }

    @Override
    public String obtenerNombreEstado() {
        return "Atendida";
    }
}
