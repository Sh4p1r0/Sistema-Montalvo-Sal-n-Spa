package proyectodp.builder;

import java.util.UUID;
import proyectodp.modelo.CitasMontalvo;
import proyectodp.state.EstadoCita;
import proyectodp.state.EstadoPendiente;

public class CitaBuilder {

    private String id;
    private String cliente;
    private String fecha;
    private String hora;
    private String servicio;
    private double precio;
    private String estilista;
    private int duracionMinutos = 45;
    private EstadoCita estado;

    public CitaBuilder setId(String id) {
        this.id = id;
        return this;
    }

    public CitaBuilder setCliente(String cliente) {
        this.cliente = cliente;
        return this;
    }

    public CitaBuilder setFecha(String fecha) {
        this.fecha = fecha;
        return this;
    }

    public CitaBuilder setHora(String hora) {
        this.hora = hora;
        return this;
    }

    public CitaBuilder setServicio(String servicio) {
        this.servicio = servicio;
        return this;
    }

    public CitaBuilder setPrecio(double precio) {
        this.precio = precio;
        return this;
    }

    public CitaBuilder setEstilista(String estilista) {
        this.estilista = estilista;
        return this;
    }

    public CitaBuilder setDuracionMinutos(int duracionMinutos) {
        this.duracionMinutos = duracionMinutos;
        return this;
    }

    public CitaBuilder setEstado(EstadoCita estado) {
        this.estado = estado;
        return this;
    }

    public CitasMontalvo build() {
        // Se usan locales para no mutar el builder: así puede reutilizarse
        // y cada build() genera su propio ID.
        String idFinal = (this.id != null && !this.id.isEmpty())
                ? this.id
                : UUID.randomUUID().toString().substring(0, 8);
        EstadoCita estadoFinal = (this.estado != null) ? this.estado : new EstadoPendiente();
        String fechaFinal = (this.fecha != null && !this.fecha.isEmpty())
                ? this.fecha
                : java.time.LocalDate.now().toString();
        String estilistaFinal = (this.estilista != null && !this.estilista.isEmpty())
                ? this.estilista
                : "Especialista Montalvo";
        return new CitasMontalvo(idFinal, cliente, fechaFinal, hora, servicio, precio, estilistaFinal, duracionMinutos, estadoFinal);
    }
}
