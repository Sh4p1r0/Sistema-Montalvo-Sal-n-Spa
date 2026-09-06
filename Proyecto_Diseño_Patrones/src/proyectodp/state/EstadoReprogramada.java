package proyectodp.state;

import proyectodp.modelo.CitasMontalvo;

public class EstadoReprogramada implements EstadoCita {

    @Override
    public void atender(CitasMontalvo cita) {
        System.out.println("La cita reprogramada de " + cita.getNombreCliente() + " está siendo atendida.");
        cita.setEstado(new EstadoAtendida());
    }

    @Override
    public void cancelar(CitasMontalvo cita) {
        System.out.println("La cita reprogramada de " + cita.getNombreCliente() + " ha sido cancelada.");
        cita.setEstado(new EstadoCancelada());
    }

    @Override
    public void reprogramar(CitasMontalvo cita, String nuevaFecha, String nuevaHora) {
        cita.setFecha(nuevaFecha);
        cita.setHora(nuevaHora);
        System.out.println("Cita reprogramada nuevamente para: " + nuevaFecha + " a las " + nuevaHora);
    }

    @Override
    public String obtenerNombreEstado() {
        return "Reprogramada";
    }
}
