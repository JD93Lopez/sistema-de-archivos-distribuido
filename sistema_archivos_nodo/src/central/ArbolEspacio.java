package central;

public class ArbolEspacio {
    private NodoArbol raiz;
    private long espacioTotal;
    private long espacioUsado;

    public ArbolEspacio() {
        this.raiz = new NodoArbol();
        this.espacioTotal = 0;
        this.espacioUsado = 0;
    }

    public ArbolEspacio(long espacioTotal) {
        this.espacioTotal = espacioTotal;
        this.espacioUsado = 0;
        this.raiz = new NodoArbol("Raíz", 0);
    }

    public NodoArbol getRaiz() { return raiz; }
    public void setRaiz(NodoArbol raiz) { this.raiz = raiz; }
    public long getEspacioTotal() { return espacioTotal; }
    public void setEspacioTotal(long espacioTotal) { this.espacioTotal = espacioTotal; }
    public long getEspacioUsado() { return espacioUsado; }
    public void setEspacioUsado(long espacioUsado) { this.espacioUsado = espacioUsado; }

    public void agregarArchivo(String ruta, long tamano) {
        // Implementación opcional
    }

    public void eliminarArchivo(String ruta) {
        // Implementación opcional
    }

    public long calcularEspacioUsado() {
        return raiz.calcularTamanoTotal();
    }

    @Override
    public String toString() {
        return "ArbolEspacio{" +
                "espacioTotal=" + espacioTotal +
                ", espacioUsado=" + espacioUsado +
                ", raiz=" + raiz +
                '}';
    }
}