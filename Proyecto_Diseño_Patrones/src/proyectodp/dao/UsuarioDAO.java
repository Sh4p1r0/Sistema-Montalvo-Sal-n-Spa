package proyectodp.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import proyectodp.config.ConexionDB;
import proyectodp.modelo.Usuario;
import proyectodp.seguridad.PasswordHasher;

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
        if (username == null || password == null || username.isBlank()) return null;

        String user = username.trim();
        String pass = password.trim();

        ConexionDB pool = ConexionDB.getInstancia();
        if (pool.isConectado()) {
            String sql = "SELECT id, username, password, rol, nombre, area_servicio FROM usuarios WHERE username = ?";
            try {
                Connection conn = pool.getConexion();
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, user);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            String almacenado = rs.getString("password");
                            if (!PasswordHasher.verificar(pass, almacenado)) {
                                return null;
                            }
                            Usuario u = new Usuario(
                                rs.getInt("id"),
                                rs.getString("username"),
                                almacenado,
                                rs.getString("rol"),
                                rs.getString("nombre"),
                                rs.getString("area_servicio")
                            );
                            if (PasswordHasher.necesitaMigracion(almacenado)) {
                                String hash = PasswordHasher.hash(pass);
                                actualizarPassword(user, hash);
                                u.setPassword(hash);
                            }
                            return u;
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[UsuarioDAO] Error al consultar Supabase, recurriendo a memoria: " + e.getMessage());
            }
        }

        // Fallback Memoria (también migra el texto plano a hash en memoria)
        for (Usuario u : USUARIOS_MEMORIA) {
            if (u.getUsername().equalsIgnoreCase(user) && PasswordHasher.verificar(pass, u.getPassword())) {
                if (PasswordHasher.necesitaMigracion(u.getPassword())) {
                    u.setPassword(PasswordHasher.hash(pass));
                }
                return u;
            }
        }
        return null;
    }

    private void actualizarPassword(String username, String hash) {
        ConexionDB pool = ConexionDB.getInstancia();
        if (!pool.isConectado()) return;
        String sql = "UPDATE usuarios SET password = ? WHERE username = ?";
        try {
            Connection conn = pool.getConexion();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, hash);
                ps.setString(2, username);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            System.err.println("[UsuarioDAO] No se pudo migrar la contraseña a hash: " + e.getMessage());
        }
    }

    public List<Usuario> listarEstilistas() {
        List<Usuario> estilistas = new ArrayList<>();
        ConexionDB pool = ConexionDB.getInstancia();
        if (pool.isConectado()) {
            String sql = "SELECT id, username, password, rol, nombre, area_servicio FROM usuarios WHERE rol = 'ESTILISTA' ORDER BY nombre";
            try {
                Connection conn = pool.getConexion();
                try (PreparedStatement ps = conn.prepareStatement(sql);
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
