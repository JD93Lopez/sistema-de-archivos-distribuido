package central;

public class NodoMetricas {
    private long espacioDisponible;
    private int cargaTrabajo;
    private long timestampActualizacion;
    private static final long TIEMPO_EXPIRACION = 1;

    public NodoMetricas() {
        this.espacioDisponible = 0;
        this.cargaTrabajo = Integer.MAX_VALUE;
        this.timestampActualizacion = 0;
    }

    public NodoMetricas(long espacioDisponible, int cargaTrabajo) {
        this.espacioDisponible = espacioDisponible;
        this.cargaTrabajo = cargaTrabajo;
        this.timestampActualizacion = System.currentTimeMillis();
    }

    public long getEspacioDisponible() {
        return espacioDisponible;
    }

    public void setEspacioDisponible(long espacioDisponible) {
        this.espacioDisponible = espacioDisponible;
        this.timestampActualizacion = System.currentTimeMillis();
    }

    public int getCargaTrabajo() {
        return cargaTrabajo;
    }

    public void setCargaTrabajo(int cargaTrabajo) {
        this.cargaTrabajo = cargaTrabajo;
        this.timestampActualizacion = System.currentTimeMillis();
    }

    public long getTimestampActualizacion() {
        return timestampActualizacion;
    }

    public boolean estanMetricasExpiradas() {
        return (System.currentTimeMillis() - timestampActualizacion) > TIEMPO_EXPIRACION;
    }

    public void actualizarMetricas(long espacioDisponible, int cargaTrabajo) {
        this.espacioDisponible = espacioDisponible;
        this.cargaTrabajo = cargaTrabajo;
        this.timestampActualizacion = System.currentTimeMillis();
    }

    public double calcularPuntajeEficiencia() {
        if (estanMetricasExpiradas()) {
            return 0;
        }

        double puntajeCarga = Math.max(0, 100 - ((cargaTrabajo*100)/5));
        
        double espacioGB = espacioDisponible / (1024.0 * 1024.0 * 1024.0);
        double puntajeEspacio = Math.min(500, espacioGB);
        
        return (puntajeCarga * 0.7) + (puntajeEspacio * 0.3);
    }

    @Override
    public String toString() {
        return String.format("NodoMetricas{espacio=%.2fGB, carga=%d tareas, actualizado=%s}", 
                           espacioDisponible / (1024.0 * 1024.0 * 1024.0), 
                           cargaTrabajo,
                           estanMetricasExpiradas() ? "EXPIRADO" : "OK");
    }
}