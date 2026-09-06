package proyectodp.state;

import proyectodp.modelo.CitasMontalvo;

public class EstadoPendiente implements EstadoCita {

    @Override
    public void atender(CitasMontalvo cita) {
        System.out.println("La cita de " + cita.getNombreCliente() + " está siendo atendida.");
        cita.setEstado(new EstadoAtendida());
    }

    @Override
    public void cancelar(CitasMontalvo cita) {
        System.out.println("La cita de " + cita.getNombreCliente() + " ha sido cancelada.");
        cita.setEstado(new EstadoCancelada());
    }

    @Override
    public void reprogramar(CitasMontalvo cita, String nuevaFecha, String nuevaHora) {
        cita.setFecha(nuevaFecha);
        cita.setHora(nuevaHora);
        cita.setEstado(new EstadoReprogramada());
        System.out.println("La cita de " + cita.getNombreCliente() + " ha sido reprogramada para " + nuevaFecha + " a las " + nuevaHora);
    }

    @Override
    public String obtenerNombreEstado() {
        return "Pendiente";
    }
}
