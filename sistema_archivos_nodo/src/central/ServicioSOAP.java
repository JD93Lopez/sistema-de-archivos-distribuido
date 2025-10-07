package central;

public interface ServicioSOAP {
    String registrarse(String nombre, String email, String contrasena);
    String iniciarSesion(String email, String contrasena);
    
    void crearDirectorio(String ruta, String token);
    void subirArchivo(Archivo archivo, String token);
    Archivo descargarArchivo(String nombre, String token);
    void moverArchivo(String origen, String destino, String token);
    void eliminarArchivo(String nombre, String token);
    void compartirArchivo(Archivo archivo, Usuario usuario, String token);
    ArbolEspacio consultarEspacioConsumido(String token);
}