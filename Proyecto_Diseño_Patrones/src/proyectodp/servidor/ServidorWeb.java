package proyectodp.servidor;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import proyectodp.facade.SistemaFacade;
import proyectodp.modelo.CitasMontalvo;
import proyectodp.modelo.Usuario;

public class ServidorWeb {

    private static final int PUERTO = 8080;
    private final SistemaFacade facade;
    private final File webDir;

    public ServidorWeb(File webDir) {
        proyectodp.config.InicializadorDB.inicializarEsquema();
        this.facade = new SistemaFacade();
        this.webDir = webDir;
        precargarDatosDemo();
    }

    private void precargarDatosDemo() {
        if (facade.obtenerCitas().isEmpty()) {
            String hoy = LocalDate.now().toString();
            String manana = LocalDate.now().plusDays(1).toString();
            try {
                facade.registrarCita("Luciana Ramos", hoy, "10:00", "CORTE", "Carlos Mendoza");
                facade.registrarCita("Andrea Paredes", hoy, "11:30", "PLANCHADO", "Sofia Silva");
                facade.registrarCita("Valeria Castro", hoy, "15:00", "MANICURA", "Camila Rios");
                facade.registrarCita("Gabriel Mendoza", manana, "14:00", "CORTE", "Carlos Mendoza");
                facade.registrarCita("Paola Navarro", manana, "16:30", "MASAJE", "Diego Morales");
            } catch (Exception e) {
                System.err.println("Aviso al precargar datos demo: " + e.getMessage());
            }
        }
    }

    public void iniciar() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PUERTO), 0);

        // Rutas API REST
        server.createContext("/api/login", new LoginHandler());
        server.createContext("/api/citas", new CitasHandler());
        server.createContext("/api/citas/reprogramar", new ReprogramarHandler());
        server.createContext("/api/citas/estado", new EstadoHandler());
        server.createContext("/api/citas/finalizar", new FinalizarHandler());
        server.createContext("/api/servicios", new ServiciosHandler());
        server.createContext("/api/estilistas", new EstilistasHandler());
        server.createContext("/api/stats", new StatsHandler());
        server.createContext("/api/caja", new CajaHandler());

        // Manejador de archivos estáticos Frontend
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(null);
        server.start();

        System.out.println("=================================================");
        System.out.println("  SERVIDOR MONTALVO SALON & SPA INICIADO");
        System.out.println("  URL Web:    http://localhost:" + PUERTO);
        System.out.println("  API REST:   http://localhost:" + PUERTO + "/api/citas");
        System.out.println("  API Caja:   http://localhost:" + PUERTO + "/api/caja");
        System.out.println("  API Stats:  http://localhost:" + PUERTO + "/api/stats");
        System.out.println("=================================================");
    }

    private static void aplicarCors(HttpExchange exchange) {
        Headers headers = exchange.getResponseHeaders();
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");
        headers.set("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.set("Pragma", "no-cache");
        headers.set("Expires", "0");
    }

    private static void responderJson(HttpExchange exchange, int statusCode, String jsonResponse) throws IOException {
        aplicarCors(exchange);
        byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String leerCuerpo(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String extraerParametroJson(String json, String campo) {
        if (json == null || campo == null) return "";
        // Capturar valor con comillas dobles o simples: "campo" : "valor"
        java.util.regex.Pattern p1 = java.util.regex.Pattern.compile("[\"']?" + java.util.regex.Pattern.quote(campo) + "[\"']?\\s*:\\s*[\"']([^\"']*)[\"']");
        java.util.regex.Matcher m1 = p1.matcher(json);
        if (m1.find()) {
            return m1.group(1).trim();
        }
        // Capturar valor sin comillas: "campo" : 123
        java.util.regex.Pattern p2 = java.util.regex.Pattern.compile("[\"']?" + java.util.regex.Pattern.quote(campo) + "[\"']?\\s*:\\s*([^,}\\s]+)");
        java.util.regex.Matcher m2 = p2.matcher(json);
        if (m2.find()) {
            return m2.group(1).trim();
        }
        return "";
    }

    // Handler de Login (/api/login)
    class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            aplicarCors(exchange);
            String metodo = exchange.getRequestMethod();

            if ("OPTIONS".equalsIgnoreCase(metodo)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("POST".equalsIgnoreCase(metodo)) {
                try {
                    String cuerpo = leerCuerpo(exchange);
                    String username = extraerParametroJson(cuerpo, "username");
                    String password = extraerParametroJson(cuerpo, "password");

                    Usuario usuario = facade.autenticar(username, password);
                    if (usuario != null) {
                        responderJson(exchange, 200, "{\"success\":true,\"usuario\":" + usuario.toJson() + "}");
                    } else {
                        responderJson(exchange, 401, "{\"success\":false,\"error\":\"Usuario o contraseña incorrectos.\"}");
                    }
                } catch (Exception ex) {
                    responderJson(exchange, 500, "{\"success\":false,\"error\":\"Error en autenticación: " + ex.getMessage().replace("\"", "'") + "\"}");
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    // Handler de Citas (/api/citas) con filtros por fecha y estilista
    class CitasHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            aplicarCors(exchange);
            String metodo = exchange.getRequestMethod();

            if ("OPTIONS".equalsIgnoreCase(metodo)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("GET".equalsIgnoreCase(metodo)) {
                String query = exchange.getRequestURI().getQuery();
                String fecha = null;
                String fechaInicio = null;
                String fechaFin = null;
                String periodo = null;
                String estilista = null;
                boolean todas = false;

                if (query != null) {
                    for (String param : query.split("&")) {
                        String[] par = param.split("=");
                        if (par.length >= 2) {
                            String clave = par[0].trim();
                            String valor = java.net.URLDecoder.decode(par[1].trim(), StandardCharsets.UTF_8);
                            if ("fecha".equalsIgnoreCase(clave)) fecha = valor;
                            else if ("fechaInicio".equalsIgnoreCase(clave)) fechaInicio = valor;
                            else if ("fechaFin".equalsIgnoreCase(clave)) fechaFin = valor;
                            else if ("periodo".equalsIgnoreCase(clave)) periodo = valor;
                            else if ("estilista".equalsIgnoreCase(clave)) estilista = valor;
                            else if ("todas".equalsIgnoreCase(clave)) todas = Boolean.parseBoolean(valor);
                        }
                    }
                }

                if (periodo != null) {
                    LocalDate hoy = LocalDate.now();
                    if ("ayer".equalsIgnoreCase(periodo)) {
                        fechaInicio = hoy.minusDays(1).toString();
                        fechaFin = fechaInicio;
                    } else if ("hoy".equalsIgnoreCase(periodo)) {
                        fechaInicio = hoy.toString();
                        fechaFin = fechaInicio;
                    } else if ("semana".equalsIgnoreCase(periodo)) {
                        fechaInicio = hoy.minusDays(hoy.getDayOfWeek().getValue() - 1).toString();
                        fechaFin = hoy.minusDays(hoy.getDayOfWeek().getValue() - 1).plusDays(6).toString();
                    }
                }

                List<CitasMontalvo> lista;
                if (todas) {
                    lista = facade.obtenerCitas();
                } else if (fechaInicio != null && fechaFin != null) {
                    lista = facade.obtenerCitasRango(fechaInicio, fechaFin, estilista, false);
                } else {
                    lista = facade.obtenerCitasActivas(estilista, fecha);
                }

                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < lista.size(); i++) {
                    sb.append(lista.get(i).toJson());
                    if (i < lista.size() - 1) sb.append(",");
                }
                sb.append("]");
                responderJson(exchange, 200, sb.toString());

            } else if ("POST".equalsIgnoreCase(metodo)) {
                try {
                    String cuerpo = leerCuerpo(exchange);
                    String cliente = extraerParametroJson(cuerpo, "cliente");
                    String fecha = extraerParametroJson(cuerpo, "fecha");
                    String hora = extraerParametroJson(cuerpo, "hora");
                    String tipoServicio = extraerParametroJson(cuerpo, "tipoServicio");
                    String estilista = extraerParametroJson(cuerpo, "estilista");

                    CitasMontalvo nuevaCita = facade.registrarCita(cliente, fecha, hora, tipoServicio, estilista);
                    responderJson(exchange, 201, "{\"success\":true,\"mensaje\":\"Cita agendada con éxito\",\"cita\":" + nuevaCita.toJson() + "}");
                } catch (Exception ex) {
                    responderJson(exchange, 400, "{\"success\":false,\"error\":\"" + ex.getMessage().replace("\"", "'") + "\"}");
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    // Handler de Reprogramación (/api/citas/reprogramar)
    class ReprogramarHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            aplicarCors(exchange);
            String metodo = exchange.getRequestMethod();

            if ("OPTIONS".equalsIgnoreCase(metodo)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("POST".equalsIgnoreCase(metodo) || "PUT".equalsIgnoreCase(metodo)) {
                try {
                    String cuerpo = leerCuerpo(exchange);
                    String id = extraerParametroJson(cuerpo, "id");
                    String nuevaFecha = extraerParametroJson(cuerpo, "nuevaFecha");
                    String nuevaHora = extraerParametroJson(cuerpo, "nuevaHora");

                    facade.reprogramarCita(id, nuevaFecha, nuevaHora);
                    CitasMontalvo citaActualizada = facade.buscarPorId(id);
                    responderJson(exchange, 200, "{\"success\":true,\"mensaje\":\"Cita reprogramada con éxito (Patrón State)\",\"cita\":" + (citaActualizada != null ? citaActualizada.toJson() : "{}") + "}");
                } catch (Exception ex) {
                    responderJson(exchange, 400, "{\"success\":false,\"error\":\"" + ex.getMessage().replace("\"", "'") + "\"}");
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    // Handler de cambio de Estado (/api/citas/estado)
    class EstadoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            aplicarCors(exchange);
            String metodo = exchange.getRequestMethod();

            if ("OPTIONS".equalsIgnoreCase(metodo)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("POST".equalsIgnoreCase(metodo)) {
                try {
                    String cuerpo = leerCuerpo(exchange);
                    String id = extraerParametroJson(cuerpo, "id");
                    String accion = extraerParametroJson(cuerpo, "accion");

                    if ("atender".equalsIgnoreCase(accion)) {
                        facade.atenderCita(id);
                    } else if ("finalizar".equalsIgnoreCase(accion)) {
                        facade.finalizarCita(id);
                    } else if ("cancelar".equalsIgnoreCase(accion)) {
                        facade.cancelarCita(id);
                    } else {
                        throw new IllegalArgumentException("Acción desconocida: " + accion);
                    }

                    CitasMontalvo citaActualizada = facade.buscarPorId(id);
                    responderJson(exchange, 200, "{\"success\":true,\"mensaje\":\"Estado actualizado correctamente\",\"cita\":" + (citaActualizada != null ? citaActualizada.toJson() : "{}") + "}");
                } catch (Exception ex) {
                    responderJson(exchange, 400, "{\"success\":false,\"error\":\"" + ex.getMessage().replace("\"", "'") + "\"}");
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    // Handler directo para Finalizar Cita (/api/citas/finalizar)
    class FinalizarHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            aplicarCors(exchange);
            String metodo = exchange.getRequestMethod();

            if ("OPTIONS".equalsIgnoreCase(metodo)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("POST".equalsIgnoreCase(metodo)) {
                try {
                    String cuerpo = leerCuerpo(exchange);
                    String id = extraerParametroJson(cuerpo, "id");
                    facade.finalizarCita(id);
                    responderJson(exchange, 200, "{\"success\":true,\"mensaje\":\"Cita finalizada y archivada exitosamente (Patrón State)\"}");
                } catch (Exception ex) {
                    responderJson(exchange, 400, "{\"success\":false,\"error\":\"" + ex.getMessage().replace("\"", "'") + "\"}");
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    // Handler de Servicios (/api/servicios)
    class ServiciosHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            aplicarCors(exchange);
            String json = "[" +
                "{\"codigo\":\"CORTE\",\"nombre\":\"Corte y Peinado\",\"precio\":35.00,\"duracion\":45,\"categoria\":\"Corte y Barbería\"}," +
                "{\"codigo\":\"PLANCHADO\",\"nombre\":\"Planchado y Cepillado\",\"precio\":50.00,\"duracion\":60,\"categoria\":\"Peinados y Planchado\"}," +
                "{\"codigo\":\"MANICURA\",\"nombre\":\"Manicura Spa Rusa\",\"precio\":28.00,\"duracion\":45,\"categoria\":\"Manicura y Uñas\"}," +
                "{\"codigo\":\"MASAJE\",\"nombre\":\"Masaje Terapéutico Relajante\",\"precio\":65.00,\"duracion\":50,\"categoria\":\"Spa y Masajes\"}" +
                "]";
            responderJson(exchange, 200, json);
        }
    }

    // Handler de Estilistas (/api/estilistas) cargado desde la BD / Usuarios
    class EstilistasHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            aplicarCors(exchange);
            List<Usuario> estilistas = facade.obtenerEstilistas();
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < estilistas.size(); i++) {
                Usuario u = estilistas.get(i);
                sb.append(String.format(
                    "{\"id\":%d,\"nombre\":\"%s\",\"especialidad\":\"%s\",\"area\":\"%s\"}",
                    u.getId(), u.getNombre(), u.getAreaServicio(), u.getAreaServicio()
                ));
                if (i < estilistas.size() - 1) sb.append(",");
            }
            sb.append("]");
            responderJson(exchange, 200, sb.toString());
        }
    }

    // Handler de Estadísticas del Administrador (/api/stats)
    class StatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            aplicarCors(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            String statsJson = facade.obtenerEstadisticasAdminJson();
            responderJson(exchange, 200, statsJson);
        }
    }

    // Handler de Cuadre de Caja para Recepción (/api/caja)
    class CajaHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            aplicarCors(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            String query = exchange.getRequestURI().getQuery();
            String periodo = "hoy";
            String fecha = null;
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] par = param.split("=");
                    if (par.length >= 2) {
                        String clave = par[0].trim();
                        String valor = java.net.URLDecoder.decode(par[1].trim(), StandardCharsets.UTF_8);
                        if ("periodo".equalsIgnoreCase(clave)) periodo = valor;
                        else if ("fecha".equalsIgnoreCase(clave)) fecha = valor;
                    }
                }
            }
            String cajaJson = facade.obtenerDetalleCajaJson(periodo, fecha);
            responderJson(exchange, 200, cajaJson);
        }
    }

    // Handler para servir la Web estática
    class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String ruta = exchange.getRequestURI().getPath();
            if (ruta == null || ruta.equals("/") || ruta.isEmpty()) {
                ruta = "/index.html";
            }

            File archivo = new File(webDir, ruta.substring(1));
            if (!archivo.exists() || archivo.isDirectory()) {
                archivo = new File(webDir, "index.html");
            }

            if (!archivo.exists()) {
                String error = "<html><body><h1>404: Archivo no encontrado en " + webDir.getAbsolutePath() + "</h1></body></html>";
                exchange.sendResponseHeaders(404, error.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(error.getBytes(StandardCharsets.UTF_8));
                }
                return;
            }

            String contentType = "text/html; charset=UTF-8";
            if (archivo.getName().endsWith(".css")) {
                contentType = "text/css; charset=UTF-8";
            } else if (archivo.getName().endsWith(".js")) {
                contentType = "application/javascript; charset=UTF-8";
            } else if (archivo.getName().endsWith(".svg")) {
                contentType = "image/svg+xml";
            } else if (archivo.getName().endsWith(".png")) {
                contentType = "image/png";
            }

            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, archivo.length());
            try (InputStream is = new FileInputStream(archivo); OutputStream os = exchange.getResponseBody()) {
                byte[] buffer = new byte[4096];
                int n;
                while ((n = is.read(buffer)) != -1) {
                    os.write(buffer, 0, n);
                }
            }
        }
    }
}
