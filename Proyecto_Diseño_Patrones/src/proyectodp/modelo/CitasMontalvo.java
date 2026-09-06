package proyectodp.modelo;

import java.util.UUID;
import proyectodp.state.EstadoCita;
import proyectodp.state.EstadoPendiente;

public class CitasMontalvo {

    private String id;
    private String nombreCliente;
    private String fecha; // Formato YYYY-MM-DD
    private String hora;  // Formato HH:mm
    private String tipoServicio;
    private double precio;
    private String estilista;
    private int duracionMinutos;
    private EstadoCita estado;

    // Constructor completo
    public CitasMontalvo(String id, String nombreCliente, String fecha, String hora, String tipoServicio, double precio, String estilista, int duracionMinutos, EstadoCita estado) {
        this.id = (id != null && !id.isEmpty()) ? id : UUID.randomUUID().toString().substring(0, 8);
        this.nombreCliente = nombreCliente;
        this.fecha = (fecha != null && !fecha.isEmpty()) ? fecha : java.time.LocalDate.now().toString();
        this.hora = hora;
        this.tipoServicio = tipoServicio;
        this.precio = precio;
        this.estilista = (estilista != null && !estilista.isEmpty()) ? estilista : "Especialista Montalvo";
        this.duracionMinutos = duracionMinutos > 0 ? duracionMinutos : 45;
        this.estado = (estado != null) ? estado : new EstadoPendiente();
    }

    // Constructor previo para compatibilidad con código existente
    public CitasMontalvo(String nombreCliente, String hora, String tipoServicio, double precio, EstadoCita estado) {
        this(UUID.randomUUID().toString().substring(0, 8), nombreCliente, java.time.LocalDate.now().toString(), hora, tipoServicio, precio, "Especialista Montalvo", 45, estado);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getHora() {
        return hora;
    }

    public void setHora(String hora) {
        this.hora = hora;
    }

    public String getTipoServicio() {
        return tipoServicio;
    }

    public void setTipoServicio(String tipoServicio) {
        this.tipoServicio = tipoServicio;
    }

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }

    public String getEstilista() {
        return estilista;
    }

    public void setEstilista(String estilista) {
        this.estilista = estilista;
    }

    public int getDuracionMinutos() {
        return duracionMinutos;
    }

    public void setDuracionMinutos(int duracionMinutos) {
        this.duracionMinutos = duracionMinutos;
    }

    public EstadoCita getEstado() {
        return estado;
    }

    public void setEstado(EstadoCita estado) {
        this.estado = estado;
    }

    public void atenderCita() {
        estado.atender(this);
    }

    public void cancelarCita() {
        estado.cancelar(this);
    }

    public void reprogramarCita(String nuevaFecha, String nuevaHora) {
        estado.reprogramar(this, nuevaFecha, nuevaHora);
    }

    public void finalizarCita() {
        estado.finalizar(this);
    }

    // Método para convertir a JSON limpio para el API REST
    public String toJson() {
        return String.format(
            java.util.Locale.US,
            "{\"id\":\"%s\",\"nombreCliente\":\"%s\",\"fecha\":\"%s\",\"hora\":\"%s\",\"tipoServicio\":\"%s\",\"precio\":%.2f,\"estilista\":\"%s\",\"duracionMinutos\":%d,\"estado\":\"%s\"}",
            escapeJson(id),
            escapeJson(nombreCliente),
            escapeJson(fecha),
            escapeJson(hora),
            escapeJson(tipoServicio),
            precio,
            escapeJson(estilista),
            duracionMinutos,
            escapeJson(estado.obtenerNombreEstado())
        );
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"");
    }

    @Override
    public String toString() {
        return "Cita [ID: " + id + " | Cliente: " + nombreCliente + " | Fecha: " + fecha + " | Hora: " + hora + " | Estilista: " + estilista + " | Servicio: " + tipoServicio + " | Total: S/. " + precio + " | Estado: " + estado.obtenerNombreEstado() + "]";
    }
}
