package proyectodp.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import proyectodp.config.ConexionDB;
import proyectodp.modelo.Usuario;

public class UsuarioDAO {

    // Lista en memoria de respaldo para modo offline / sin base de datos activa
    private static final List<Usuario> USUARIOS_MEMORIA = new ArrayList<>();

    static {
        USUARIOS_MEMORIA.add(new Usuario(1, "recep", "montalvo", "RECEPCIONISTA", "Recepción Principal", "Caja y Atención General"));
        USUARIOS_MEMORIA.add(new Usuario(2, "admin", "montalvo123", "ADMIN", "Gerencia Montalvo", "Administración"));
        USUARIOS_MEMORIA.add(new Usuario(3, "carlos", "123456", "ESTILISTA", "Carlos Mendoza", "Corte y Barbería"));
        USUARIOS_MEMORIA.add(new Usuario(4, "sofia", "123456", "ESTILISTA", "Sofia Silva", "Peinados y Planchado"));
        USUARIOS_MEMORIA.add(new Usuario(5, "camila", "123456", "ESTILISTA", "Camila Rios", "Manicura y Uñas"));
        USUARIOS_MEMORIA.add(new Usuario(6, "diego", "123456", "ESTILISTA", "Diego Morales", "Spa y Masajes"));
    }

    public Usuario autenticar(String username, String password) {
        if (username == null || password == null) return null;

        ConexionDB pool = ConexionDB.getInstancia();
        if (pool.isConectado()) {
            String sql = "SELECT id, username, password, rol, nombre, area_servicio FROM usuarios WHERE username = ? AND password = ?";
            try (Connection conn = pool.getConexion();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username.trim());
                ps.setString(2, password.trim());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new Usuario(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("rol"),
                            rs.getString("nombre"),
                            rs.getString("area_servicio")
                        );
                    }
                }
            } catch (Exception e) {
                System.err.println("[UsuarioDAO] Error al consultar Supabase, recurriendo a memoria: " + e.getMessage());
            }
        }

        // Fallback Memoria
        for (Usuario u : USUARIOS_MEMORIA) {
            if (u.getUsername().equalsIgnoreCase(username.trim()) && u.getPassword().equals(password.trim())) {
                return u;
            }
        }
        return null;
    }

    public List<Usuario> listarEstilistas() {
        List<Usuario> estilistas = new ArrayList<>();
        ConexionDB pool = ConexionDB.getInstancia();
        if (pool.isConectado()) {
            String sql = "SELECT id, username, password, rol, nombre, area_servicio FROM usuarios WHERE rol = 'ESTILISTA' ORDER BY nombre";
            try (Connection conn = pool.getConexion();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    estilistas.add(new Usuario(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("rol"),
                        rs.getString("nombre"),
                        rs.getString("area_servicio")
                    ));
                }
                if (!estilistas.isEmpty()) {
                    return estilistas;
                }
            } catch (Exception e) {
                System.err.println("[UsuarioDAO] Error al listar estilistas de Supabase: " + e.getMessage());
            }
        }

        // Fallback Memoria
        for (Usuario u : USUARIOS_MEMORIA) {
            if ("ESTILISTA".equalsIgnoreCase(u.getRol())) {
                estilistas.add(u);
            }
        }
        return estilistas;
    }
}
