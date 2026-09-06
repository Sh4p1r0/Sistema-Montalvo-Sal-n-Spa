package proyectodp.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Patrón de Diseño: SINGLETON
 * Administra de manera centralizada y única la conexión a la base de datos PostgreSQL (Supabase).
 * Cuenta con detección automática de estado para fallback a memoria si no hay conexión disponible.
 */
public class ConexionDB {

    private static ConexionDB instancia;
    private Connection conexion;
    private String url;
    private String usuario;
    private String password;
    private boolean conectado = false;

    private ConexionDB() {
        cargarConfiguracion();
        probarConexion();
    }

    public static synchronized ConexionDB getInstancia() {
        if (instancia == null) {
            instancia = new ConexionDB();
        }
        return instancia;
    }

    private void cargarConfiguracion() {
        Properties prop = new Properties();
        File propFile = new File("db.properties");
        if (!propFile.exists()) {
            propFile = new File("Proyecto_Diseño_Patrones/db.properties");
        }

        if (propFile.exists()) {
            try (InputStream is = new FileInputStream(propFile)) {
                prop.load(is);
                this.url = prop.getProperty("db.url");
                this.usuario = prop.getProperty("db.user");
                this.password = prop.getProperty("db.password", "");
            } catch (Exception e) {
                System.err.println("[ConexionDB] Error al leer db.properties: " + e.getMessage());
            }
        }

        // Fallbacks por defecto con los datos de Supabase si no se encontraron en el archivo
        if (this.url == null || this.url.isEmpty()) {
            this.url = "jdbc:postgresql://aws-0-sa-east-1.pooler.supabase.com:5432/postgres?sslmode=require";
        }
        if (this.usuario == null || this.usuario.isEmpty()) {
            this.usuario = "postgres.cgexvsytoxdzronnmjlm";
        }
        if (this.password == null) {
            this.password = "";
        }
    }

    private void probarConexion() {
        if (password == null || password.trim().isEmpty() || "[YOUR-PASSWORD]".equals(password)) {
            System.out.println("[ConexionDB] Aviso: Contraseña de Supabase no configurada en db.properties.");
            System.out.println("[ConexionDB] Operando en Modo Híbrido: Persistencia en Memoria (Singleton Activo).");
            conectado = false;
            return;
        }

        try {
            Class.forName("org.postgresql.Driver");
            this.conexion = DriverManager.getConnection(url, usuario, password);
            this.conectado = (this.conexion != null && !this.conexion.isClosed());
            if (this.conectado) {
                System.out.println("[ConexionDB] Conexión exitosa a Supabase PostgreSQL.");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("[ConexionDB] Driver PostgreSQL no encontrado en Classpath: " + e.getMessage());
            this.conectado = false;
        } catch (SQLException e) {
            System.err.println("[ConexionDB] No se pudo conectar a Supabase: " + e.getMessage());
            System.out.println("[ConexionDB] Activando Modo Híbrido: Operaciones respaldadas en Memoria.");
            this.conectado = false;
        }
    }

    public synchronized Connection getConexion() throws SQLException {
        if (conexion == null || conexion.isClosed()) {
            if (password != null && !password.trim().isEmpty() && !"[YOUR-PASSWORD]".equals(password)) {
                try {
                    Class.forName("org.postgresql.Driver");
                    this.conexion = DriverManager.getConnection(url, usuario, password);
                    this.conectado = (this.conexion != null && !this.conexion.isClosed());
                } catch (Exception e) {
                    this.conectado = false;
                    throw new SQLException("Error al reconectar a la BD: " + e.getMessage());
                }
            }
        }
        return conexion;
    }

    public boolean isConectado() {
        try {
            return conectado && conexion != null && !conexion.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}
