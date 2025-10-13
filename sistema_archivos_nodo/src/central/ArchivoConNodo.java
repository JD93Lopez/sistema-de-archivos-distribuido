package central;

/**
 * Representa un archivo con información de los nodos donde está almacenado
 */
public class ArchivoConNodo {
    private String nombre;
    private String ruta;
    private int numeroNodo;
    private Integer numeroNodoRespaldo; // Puede ser null si no hay respaldo
    
    public ArchivoConNodo(String nombre, String ruta, int numeroNodo, Integer numeroNodoRespaldo) {
        this.nombre = nombre;
        this.ruta = ruta;
        this.numeroNodo = numeroNodo;
        this.numeroNodoRespaldo = numeroNodoRespaldo;
    }
    
    public String getNombre() {
        return nombre;
    }
    
    public String getRuta() {
        return ruta;
    }
    
    public int getNumeroNodo() {
        return numeroNodo;
    }
    
    public Integer getNumeroNodoRespaldo() {
        return numeroNodoRespaldo;
    }
    
    public boolean tieneRespaldo() {
        return numeroNodoRespaldo != null;
    }
    
    @Override
    public String toString() {
        return "ArchivoConNodo{" +
                "nombre='" + nombre + '\'' +
                ", ruta='" + ruta + '\'' +
                ", numeroNodo=" + numeroNodo +
                ", numeroNodoRespaldo=" + numeroNodoRespaldo +
                '}';
    }
}