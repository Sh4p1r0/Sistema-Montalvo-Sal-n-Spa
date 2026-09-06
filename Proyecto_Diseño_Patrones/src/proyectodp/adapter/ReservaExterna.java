package proyectodp.adapter;

public class ReservaExterna {

    private String nombre;
    private String horario;
    private String servicio;

    public ReservaExterna(String nombre, String horario, String servicio) {
        this.nombre = nombre;
        this.horario = horario;
        this.servicio = servicio;
    }

    public String getNombre() {
        return nombre;
    }

    public String getHorario() {
        return horario;
    }

    public String getServicio() {
        return servicio;
    }
}
