package central;

import nodo.InterfazRMI;

/**
 * Representa la información de un nodo en el sistema distribuido
 */
public class InfoNodo {
    private final int numeroNodo;
    private final InterfazRMI interfazRMI;
    private final String direccion;
    private NodoMetricas metricas;
    
    public InfoNodo(int numeroNodo, InterfazRMI interfazRMI, String direccion) {
        this.numeroNodo = numeroNodo;
        this.interfazRMI = interfazRMI;
        this.direccion = direccion;
        this.metricas = new NodoMetricas(); // Inicializar con métricas vacías
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
    
    public NodoMetricas getMetricas() {
        return metricas;
    }
    
    /**
     * Actualiza las métricas del nodo consultando directamente al nodo remoto
     */
    public void actualizarMetricas() {
        try {
            long espacioDisponible = interfazRMI.obtenerEspacioDisponible();
            int cargaTrabajo = interfazRMI.obtenerCargaTrabajo();
            metricas.actualizarMetricas(espacioDisponible, cargaTrabajo);
        } catch (Exception e) {
            System.err.println("Error al actualizar métricas del nodo " + numeroNodo + ": " + e.getMessage());
            // Las métricas quedan expiradas si no se pueden actualizar
        }
    }
    
    /**
     * Verifica si las métricas están actualizadas, si no las actualiza
     */
    public void asegurarMetricasActualizadas() {
        if (metricas.estanMetricasExpiradas()) {
            actualizarMetricas();
        }
    }
    
    /**
     * Obtiene el puntaje de eficiencia del nodo
     */
    public double getPuntajeEficiencia() {
        asegurarMetricasActualizadas();
        return metricas.calcularPuntajeEficiencia();
    }
    
    @Override
    public String toString() {
        return "InfoNodo{" +
                "numeroNodo=" + numeroNodo +
                ", direccion='" + direccion + '\'' +
                ", metricas=" + metricas +
                '}';
    }
}