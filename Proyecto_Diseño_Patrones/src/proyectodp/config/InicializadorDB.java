package proyectodp.config;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Inicializador automático del esquema y datos semilla en Supabase PostgreSQL.
 */
public class InicializadorDB {

    public static void main(String[] args) {
        System.out.println("[InicializadorDB] Conectando a Supabase para inicializar base de datos...");
        inicializarEsquema();
    }

    public static boolean inicializarEsquema() {
        ConexionDB pool = ConexionDB.getInstancia();
        if (!pool.isConectado()) {
            System.err.println("[InicializadorDB] No se puede inicializar: Conexión a Supabase inactiva.");
            return false;
        }

        File schemaFile = new File("schema.sql");
        if (!schemaFile.exists()) {
            schemaFile = new File("../schema.sql");
        }
        if (!schemaFile.exists()) {
            schemaFile = new File("Proyecto_Diseño_Patrones/schema.sql");
        }

        if (!schemaFile.exists()) {
            System.err.println("[InicializadorDB] Archivo schema.sql no encontrado.");
            return false;
        }

        try {
            // Verificar si la tabla de citas ya existe para no re-ejecutar el script semilla en cada inicio
            try (Connection conn = pool.getConexion();
                 Statement stmt = conn.createStatement();
                 java.sql.ResultSet rs = stmt.executeQuery("SELECT to_regclass('public.citas')")) {
                if (rs.next() && rs.getString(1) != null) {
                    System.out.println("[InicializadorDB] Tablas en Supabase ya verificadas y listas.");
                    return true;
                }
            } catch (Exception e) {
                // Si la tabla no existe, continuar con la creación normal
            }

            String sqlCompleto = Files.readString(schemaFile.toPath(), StandardCharsets.UTF_8);
            try (Connection conn = pool.getConexion();
                 Statement stmt = conn.createStatement()) {
                System.out.println("[InicializadorDB] Ejecutando script SQL en Supabase...");
                stmt.execute(sqlCompleto);
                System.out.println("[InicializadorDB] ¡Tablas y datos iniciales creados exitosamente en Supabase!");
                return true;
            }
        } catch (Exception e) {
            System.err.println("[InicializadorDB] Error al ejecutar script en Supabase: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
