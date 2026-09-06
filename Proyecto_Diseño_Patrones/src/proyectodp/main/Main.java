package proyectodp.main;

/**
 * Punto de entrada principal del Sistema Montalvo Salón & Spa.
 * Inicia el Servidor Web nativo y abre la aplicación en el navegador.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("  MONTALVO SALÓN & SPA - SISTEMA DE CITAS WEB     ");
        System.out.println("  Arquitectura de Patrones de Diseño (GoF)       ");
        System.out.println("==================================================");

        // Delegar ejecución al servidor web
        MainWeb.main(args);
    }
}
