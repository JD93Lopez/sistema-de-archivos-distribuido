package central;

import java.util.List;

public class Solicitud {
    private TipoSolicitud tipo;
    private Archivo archivo;
    private List<Usuario> usuarios;
    private String rutaDestino;

    public Solicitud(TipoSolicitud tipo, Archivo archivo, List<Usuario> usuarios) {
        this.tipo = tipo;
        this.archivo = archivo;
        this.usuarios = usuarios;
    }

    // Getters y Setters
    public TipoSolicitud getTipo() {
        return tipo;
    }

    public void setTipo(TipoSolicitud tipo) {
        this.tipo = tipo;
    }

    public Archivo getArchivo() {
        return archivo;
    }

    public void setArchivo(Archivo archivo) {
        this.archivo = archivo;
    }

    public List<Usuario> getUsuarios() {
        return usuarios;
    }

    public void setUsuarios(List<Usuario> usuarios) {
        this.usuarios = usuarios;
    }

    public String getRutaDestino() {
        return rutaDestino;
    }

    public void setRutaDestino(String rutaDestino) {
        this.rutaDestino = rutaDestino;
    }

    @Override
    public String toString() {
        return "Solicitud{" +
                "tipo=" + tipo +
                ", archivo=" + archivo.getNombre() +
                ", usuarios=" + usuarios.size() +
                '}';
    }
}