package proyectodp.main;

import java.io.File;
import proyectodp.servidor.ServidorWeb;

public class MainWeb {

    public static void main(String[] args) {
        try {
            // Ubicación de la carpeta web frontend
            File webDir = new File("web");
            if (!webDir.exists() || !new File(webDir, "styles.css").exists()) {
                webDir = new File("Proyecto_Diseño_Patrones/web");
            }
            if (!webDir.exists() || !new File(webDir, "styles.css").exists()) {
                webDir = new File("../web");
            }

            ServidorWeb servidor = new ServidorWeb(webDir);
            servidor.iniciar();

            System.out.println("Sistema de Citas Montalvo (Patrones de Diseño) activo.");
            System.out.println("Abre en tu navegador: http://localhost:8080");

            // Intentar abrir el navegador automáticamente si está soportado
            try {
                if (java.awt.Desktop.isDesktopSupported()
                        && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI("http://localhost:8080"));
                }
            } catch (Exception e) {
                // No crítico
            }

        } catch (Exception e) {
            System.err.println("Error al iniciar el servidor web: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
