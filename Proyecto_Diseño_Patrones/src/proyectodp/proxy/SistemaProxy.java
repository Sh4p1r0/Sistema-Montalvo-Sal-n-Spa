package proyectodp.proxy;

import java.util.List;
import proyectodp.dao.UsuarioDAO;
import proyectodp.modelo.CitasMontalvo;
import proyectodp.modelo.Usuario;

/**
 * Patrón de Diseño: PROXY (Virtual / Protección)
 * Controla el acceso al sistema real validando autenticación y permisos de roles
 * (ADMIN o ESTILISTA) mediante la base de datos o memoria de respaldo.
 */
public class SistemaProxy implements ISistemaAcceso {

    private final SistemaReal sistemaReal;
    private final UsuarioDAO usuarioDAO;
    private Usuario usuarioAutenticado;

    public SistemaProxy() {
        this.sistemaReal = new SistemaReal();
        this.usuarioDAO = new UsuarioDAO();
        this.usuarioAutenticado = null;
    }

    public Usuario login(String username, String password) {
        Usuario user = usuarioDAO.autenticar(username, password);
        if (user != null) {
            this.usuarioAutenticado = user;
            System.out.println("[Proxy] Sesión iniciada con éxito: " + user.getNombre() + " (" + user.getRol() + ")");
            return user;
        }
        this.usuarioAutenticado = null;
        System.out.println("[Proxy] Credenciales inválidas para usuario: " + username);
        return null;
    }

    public void logout() {
        if (this.usuarioAutenticado != null) {
            System.out.println("[Proxy] Sesión cerrada para: " + this.usuarioAutenticado.getUsername());
        }
        this.usuarioAutenticado = null;
    }

    public boolean isAutenticado() {
        return usuarioAutenticado != null;
    }

    public Usuario getUsuarioAutenticado() {
        return usuarioAutenticado;
    }

    public boolean esRecepcionista() {
        return usuarioAutenticado != null && ("RECEPCIONISTA".equalsIgnoreCase(usuarioAutenticado.getRol()) || "ADMIN".equalsIgnoreCase(usuarioAutenticado.getRol()));
    }

    public boolean esAdmin() {
        return usuarioAutenticado != null && "ADMIN".equalsIgnoreCase(usuarioAutenticado.getRol());
    }

    public boolean esEstilista() {
        return usuarioAutenticado != null && "ESTILISTA".equalsIgnoreCase(usuarioAutenticado.getRol());
    }

    @Override
    public void registrarCita(String cliente, String hora, String tipoServicio) {
        if (!isAutenticado()) {
            throw new SecurityException("Acceso denegado: debe iniciar sesión en el sistema.");
        }
        sistemaReal.registrarCita(cliente, hora, tipoServicio);
    }

    @Override
    public List<CitasMontalvo> obtenerCitas() {
        return sistemaReal.obtenerCitas();
    }

    @Override
    public void atenderCita(int indice) {
        if (!isAutenticado()) {
            throw new SecurityException("Acceso denegado: debe iniciar sesión.");
        }
        sistemaReal.atenderCita(indice);
    }

    @Override
    public void cancelarCita(int indice) {
        if (!isAutenticado()) {
            throw new SecurityException("Acceso denegado: debe iniciar sesión.");
        }
        sistemaReal.cancelarCita(indice);
    }
}
