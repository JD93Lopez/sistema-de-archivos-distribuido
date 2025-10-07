package central;
import java.io.Serializable;

public class Archivo implements Serializable {

    private int id;
    private String nombre;
    private String ruta;
    private byte[] contenido;
    private int idUsuario;

    public Archivo() {
    }

    public Archivo(String nombre, String ruta, byte[] contenido) {
        this.nombre = nombre;
        this.ruta = ruta;
        this.contenido = contenido;
    }

    // public Archivo(String nombre, byte[] contenido) {
    //     this.nombre = nombre;
    //     this.contenido = contenido;
    // }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
    
    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getRuta() {
        return ruta;
    }

    public void setRuta(String ruta) {
        this.ruta = ruta;
    }

    public byte[] getContenido() {
        return contenido;
    }

    public void setContenido(byte[] contenido) {
        this.contenido = contenido;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }
}