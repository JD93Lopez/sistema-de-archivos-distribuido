package central;

import java.io.Serializable;

public class Archivo implements Serializable {

    private int id;
    private String nombre;
    private String ruta;
    private byte[] contenido;
    private int idUsuario;
    private int nodo;
    private Integer nodoRespaldo;

    public Archivo() {
    }

    public Archivo(String nombre, String ruta, byte[] contenido) {
        this.nombre = nombre;
        this.ruta = ruta;
        this.contenido = contenido;
    }

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

    public int getNodo() {
        return nodo;
    }

    public void setNodo(int nodo) {
        this.nodo = nodo;
    }

    public Integer getNodoRespaldo() {
        return nodoRespaldo;
    }

    public void setNodoRespaldo(Integer nodoRespaldo) {
        this.nodoRespaldo = nodoRespaldo;
    }

    public boolean tieneRespaldo() {
        return nodoRespaldo != null;
    }

    @Override
    public String toString() {
        StringBuilder stringFile = new StringBuilder();
        stringFile.append("{");
        stringFile.append("id:").append(id).append(",");
        stringFile.append("nombre:").append(nombre != null ? nombre : "").append(",");
        stringFile.append("ruta:").append(ruta != null ? ruta : "").append(",");
        stringFile.append("contenido:").append(contenido != null ? javax.xml.bind.DatatypeConverter.printBase64Binary(contenido) : "").append(",");
        stringFile.append("idUsuario:").append(idUsuario).append(",");
        stringFile.append("nodo:").append(nodo).append(",");
        stringFile.append("nodoRespaldo:").append(nodoRespaldo);
        stringFile.append("}");
        return stringFile.toString();
    }

}