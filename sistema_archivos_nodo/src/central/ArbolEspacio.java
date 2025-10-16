package central;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        this.raiz = new NodoArbol("Sistema de Archivos", 0);
    }

    public NodoArbol getRaiz() { return raiz; }
    public void setRaiz(NodoArbol raiz) { this.raiz = raiz; }
    public long getEspacioTotal() { return espacioTotal; }
    public void setEspacioTotal(long espacioTotal) { this.espacioTotal = espacioTotal; }
    public long getEspacioUsado() { return espacioUsado; }
    public void setEspacioUsado(long espacioUsado) { this.espacioUsado = espacioUsado; }

    public void agregarArchivo(String ruta, long tamano) {
    }

    public void eliminarArchivo(String ruta) {
    }

    public void construirArbolDesdeEstructura(List<Object[]> elementos) {
        Map<String, NodoArbol> nodosMap = new HashMap<>();
        
        nodosMap.put("/", raiz);
        
        for (Object[] elemento : elementos) {
            String tipo = (String) elemento[0];
            String nombre = (String) elemento[1];
            String ruta = (String) elemento[2];
            long tamano = (Long) elemento[3];
            
            if (!ruta.startsWith("/")) {
                ruta = "/" + ruta;
            }
            if (!ruta.endsWith("/") && !tipo.equals("archivo")) {
                ruta = ruta + "/";
            }
            
            String rutaCompleta;
            if (tipo.equals("archivo")) {
                rutaCompleta = ruta.endsWith("/") ? ruta + nombre : ruta + "/" + nombre;
            } else {
                rutaCompleta = ruta;
            }
            
            crearDirectoriosPadre(rutaCompleta, nodosMap);
            
            NodoArbol nodoActual;
            if (tipo.equals("archivo")) {
                nodoActual = new NodoArbol(nombre + " (" + formatearTamano(tamano) + ")", tamano);
            } else {
                nodoActual = new NodoArbol(nombre + "/", 0);
            }
            
            String rutaPadre = obtenerRutaPadre(rutaCompleta);
            NodoArbol nodoPadre = nodosMap.get(rutaPadre);
            
            if (nodoPadre != null) {
                nodoPadre.agregarHijo(nodoActual);
                nodosMap.put(rutaCompleta, nodoActual);
            } else {
                raiz.agregarHijo(nodoActual);
                nodosMap.put(rutaCompleta, nodoActual);
            }
        }
    }
    
    private void crearDirectoriosPadre(String rutaCompleta, Map<String, NodoArbol> nodosMap) {
        String[] partes = rutaCompleta.split("/");
        String rutaAcumulada = "";
        
        for (int i = 0; i < partes.length - 1; i++) {
            if (!partes[i].isEmpty()) {
                rutaAcumulada += "/" + partes[i];
                
                if (!nodosMap.containsKey(rutaAcumulada + "/")) {
                    NodoArbol nuevoDirectorio = new NodoArbol(partes[i] + "/", 0);
                    String rutaPadre = obtenerRutaPadre(rutaAcumulada + "/");
                    NodoArbol nodoPadre = nodosMap.getOrDefault(rutaPadre, raiz);
                    
                    nodoPadre.agregarHijo(nuevoDirectorio);
                    nodosMap.put(rutaAcumulada + "/", nuevoDirectorio);
                }
            }
        }
    }
    
    private String obtenerRutaPadre(String ruta) {
        if (ruta.equals("/") || ruta.isEmpty()) {
            return "/";
        }
        
        if (ruta.endsWith("/")) {
            ruta = ruta.substring(0, ruta.length() - 1);
        }
        
        int ultimaBarra = ruta.lastIndexOf("/");
        if (ultimaBarra <= 0) {
            return "/";
        }
        
        return ruta.substring(0, ultimaBarra + 1);
    }
    
    private String formatearTamano(long tamano) {
        if (tamano < 1024) {
            return tamano + " B";
        } else if (tamano < 1024 * 1024) {
            return String.format("%.1f KB", tamano / 1024.0);
        } else if (tamano < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", tamano / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", tamano / (1024.0 * 1024.0 * 1024.0));
        }
    }

    public long calcularEspacioUsado() {
        this.espacioUsado = raiz.calcularTamanoTotal();
        return this.espacioUsado;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== REPORTE DE ESPACIO CONSUMIDO ===\n");
        sb.append("Espacio Total: ").append(formatearTamano(espacioTotal)).append("\n");
        sb.append("Espacio Usado: ").append(formatearTamano(calcularEspacioUsado())).append("\n");
        sb.append("Espacio Libre: ").append(formatearTamano(espacioTotal - espacioUsado)).append("\n");
        sb.append("Porcentaje Usado: ").append(String.format("%.2f", (espacioUsado * 100.0) / espacioTotal)).append("%\n\n");
        sb.append("=== ESTRUCTURA DE ARCHIVOS ===\n");
        
        if (raiz != null) {
            construirArbolString(raiz, sb, "", true);
        }
        
        return sb.toString();
    }
    
    private void construirArbolString(NodoArbol nodo, StringBuilder sb, String prefijo, boolean esUltimo) {
        if (nodo == null) return;
        
        String simbolo = esUltimo ? "|__ " : "|-- ";
        sb.append(prefijo).append(simbolo).append(nodo.getNombre()).append("\n");
        
        // Nuevo prefijo para los hijos
        String nuevoPrefijo = prefijo + (esUltimo ? "    " : "|   ");

        List<NodoArbol> hijos = nodo.getHijos();
        for (int i = 0; i < hijos.size(); i++) {
            boolean esUltimoHijo = (i == hijos.size() - 1);
            construirArbolString(hijos.get(i), sb, nuevoPrefijo, esUltimoHijo);
        }
    }
}