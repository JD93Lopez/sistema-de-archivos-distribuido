package central;

import javax.jws.WebService;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.xml.ws.Endpoint;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import java.sql.SQLException;

@WebService
public class ServicioSOAPImpl implements ServicioSOAP {

    private ServidorAplicacion servidorAplicacion;
    private AuthService authService;
    
    private static boolean VALIDACION_TOKEN_HABILITADA = false;

    public ServicioSOAPImpl() {
        this.servidorAplicacion = new ServidorAplicacion();
        this.authService = new AuthService();
    }
    
    
    @WebMethod
    public String registrarse(@WebParam(name = "nombre") String nombre, 
                                  @WebParam(name = "email") String email, 
                                  @WebParam(name = "contrasena") String contrasena) {
        System.out.println("Registro solicitado para: " + email);
        return authService.registrarse(nombre, email, contrasena).toString();
    }

    @WebMethod
    public String iniciarSesion(@WebParam(name = "email") String email, 
                                    @WebParam(name = "contrasena") String contrasena) {
        System.out.println("Inicio de sesión solicitado para: " + email);
        return authService.iniciarSesion(email, contrasena).toString();
    }

    private int validarToken(String token) {
        if (!VALIDACION_TOKEN_HABILITADA) {
            System.out.println("usando usuario por defecto (ID: 1)");
            return 1;
        }
        
        if (token == null || token.trim().isEmpty()) {
            throw new RuntimeException("Token de autenticación requerido");
        }
        
        TokenManager.TokenInfo tokenInfo = authService.validarToken(token);
        if (tokenInfo == null) {
            throw new RuntimeException("Token de autenticación inválido");
        }
        
        System.out.println("Token valido para usuario ID: " + tokenInfo.userId);
        return tokenInfo.userId;
    }

    @WebMethod
    public void crearDirectorio(@WebParam(name = "ruta") String ruta, 
                               @WebParam(name = "token") String token) {
        int userId = validarToken(token);
        System.out.println("Crear directorio llamado: " + ruta + " (Usuario: " + userId + ")");
        servidorAplicacion.crearDirectorio(ruta, userId);
    }

    @WebMethod
    public void subirArchivo(@WebParam(name = "archivo") Archivo archivo, 
                           @WebParam(name = "token") String token) {
        int userId = validarToken(token);
        archivo.setIdUsuario(userId);
        System.out.println("Subir archivo: " + archivo.getNombre() + " (Usuario: " + userId + ")");
        servidorAplicacion.subirArchivo(archivo);
    }

    @WebMethod
    public Archivo descargarArchivo(@WebParam(name = "nombre") String nombre, 
                                  @WebParam(name = "token") String token) {
        int userId = validarToken(token);
        System.out.println("Descargar archivo: " + nombre + " (Usuario: " + userId + ")");
        Archivo archivo = servidorAplicacion.descargarArchivo(nombre, userId);
        if (archivo != null) {
            archivo.setIdUsuario(userId);
        }
        return archivo;
    }

    @WebMethod
    public void moverArchivo(@WebParam(name = "origen") String origen, 
                           @WebParam(name = "destino") String destino, 
                           @WebParam(name = "token") String token) {
        int userId = validarToken(token);
        System.out.println("Mover archivo de " + origen + " a " + destino + " (Usuario: " + userId + ")");
        servidorAplicacion.moverArchivo(origen, destino, userId);
    }

    @WebMethod
    public void eliminarArchivo(@WebParam(name = "nombre") String nombre, 
                              @WebParam(name = "token") String token) {
        int userId = validarToken(token);
        System.out.println("Eliminar archivo: " + nombre + " (Usuario: " + userId + ")");
        servidorAplicacion.eliminarArchivo(nombre, userId);
    }

    @WebMethod
    public void compartirArchivo(@WebParam(name = "archivo") Archivo archivo, 
                               @WebParam(name = "usuario") Usuario usuario, 
                               @WebParam(name = "token") String token) {
        int userId = validarToken(token);
        archivo.setIdUsuario(userId);
        System.out.println("Compartir archivo " + archivo.getNombre() + " con " + usuario.getNombre() + " (Usuario: " + userId + ")");
        servidorAplicacion.compartirArchivo(archivo, usuario);
    }

    @WebMethod
    public ArbolEspacio consultarEspacioConsumido(@WebParam(name = "token") String token) {
        int userId = validarToken(token);
        System.out.println("Consultar espacio consumido (Usuario: " + userId + ")");
        return servidorAplicacion.consultarEspacioConsumido(userId);
    }

    public static void main(String[] args) {
        
        Endpoint.publish("http://localhost:8080/ServicioSOAP", new ServicioSOAPImpl());
        System.out.println(" Servicio SOAP listo y esperando solicitudes...");

    }
}

class TokenManager {
    private static final String SECRET_KEY = "mi_clave_secreta_para_tokens_2024";
    private static final long TOKEN_VALIDITY = 24 * 60 * 60 * 1000;
    
    private static final Map<String, TokenInfo> validTokens = new ConcurrentHashMap<>();
    
    
    public static String generateToken(int userId, String email) {
        try {
            long timestamp = System.currentTimeMillis();
            long expiration = timestamp + TOKEN_VALIDITY;
            
            String userData = userId + ":" + email + ":" + expiration;
            
            String signature = createSignature(userData);

            String token = Base64.getEncoder().encodeToString(userData.getBytes()) + "." + signature;
            
            validTokens.put(token, new TokenInfo(userId, email, expiration));
            
            return token;
        } catch (Exception e) {
            throw new RuntimeException("Error al generar token", e);
        }
    }
    
    public static TokenInfo validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        
        try {
            TokenInfo tokenInfo = validTokens.get(token);
            if (tokenInfo == null) {
                return null;
            }
            
            if (System.currentTimeMillis() > tokenInfo.expiration) {
                validTokens.remove(token);
                return null;
            }
            
            String[] parts = token.split("\\.");
            if (parts.length != 2) {
                return null;
            }
            
            String payload = new String(Base64.getDecoder().decode(parts[0]));
            String signature = parts[1];
            
            String expectedSignature = createSignature(payload);
            if (!signature.equals(expectedSignature)) {
                validTokens.remove(token);
                return null;
            }
            
            return tokenInfo;
        } catch (Exception e) {
            return null;
        }
    }
    
    public static void invalidateToken(String token) {
        validTokens.remove(token);
    }
    
    private static String createSignature(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String signatureData = payload + SECRET_KEY;
            byte[] hashBytes = digest.digest(signatureData.getBytes());
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error al crear firma", e);
        }
    }
    
    public static void cleanupExpiredTokens() {
        long currentTime = System.currentTimeMillis();
        validTokens.entrySet().removeIf(entry -> 
            currentTime > entry.getValue().expiration
        );
    }
    
    public static class TokenInfo {
        public final int userId;
        public final String email;
        public final long expiration;
        
        public TokenInfo(int userId, String email, long expiration) {
            this.userId = userId;
            this.email = email;
            this.expiration = expiration;
        }
    }
}

class AuthService {
    private final ServidorBaseDatos baseDatos;
    
    public AuthService() {
        this.baseDatos = new ServidorBaseDatos();
    }
    
    public AuthResponse registrarse(String nombre, String email, String contrasena) {
        try {
            if (baseDatos.existeUsuarioPorEmail(email)) {
                return new AuthResponse(false, "El email ya está registrado", null, null);
            }
            
            String contrasenaEncriptada = encriptarContrasena(contrasena);
            
            Usuario usuario = new Usuario(null, nombre, email, contrasenaEncriptada, null);
            int userId = baseDatos.registrarUsuario(usuario);
            
            if (userId > 0) {
                String token = TokenManager.generateToken(userId, email);
                
                return new AuthResponse(true, "Usuario registrado exitosamente", 
                                      token, new Usuario(String.valueOf(userId), nombre, email, null, null));
            } else {
                return new AuthResponse(false, "Error al registrar usuario", null, null);
            }
            
        } catch (SQLException e) {
            System.err.println("Error en el registro: " + e.getMessage());
            return new AuthResponse(false, "Error interno del servidor", null, null);
        }
    }
    
    public AuthResponse iniciarSesion(String email, String contrasena) {
        try {
            Usuario usuario = baseDatos.buscarUsuarioPorEmail(email);
            if (usuario == null) {
                return new AuthResponse(false, "Email o contraseña incorrectos", null, null);
            }

            String contrasenaEncriptada = encriptarContrasena(contrasena);
            if (!contrasenaEncriptada.equals(usuario.getContrasena())) {
                return new AuthResponse(false, "Email o contraseña incorrectos", null, null);
            }
            
            String token = TokenManager.generateToken(Integer.parseInt(usuario.getId()), email);
            
            Usuario usuarioSinContrasena = new Usuario(usuario.getId(), usuario.getNombre(), 
                                                     usuario.getEmail(), null, usuario.getPermisos());
            
            return new AuthResponse(true, "Inicio de sesión exitoso", token, usuarioSinContrasena);
            
        } catch (SQLException e) {
            System.err.println("Error en el inicio de sesión: " + e.getMessage());
            return new AuthResponse(false, "Error interno del servidor", null, null);
        }
    }
    
    public TokenManager.TokenInfo validarToken(String token) {
        return TokenManager.validateToken(token);
    }
    
    private String encriptarContrasena(String contrasena) {
        try {
            String salt = "sistema_archivos_salt_2024";
            String input = contrasena + salt;
            
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes());
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error al encriptar contraseña", e);
        }
    }
}

class AuthResponse {
    private boolean exitoso;
    private String mensaje;
    private String token;
    private Usuario usuario;
    
    public AuthResponse() {
    }
    
    public AuthResponse(boolean exitoso, String mensaje, String token, Usuario usuario) {
        this.exitoso = exitoso;
        this.mensaje = mensaje;
        this.token = token;
        this.usuario = usuario;
    }
    
    public boolean isExitoso() {
        return exitoso;
    }
    
    public void setExitoso(boolean exitoso) {
        this.exitoso = exitoso;
    }
    
    public String getMensaje() {
        return mensaje;
    }
    
    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public Usuario getUsuario() {
        return usuario;
    }
    
    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }
    
    @Override
    public String toString() {
        return "AuthResponse{" +
                "exitoso=" + exitoso +
                ", mensaje='" + mensaje + '\'' +
                ", token='" + token + '\'' +
                ", usuario=" + usuario +
                '}';
    }
}