package proyectodp.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import proyectodp.config.ConexionDB;
import proyectodp.modelo.CitasMontalvo;
import proyectodp.singleton.GestionMemoria;
import proyectodp.state.EstadoAtendida;
import proyectodp.state.EstadoCancelada;
import proyectodp.state.EstadoCita;
import proyectodp.state.EstadoFinalizada;
import proyectodp.state.EstadoPendiente;
import proyectodp.state.EstadoReprogramada;

public class CitaDAO {

    private final GestionMemoria memoria;

    public CitaDAO() {
        this.memoria = GestionMemoria.getInstancia();
    }

    public static EstadoCita parseEstado(String estadoStr) {
        if (estadoStr == null) return new EstadoPendiente();
        switch (estadoStr.trim().toLowerCase()) {
            case "atendida": return new EstadoAtendida();
            case "reprogramada": return new EstadoReprogramada();
            case "cancelada": return new EstadoCancelada();
            case "finalizada": return new EstadoFinalizada();
            default: return new EstadoPendiente();
        }
    }

    public void insertar(CitasMontalvo cita) {
        // Mantener memoria actualizada siempre
        memoria.agregarCita(cita);

        ConexionDB pool = ConexionDB.getInstancia();
        if (pool.isConectado()) {
            String sql = "INSERT INTO citas (id, cliente, fecha, hora, estilista, servicio, precio, estado) VALUES (?, ?, ?::date, ?, ?, ?, ?, ?) "
                       + "ON CONFLICT (id) DO UPDATE SET cliente = EXCLUDED.cliente, fecha = EXCLUDED.fecha, hora = EXCLUDED.hora, estado = EXCLUDED.estado";
            try (Connection conn = pool.getConexion();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, cita.getId());
                ps.setString(2, cita.getNombreCliente());
                ps.setString(3, cita.getFecha());
                ps.setString(4, cita.getHora());
                ps.setString(5, cita.getEstilista());
                ps.setString(6, cita.getTipoServicio());
                ps.setDouble(7, cita.getPrecio());
                ps.setString(8, cita.getEstado().obtenerNombreEstado());
                ps.executeUpdate();
            } catch (Exception e) {
                System.err.println("[CitaDAO] Error al insertar en Supabase: " + e.getMessage());
            }
        }
    }

    public void actualizarEstado(String id, String nuevoEstado) {
        // Actualizar en memoria
        CitasMontalvo cita = memoria.buscarPorId(id);
        if (cita != null) {
            cita.setEstado(parseEstado(nuevoEstado));
        }

        ConexionDB pool = ConexionDB.getInstancia();
        if (pool.isConectado()) {
            String sql = "UPDATE citas SET estado = ? WHERE id = ?";
            try (Connection conn = pool.getConexion();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, nuevoEstado);
                ps.setString(2, id);
                ps.executeUpdate();
            } catch (Exception e) {
                System.err.println("[CitaDAO] Error al actualizar estado en Supabase: " + e.getMessage());
            }
        }
    }

    public void actualizarReprogramacion(String id, String nuevaFecha, String nuevaHora) {
        CitasMontalvo cita = memoria.buscarPorId(id);
        if (cita != null) {
            cita.setFecha(nuevaFecha);
            cita.setHora(nuevaHora);
            cita.setEstado(new EstadoReprogramada());
        }

        ConexionDB pool = ConexionDB.getInstancia();
        if (pool.isConectado()) {
            String sql = "UPDATE citas SET fecha = ?::date, hora = ?, estado = 'Reprogramada' WHERE id = ?";
            try (Connection conn = pool.getConexion();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, nuevaFecha);
                ps.setString(2, nuevaHora);
                ps.setString(3, id);
                ps.executeUpdate();
            } catch (Exception e) {
                System.err.println("[CitaDAO] Error al reprogramar en Supabase: " + e.getMessage());
            }
        }
    }

    public List<CitasMontalvo> listarTodas() {
        ConexionDB pool = ConexionDB.getInstancia();
        if (pool.isConectado()) {
            List<CitasMontalvo> listaBD = new ArrayList<>();
            String sql = "SELECT id, cliente, to_char(fecha, 'YYYY-MM-DD') as fecha_str, hora, estilista, servicio, precio, estado FROM citas ORDER BY fecha, hora";
            try (Connection conn = pool.getConexion();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CitasMontalvo c = new CitasMontalvo(
                        rs.getString("id"),
                        rs.getString("cliente"),
                        rs.getString("fecha_str"),
                        rs.getString("hora"),
                        rs.getString("servicio"),
                        rs.getDouble("precio"),
                        rs.getString("estilista"),
                        45,
                        parseEstado(rs.getString("estado"))
                    );
                    listaBD.add(c);
                }
                if (!listaBD.isEmpty()) {
                    return listaBD;
                }
            } catch (Exception e) {
                System.err.println("[CitaDAO] Error al consultar Supabase, usando memoria: " + e.getMessage());
            }
        }
        return memoria.listarCitas();
    }

    public List<CitasMontalvo> listarActivas(String estilista, String fecha) {
        List<CitasMontalvo> todas = listarTodas();
        List<CitasMontalvo> activas = new ArrayList<>();

        for (CitasMontalvo c : todas) {
            // Regla de Negocio: Las citas 'Finalizada' se excluyen de la agenda activa
            String estado = c.getEstado().obtenerNombreEstado();
            if ("Finalizada".equalsIgnoreCase(estado)) {
                continue;
            }

            if (estilista != null && !estilista.trim().isEmpty() && !c.getEstilista().equalsIgnoreCase(estilista.trim())) {
                continue;
            }

            if (fecha != null && !fecha.trim().isEmpty() && !c.getFecha().equals(fecha.trim())) {
                continue;
            }

            activas.add(c);
        }
        return activas;
    }

    public List<CitasMontalvo> listarPorRangoFechas(String fechaInicio, String fechaFin, String estilista, boolean incluirFinalizadas) {
        List<CitasMontalvo> todas = listarTodas();
        List<CitasMontalvo> filtradas = new ArrayList<>();

        for (CitasMontalvo c : todas) {
            String estado = c.getEstado().obtenerNombreEstado();
            if (!incluirFinalizadas && "Finalizada".equalsIgnoreCase(estado)) {
                continue;
            }

            if (estilista != null && !estilista.trim().isEmpty() && !c.getEstilista().equalsIgnoreCase(estilista.trim())) {
                continue;
            }

            if (fechaInicio != null && c.getFecha().compareTo(fechaInicio) < 0) {
                continue;
            }

            if (fechaFin != null && c.getFecha().compareTo(fechaFin) > 0) {
                continue;
            }

            filtradas.add(c);
        }
        return filtradas;
    }

    public CitasMontalvo buscarPorId(String id) {
        for (CitasMontalvo c : listarTodas()) {
            if (c.getId().equalsIgnoreCase(id)) {
                return c;
            }
        }
        return null;
    }
}
