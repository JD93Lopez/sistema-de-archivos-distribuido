package backend_cliente;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;

import backend_cliente.generated.*;

public class Main {

    private static final backend_cliente.generated.ServicioSOAPImpl soapPort = createSoapPort();

    private static backend_cliente.generated.ServicioSOAPImpl createSoapPort() {
        ServicioSOAPImplService service = new ServicioSOAPImplService();
        backend_cliente.generated.ServicioSOAPImpl port = service.getServicioSOAPImplPort();
        return port;
    }

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);
        server.createContext("/registro", new RegistroHandler());
        server.createContext("/login", new LoginHandler());
        server.createContext("/directorio", new CrearDirectorioHandler());
        server.createContext("/archivo/subir", new SubirArchivoHandler());
        server.createContext("/archivo/descargar", new DescargarArchivoHandler());
        server.createContext("/archivo/mover", new MoverArchivoHandler());
        server.createContext("/archivo", new EliminarArchivoHandler());
        server.createContext("/archivo/compartir", new CompartirArchivoHandler());
        server.createContext("/espacio", new ConsultarEspacioHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("✅ Servicio REST iniciado en http://localhost:8081");
    }

    static String readBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int nRead;
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }

    static void sendJsonResponse(HttpExchange exchange, int code, Object obj) throws IOException {
        String json = JsonParser.toJson(obj);
        byte[] response = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(code, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    static void sendErrorResponse(HttpExchange exchange, int code, String message) throws IOException {
        sendJsonResponse(exchange, code, Collections.singletonMap("error", message));
    }

    static String getTokenFromAuthHeader(HttpExchange exchange) {
        List<String> auth = exchange.getRequestHeaders().get("Authorization");
        if (auth == null || auth.isEmpty()) return null;
        String header = auth.get(0);
        if (header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    static String requireParam(Map<String, Object> json, String key) {
        Object val = json.get(key);
        if (val == null || !(val instanceof String) || ((String) val).isEmpty()) {
            throw new IllegalArgumentException("Falta el parámetro: " + key);
        }
        return (String) val;
    }

    static void requireMethod(HttpExchange exchange, String method) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase(method)) {
            sendErrorResponse(exchange, 405, "Método no permitido");
            throw new RuntimeException("Método no permitido");
        }
    }

    static class RegistroHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            try {
                requireMethod(exchange, "POST");
                Map<String, Object> json = JsonParser.parse(readBody(exchange));
                String nombre = requireParam(json, "nombre");
                String email = requireParam(json, "email");
                String contrasena = requireParam(json, "contrasena");

                String token = soapPort.registrarse(nombre, email, contrasena);
                Map<String, Object> responseMap = new HashMap<>();
                responseMap.put("mensaje", "Registro exitoso");
                responseMap.put("token", token);
                sendJsonResponse(exchange, 200, responseMap);
            } catch (Exception e) {
                sendErrorResponse(exchange, 400, e.getMessage());
            }
        }
    }

    static class LoginHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            try {
                requireMethod(exchange, "POST");
                Map<String, Object> json = JsonParser.parse(readBody(exchange));
                String email = requireParam(json, "email");
                String contrasena = requireParam(json, "contrasena");

                String token = soapPort.iniciarSesion(email, contrasena);
                Map<String, Object> responseMap = new HashMap<>();
                responseMap.put("token", token);
                sendJsonResponse(exchange, 200, responseMap);
            } catch (Exception e) {
                sendErrorResponse(exchange, 400, e.getMessage());
            }
        }
    }

    static class CrearDirectorioHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            try {
                requireMethod(exchange, "POST");
                Map<String, Object> json = JsonParser.parse(readBody(exchange));
                String ruta = requireParam(json, "ruta");
                String token = requireParam(json, "token");

                soapPort.crearDirectorio(ruta, token);
                Map<String, Object> responseMap = new HashMap<>();
                responseMap.put("mensaje", "Directorio creado");
                sendJsonResponse(exchange, 200, responseMap);
            } catch (Exception e) {
                sendErrorResponse(exchange, 400, e.getMessage());
            }
        }
    }

    static class SubirArchivoHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            try {
                requireMethod(exchange, "POST");
                Map<String, Object> json = JsonParser.parse(readBody(exchange));
                @SuppressWarnings("unchecked")
                Map<String, Object> archivoJson = (Map<String, Object>) json.get("archivo");
                if (archivoJson == null) throw new IllegalArgumentException("Falta 'archivo'");
                String nombre = requireParam(archivoJson, "nombre");
                String ruta = requireParam(archivoJson, "ruta");
                String contenidoB64 = requireParam(archivoJson, "contenido");
                String token = requireParam(json, "token");

                backend_cliente.generated.Archivo archivo = new backend_cliente.generated.Archivo();
                archivo.setNombre(nombre);
                archivo.setRuta(ruta);
                archivo.setContenido(Base64.getDecoder().decode(contenidoB64));

                soapPort.subirArchivo(archivo, token);
                Map<String, Object> responseMap = new HashMap<>();
                responseMap.put("mensaje", "Archivo subido");
                sendJsonResponse(exchange, 200, responseMap);
            } catch (Exception e) {
                sendErrorResponse(exchange, 400, e.getMessage());
            }
        }
    }

    static class DescargarArchivoHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            try {
                requireMethod(exchange, "GET");
                String query = exchange.getRequestURI().getQuery();
                String nombre = getQueryParam(query, "nombre");
                String token = getQueryParam(query, "token");
                if (nombre == null || nombre.isEmpty()) {
                    throw new IllegalArgumentException("Falta parámetro 'nombre'");
                }
                if (token == null || token.isEmpty()) {
                    token = getTokenFromAuthHeader(exchange);
                    if (token == null) throw new IllegalArgumentException("Falta token");
                }

                backend_cliente.generated.Archivo archivo = soapPort.descargarArchivo(nombre, token);
                sendJsonResponse(exchange, 200, archivoToString(archivo));
            } catch (Exception e) {
                sendErrorResponse(exchange, 400, e.getMessage());
            }
        }

        private String archivoToString (backend_cliente.generated.Archivo archivo) {
            StringBuilder stringFile = new StringBuilder();
            stringFile.append("{");
            stringFile.append("nombre:").append(archivo.getNombre() != null ? archivo.getNombre() : "").append(",");
            stringFile.append("ruta:").append(archivo.getRuta() != null ? archivo.getRuta() : "").append(",");
            stringFile.append("contenido:").append(archivo.getContenido() != null ? Base64.getEncoder().encodeToString(archivo.getContenido()) : "");
            stringFile.append("}");
            return stringFile.toString();
        }
    }

    static class MoverArchivoHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            try {
                requireMethod(exchange, "POST");
                Map<String, Object> json = JsonParser.parse(readBody(exchange));
                String origen = requireParam(json, "origen");
                String destino = requireParam(json, "destino");
                String token = requireParam(json, "token");

                soapPort.moverArchivo(origen, destino, token);
                Map<String, Object> responseMap = new HashMap<>();
                responseMap.put("mensaje", "Archivo movido");
                sendJsonResponse(exchange, 200, responseMap);
            } catch (Exception e) {
                sendErrorResponse(exchange, 400, e.getMessage());
            }
        }
    }

    static class EliminarArchivoHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            try {
                requireMethod(exchange, "DELETE");
                String query = exchange.getRequestURI().getQuery();
                String nombre = getQueryParam(query, "nombre");
                String token = getQueryParam(query, "token");
                if (nombre == null || nombre.isEmpty()) {
                    throw new IllegalArgumentException("Falta parámetro 'nombre'");
                }
                if (token == null || token.isEmpty()) {
                    token = getTokenFromAuthHeader(exchange);
                    if (token == null) throw new IllegalArgumentException("Falta token");
                }

                soapPort.eliminarArchivo(nombre, token);
                Map<String, Object> responseMap = new HashMap<>();
                responseMap.put("mensaje", "Archivo eliminado");
                sendJsonResponse(exchange, 200, responseMap);
            } catch (Exception e) {
                sendErrorResponse(exchange, 400, e.getMessage());
            }
        }
    }

    static class CompartirArchivoHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            try {
                requireMethod(exchange, "POST");
                Map<String, Object> json = JsonParser.parse(readBody(exchange));
                @SuppressWarnings("unchecked")
                Map<String, Object> archivoJson = (Map<String, Object>) json.get("archivo");
                @SuppressWarnings("unchecked")
                Map<String, Object> usuarioJson = (Map<String, Object>) json.get("usuario");
                if (archivoJson == null) throw new IllegalArgumentException("Falta 'archivo'");
                if (usuarioJson == null) throw new IllegalArgumentException("Falta 'usuario'");
                String token = requireParam(json, "token");

                String archivoNombre = requireParam(archivoJson, "nombre");
                String archivoRuta = requireParam(archivoJson, "ruta");
                String archivoContenido = requireParam(archivoJson, "contenido");
                String usuarioNombre = requireParam(usuarioJson, "nombre");
                String usuarioEmail = requireParam(usuarioJson, "email");

                backend_cliente.generated.Archivo archivo = new backend_cliente.generated.Archivo();
                archivo.setNombre(archivoNombre);
                archivo.setRuta(archivoRuta);
                archivo.setContenido(Base64.getDecoder().decode(archivoContenido));

                backend_cliente.generated.Usuario usuario = new backend_cliente.generated.Usuario();
                usuario.setNombre(usuarioNombre);
                usuario.setEmail(usuarioEmail);

                soapPort.compartirArchivo(archivo, usuario, token);
                Map<String, Object> responseMap = new HashMap<>();
                responseMap.put("mensaje", "Archivo compartido");
                sendJsonResponse(exchange, 200, responseMap);
            } catch (Exception e) {
                sendErrorResponse(exchange, 400, e.getMessage());
            }
        }
    }

    static class ConsultarEspacioHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            try {
                requireMethod(exchange, "GET");
                String token = getTokenFromAuthHeader(exchange);
                if (token == null) {
                    throw new IllegalArgumentException("Token requerido en header: Authorization: Bearer <token>");
                }

                backend_cliente.generated.ArbolEspacio arbol = soapPort.consultarEspacioConsumido(token);
                sendJsonResponse(exchange, 200, arbol);
            } catch (Exception e) {
                sendErrorResponse(exchange, 400, e.getMessage());
            }
        }
    }

    static String getQueryParam(String query, String param) {
        if (query == null) return null;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && kv[0].equals(param)) {
                try {
                    return java.net.URLDecoder.decode(kv[1], "UTF-8");
                } catch (Exception e) {
                    return kv[1];
                }
            }
        }
        return null;
    }
}