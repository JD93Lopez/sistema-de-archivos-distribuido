package central;

import nodo.InterfazRMI;

/**
 * Representa la información de un nodo en el sistema distribuido
 */
public class InfoNodo {
    private final int numeroNodo;
    private final InterfazRMI interfazRMI;
    private final String direccion;
    
    public InfoNodo(int numeroNodo, InterfazRMI interfazRMI, String direccion) {
        this.numeroNodo = numeroNodo;
        this.interfazRMI = interfazRMI;
        this.direccion = direccion;
    }
    
    public int getNumeroNodo() {
        return numeroNodo;
    }
    
    public InterfazRMI getInterfazRMI() {
        return interfazRMI;
    }
    
    public String getDireccion() {
        return direccion;
    }
    
    @Override
    public String toString() {
        return "InfoNodo{" +
                "numeroNodo=" + numeroNodo +
                ", direccion='" + direccion + '\'' +
                '}';
    }
}