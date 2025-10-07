package nodo;

import central.Archivo;
import central.Usuario;
import central.TipoSolicitud;

public class Tarea {
    private String descripcion;
    private int prioridad;
    private TipoSolicitud tipoTarea;
    private String ruta;
    private String rutaOrigen;
    private String rutaDestino;
    private String nombreArchivo;
    private Archivo archivo;
    private Usuario usuario;
    
    public Tarea(String descripcion, int prioridad) {
        this.descripcion = descripcion;
        this.prioridad = prioridad;
    }
    
    
    public String getDescripcion() {
        return descripcion;
    }
    
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
    
    public int getPrioridad() {
        return prioridad;
    }
    
    public void setPrioridad(int prioridad) {
        this.prioridad = prioridad;
    }
    
    public TipoSolicitud getTipoTarea() {
        return tipoTarea;
    }
    
    public void setTipoTarea(TipoSolicitud tipoTarea) {
        this.tipoTarea = tipoTarea;
    }
    
    public String getRuta() {
        return ruta;
    }
    
    public void setRuta(String ruta) {
        this.ruta = ruta;
    }
    
    public String getRutaOrigen() {
        return rutaOrigen;
    }
    
    public void setRutaOrigen(String rutaOrigen) {
        this.rutaOrigen = rutaOrigen;
    }
    
    public String getRutaDestino() {
        return rutaDestino;
    }
    
    public void setRutaDestino(String rutaDestino) {
        this.rutaDestino = rutaDestino;
    }
    
    public String getNombreArchivo() {
        return nombreArchivo;
    }
    
    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }
    
    public Archivo getArchivo() {
        return archivo;
    }
    
    public void setArchivo(Archivo archivo) {
        this.archivo = archivo;
    }
    
    public Usuario getUsuario() {
        return usuario;
    }
    
    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }
}
