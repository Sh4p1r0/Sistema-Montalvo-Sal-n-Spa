package proyectodp.facade;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import proyectodp.builder.CitaBuilder;
import proyectodp.dao.CitaDAO;
import proyectodp.dao.UsuarioDAO;
import proyectodp.factory.ServicioFactory;
import proyectodp.interfaces.IServicioSalon;
import proyectodp.modelo.CitasMontalvo;
import proyectodp.modelo.Usuario;

/**
 * Patrón de Diseño: FACADE
 * Fachada única que simplifica la interacción de toda la lógica de negocio del sistema:
 * creación de citas (Builder + Factory), gestión de estados (State), persistencia (CitaDAO/Supabase),
 * y cálculo de estadísticas consolidadas para el panel de administración.
 */
public class SistemaFacade {

    private final CitaDAO citaDAO;
    private final UsuarioDAO usuarioDAO;

    public SistemaFacade() {
        this.citaDAO = new CitaDAO();
        this.usuarioDAO = new UsuarioDAO();
    }

    // Sobrecarga anterior para compatibilidad
    public void registrarCita(String cliente, String hora, String tipoServicio) {
        registrarCita(cliente, LocalDate.now().toString(), hora, tipoServicio, "Carlos Mendoza");
    }

    // Registro completo con reglas de negocio y no solapamiento
    public CitasMontalvo registrarCita(String cliente, String fecha, String hora, String tipoServicio, String estilista) {
        if (cliente == null || cliente.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del cliente no puede estar vacío.");
        }
        if (fecha == null || fecha.trim().isEmpty()) {
            throw new IllegalArgumentException("La fecha es obligatoria.");
        }
        if (hora == null || hora.trim().isEmpty()) {
            throw new IllegalArgumentException("La hora es obligatoria.");
        }

        String estilistaFinal = (estilista != null && !estilista.trim().isEmpty()) ? estilista.trim() : "Carlos Mendoza";

        // Regla de Negocio: Validar no solapamiento de horario para el estilista
        validarDisponibilidadEstilista(fecha, hora, estilistaFinal, null);

        IServicioSalon servicio = ServicioFactory.crearServicio(tipoServicio);

        CitasMontalvo cita = new CitaBuilder()
                .setCliente(cliente.trim())
                .setFecha(fecha)
                .setHora(hora)
                .setEstilista(estilistaFinal)
                .setServicio(servicio.obtenerDescripcion())
                .setPrecio(servicio.obtenerPrecio())
                .setDuracionMinutos(45)
                .build();

        citaDAO.insertar(cita);
        return cita;
    }

    // Reprogramar cita aplicando Patrón State y validando no cruce
    public void reprogramarCita(String id, String nuevaFecha, String nuevaHora) {
        CitasMontalvo cita = citaDAO.buscarPorId(id);
        if (cita == null) {
            throw new IllegalArgumentException("No se encontró la cita con ID: " + id);
        }

        // Validar disponibilidad
        validarDisponibilidadEstilista(nuevaFecha, nuevaHora, cita.getEstilista(), id);

        // Delegar la transición al patrón State
        cita.reprogramarCita(nuevaFecha, nuevaHora);
        citaDAO.actualizarReprogramacion(id, nuevaFecha, nuevaHora);
    }

    // Atender cita aplicando Patrón State
    public void atenderCita(String id) {
        CitasMontalvo cita = citaDAO.buscarPorId(id);
        if (cita == null) {
            throw new IllegalArgumentException("No se encontró la cita con ID: " + id);
        }
        cita.atenderCita();
        citaDAO.actualizarEstado(id, "Atendida");
    }

    // Finalizar cita (Archivar del calendario activo) aplicando Patrón State
    public void finalizarCita(String id) {
        CitasMontalvo cita = citaDAO.buscarPorId(id);
        if (cita == null) {
            throw new IllegalArgumentException("No se encontró la cita con ID: " + id);
        }
        cita.finalizarCita();
        citaDAO.actualizarEstado(id, "Finalizada");
    }

    // Cancelar cita aplicando Patrón State
    public void cancelarCita(String id) {
        CitasMontalvo cita = citaDAO.buscarPorId(id);
        if (cita == null) {
            throw new IllegalArgumentException("No se encontró la cita con ID: " + id);
        }
        cita.cancelarCita();
        citaDAO.actualizarEstado(id, "Cancelada");
    }

    // Obtener todas las citas (para Administrador e Historial)
    public List<CitasMontalvo> obtenerCitas() {
        return citaDAO.listarTodas();
    }

    // Obtener citas activas (Pendientes, Reprogramadas, Atendidas) excluyendo 'Finalizada'
    public List<CitasMontalvo> obtenerCitasActivas(String estilista, String fecha) {
        return citaDAO.listarActivas(estilista, fecha);
    }

    public CitasMontalvo buscarPorId(String id) {
        return citaDAO.buscarPorId(id);
    }

    public List<Usuario> obtenerEstilistas() {
        return usuarioDAO.listarEstilistas();
    }

    public Usuario autenticar(String username, String password) {
        return usuarioDAO.autenticar(username, password);
    }

    // Obtener citas en un rango de fechas
    public List<CitasMontalvo> obtenerCitasRango(String fechaInicio, String fechaFin, String estilista, boolean incluirFinalizadas) {
        return citaDAO.listarPorRangoFechas(fechaInicio, fechaFin, estilista, incluirFinalizadas);
    }

    // Estadísticas completas y métricas financieras para Recepción y Administrador
    public String obtenerEstadisticasAdminJson() {
        List<CitasMontalvo> todas = citaDAO.listarTodas();
        LocalDate hoyDate = LocalDate.now();
        String hoy = hoyDate.toString();
        String ayer = hoyDate.minusDays(1).toString();
        String inicioSemana = hoyDate.minusDays(hoyDate.getDayOfWeek().getValue() - 1).toString();
        String finSemana = hoyDate.minusDays(hoyDate.getDayOfWeek().getValue() - 1).plusDays(6).toString();

        int total = todas.size();
        int citasHoy = 0;
        int citasAyer = 0;
        int citasSemana = 0;
        int pendientes = 0;
        int atendidas = 0;
        int finalizadas = 0;
        int canceladas = 0;

        double ingresosTotales = 0.0;
        double ingresosHoy = 0.0;
        double ingresosAyer = 0.0;
        double ingresosSemana = 0.0;

        Map<String, Integer> porServicio = new HashMap<>();
        Map<String, Integer> porEstilista = new HashMap<>();

        for (CitasMontalvo c : todas) {
            String estado = c.getEstado().obtenerNombreEstado();
            String f = c.getFecha();

            if (f.equals(hoy)) citasHoy++;
            if (f.equals(ayer)) citasAyer++;
            if (f.compareTo(inicioSemana) >= 0 && f.compareTo(finSemana) <= 0) citasSemana++;

            boolean pagada = "Atendida".equalsIgnoreCase(estado) || "Finalizada".equalsIgnoreCase(estado);

            if ("Pendiente".equalsIgnoreCase(estado)) pendientes++;
            else if ("Atendida".equalsIgnoreCase(estado)) atendidas++;
            else if ("Finalizada".equalsIgnoreCase(estado)) finalizadas++;
            else if ("Cancelada".equalsIgnoreCase(estado)) canceladas++;

            if (pagada) {
                ingresosTotales += c.getPrecio();
                if (f.equals(hoy)) ingresosHoy += c.getPrecio();
                if (f.equals(ayer)) ingresosAyer += c.getPrecio();
                if (f.compareTo(inicioSemana) >= 0 && f.compareTo(finSemana) <= 0) ingresosSemana += c.getPrecio();
            }

            porServicio.put(c.getTipoServicio(), porServicio.getOrDefault(c.getTipoServicio(), 0) + 1);
            porEstilista.put(c.getEstilista(), porEstilista.getOrDefault(c.getEstilista(), 0) + 1);
        }

        // Armar JSON manual sin dependencias externas
        StringBuilder sbServicios = new StringBuilder("{");
        int count = 0;
        for (Map.Entry<String, Integer> entry : porServicio.entrySet()) {
            if (count++ > 0) sbServicios.append(",");
            sbServicios.append("\"").append(entry.getKey()).append("\":").append(entry.getValue());
        }
        sbServicios.append("}");

        StringBuilder sbEstilistas = new StringBuilder("{");
        count = 0;
        for (Map.Entry<String, Integer> entry : porEstilista.entrySet()) {
            if (count++ > 0) sbEstilistas.append(",");
            sbEstilistas.append("\"").append(entry.getKey()).append("\":").append(entry.getValue());
        }
        sbEstilistas.append("}");

        return String.format(
            Locale.US,
            "{\"total\":%d,\"citasHoy\":%d,\"citasAyer\":%d,\"citasSemana\":%d,\"pendientes\":%d,\"atendidas\":%d,\"finalizadas\":%d,\"canceladas\":%d,\"ingresos\":%.2f,\"ingresosHoy\":%.2f,\"ingresosAyer\":%.2f,\"ingresosSemana\":%.2f,\"porServicio\":%s,\"porEstilista\":%s}",
            total, citasHoy, citasAyer, citasSemana, pendientes, atendidas, finalizadas, canceladas, ingresosTotales, ingresosHoy, ingresosAyer, ingresosSemana, sbServicios.toString(), sbEstilistas.toString()
        );
    }

    // Detalle de Caja para Recepción (Servicios cobrados en el período)
    public String obtenerDetalleCajaJson(String periodo, String fechaDirecta) {
        LocalDate hoyDate = LocalDate.now();
        String inicio = hoyDate.toString();
        String fin = hoyDate.toString();
        String labelPeriodo = "Hoy";

        if ("ayer".equalsIgnoreCase(periodo)) {
            inicio = hoyDate.minusDays(1).toString();
            fin = inicio;
            labelPeriodo = "Ayer (" + inicio + ")";
        } else if ("semana".equalsIgnoreCase(periodo)) {
            inicio = hoyDate.minusDays(hoyDate.getDayOfWeek().getValue() - 1).toString();
            fin = hoyDate.minusDays(hoyDate.getDayOfWeek().getValue() - 1).plusDays(6).toString();
            labelPeriodo = "Esta Semana (" + inicio + " al " + fin + ")";
        } else if ("todas".equalsIgnoreCase(periodo)) {
            inicio = "1900-01-01";
            fin = "2099-12-31";
            labelPeriodo = "Histórico Total";
        } else if (fechaDirecta != null && !fechaDirecta.trim().isEmpty()) {
            inicio = fechaDirecta.trim();
            fin = inicio;
            labelPeriodo = "Fecha " + inicio;
        }

        List<CitasMontalvo> lista = citaDAO.listarPorRangoFechas(inicio, fin, null, true);
        List<CitasMontalvo> cobradas = new ArrayList<>();
        double totalRecaudado = 0.0;
        Map<String, Double> porEstilista = new HashMap<>();

        for (CitasMontalvo c : lista) {
            String est = c.getEstado().obtenerNombreEstado();
            if ("Atendida".equalsIgnoreCase(est) || "Finalizada".equalsIgnoreCase(est)) {
                cobradas.add(c);
                totalRecaudado += c.getPrecio();
                porEstilista.put(c.getEstilista(), porEstilista.getOrDefault(c.getEstilista(), 0.0) + c.getPrecio());
            }
        }

        StringBuilder sbCitas = new StringBuilder("[");
        for (int i = 0; i < cobradas.size(); i++) {
            sbCitas.append(cobradas.get(i).toJson());
            if (i < cobradas.size() - 1) sbCitas.append(",");
        }
        sbCitas.append("]");

        StringBuilder sbEstilistas = new StringBuilder("{");
        int count = 0;
        for (Map.Entry<String, Double> e : porEstilista.entrySet()) {
            if (count++ > 0) sbEstilistas.append(",");
            sbEstilistas.append(String.format(Locale.US, "\"%s\":%.2f", e.getKey(), e.getValue()));
        }
        sbEstilistas.append("}");

        return String.format(
            Locale.US,
            "{\"periodo\":\"%s\",\"fechaInicio\":\"%s\",\"fechaFin\":\"%s\",\"totalRecaudado\":%.2f,\"cantidadServicios\":%d,\"citas\":%s,\"facturacionEstilistas\":%s}",
            labelPeriodo, inicio, fin, totalRecaudado, cobradas.size(), sbCitas.toString(), sbEstilistas.toString()
        );
    }

    // Regla de Negocio: Validar cruce de horario por estilista
    private void validarDisponibilidadEstilista(String fecha, String hora, String estilista, String idCitaExcluir) {
        if (estilista == null) return;

        for (CitasMontalvo c : citaDAO.listarTodas()) {
            if (idCitaExcluir != null && c.getId().equalsIgnoreCase(idCitaExcluir)) {
                continue;
            }

            String estado = c.getEstado().obtenerNombreEstado();
            if ("Cancelada".equalsIgnoreCase(estado) || "Finalizada".equalsIgnoreCase(estado)) {
                continue;
            }

            if (c.getFecha().equals(fecha) &&
                c.getHora().equals(hora) &&
                c.getEstilista().equalsIgnoreCase(estilista)) {
                throw new IllegalStateException("Conflicto de agenda: El estilista '" + estilista + "' ya tiene una cita reservada el " + fecha + " a las " + hora + ".");
            }
        }
    }
}
