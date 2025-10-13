package central;

import java.util.ArrayList;
import java.util.List;

public class NodoArbol {
    private String nombre;
    private long tamano;
    private List<NodoArbol> hijos;

    public NodoArbol() {
        this.hijos = new ArrayList<>();
    }

    public NodoArbol(String nombre, long tamano) {
        this.nombre = nombre;
        this.tamano = tamano;
        this.hijos = new ArrayList<>();
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public long getTamano() { return tamano; }
    public void setTamano(long tamano) { this.tamano = tamano; }
    public List<NodoArbol> getHijos() { return hijos; }
    public void setHijos(List<NodoArbol> hijos) { this.hijos = hijos; }

    public void agregarHijo(NodoArbol hijo) {
        hijos.add(hijo);
    }

    public void eliminarHijo(NodoArbol hijo) {
        hijos.remove(hijo);
    }

    public long calcularTamanoTotal() {
        if (hijos.isEmpty()) {
            return tamano;
        }
        long tamanoTotal = tamano;
        for (NodoArbol hijo : hijos) {
            tamanoTotal += hijo.calcularTamanoTotal();
        }
        return tamanoTotal;
    }

    @Override
    public String toString() {
        return "NodoArbol{" +
                "nombre='" + nombre + '\'' +
                ", tamano=" + tamano +
                ", hijos=" + hijos.size() +
                '}';
    }
}