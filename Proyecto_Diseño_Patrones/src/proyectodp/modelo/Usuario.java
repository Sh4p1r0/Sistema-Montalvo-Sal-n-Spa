package proyectodp.modelo;

public class Usuario {

    private int id;
    private String username;
    private String password;
    private String rol; // 'ADMIN' o 'ESTILISTA'
    private String nombre;
    private String areaServicio;

    public Usuario(int id, String username, String password, String rol, String nombre, String areaServicio) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.rol = rol;
        this.nombre = nombre;
        this.areaServicio = areaServicio;
    }

    public Usuario(String username, String password, String rol, String nombre, String areaServicio) {
        this(0, username, password, rol, nombre, areaServicio);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getAreaServicio() {
        return areaServicio;
    }

    public void setAreaServicio(String areaServicio) {
        this.areaServicio = areaServicio;
    }

    public String toJson() {
        return String.format(
            "{\"id\":%d,\"username\":\"%s\",\"rol\":\"%s\",\"nombre\":\"%s\",\"areaServicio\":\"%s\"}",
            id, escapeJson(username), escapeJson(rol), escapeJson(nombre), escapeJson(areaServicio)
        );
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"");
    }
}
