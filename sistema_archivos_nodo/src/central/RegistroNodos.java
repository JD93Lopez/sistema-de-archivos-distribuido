package central;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import nodo.InterfazRMI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class RegistroNodos {
    private static final String ARCHIVO_CONFIG = "C:\\Users\\juand\\Desktop\\Code\\Distribuidos\\sistema-de-archivos-distribuido\\sistema_archivos_nodo\\n" + //
                "odos.config";
    
    private List<InterfazRMI> nodosActivos = new ArrayList<>();
    private List<InfoNodo> infoNodos = new ArrayList<>();
    private List<String> direccionesNodos = new ArrayList<>();
    private AtomicInteger indiceRoundRobin = new AtomicInteger(0);

    public void cargarConfiguracion() {
        direccionesNodos.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader(ARCHIVO_CONFIG))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                linea = linea.trim();

                if (!linea.isEmpty() && !linea.startsWith("#")) {
                    
                    if (linea.contains(":")) {
                        direccionesNodos.add(linea);
                        System.out.println("Configuración cargada: " + linea);
                    } else {
                        System.err.println("Formato inválido en configuración: " + linea);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error al leer archivo de configuración: " + e.getMessage());

            direccionesNodos.add("localhost:1099");
            direccionesNodos.add("localhost:1100");
            direccionesNodos.add("localhost:1101");
            System.out.println("Usando configuración por defecto");
        }
    }

    public void actualizarNodos() {
        if (direccionesNodos.isEmpty()) {
            cargarConfiguracion();
        }
        
        List<InterfazRMI> nuevosNodos = new ArrayList<>();
        List<InfoNodo> nuevosInfoNodos = new ArrayList<>();
        
        for (int i = 0; i < direccionesNodos.size(); i++) {
            String direccion = direccionesNodos.get(i);
            try {
                String[] partes = direccion.split(":");
                if (partes.length != 2) {
                    System.err.println("Formato inválido: " + direccion);
                    continue;
                }
                
                String host = partes[0];
                int puerto = Integer.parseInt(partes[1]);
                
                Registry registry = LocateRegistry.getRegistry(host, puerto);
                InterfazRMI nodo = (InterfazRMI) registry.lookup("NodoDistribuido");
                nodo.ping();
                
                int numeroNodo = i + 1;
                InfoNodo infoNodo = new InfoNodo(numeroNodo, nodo, direccion);
                
                nuevosNodos.add(nodo);
                nuevosInfoNodos.add(infoNodo);
                System.out.println("Nodo " + numeroNodo + " conectado: " + direccion);
            } catch (Exception e) {
                System.err.println("No se pudo conectar al nodo: " + direccion + " - " + e.getMessage());
            }
        }
        this.nodosActivos = nuevosNodos;
        this.infoNodos = nuevosInfoNodos;
        this.indiceRoundRobin.set(0);
    }

    public InterfazRMI obtenerNodoParaTrabajo() {
        if (nodosActivos.isEmpty()) {
            actualizarNodos();
            if (nodosActivos.isEmpty()) {
                throw new RuntimeException("No hay nodos disponibles.");
            }
        }
        int indice = indiceRoundRobin.getAndIncrement() % nodosActivos.size();
        return nodosActivos.get(indice);
    }

    public void recargarConfiguracion() {
        System.out.println("Recargando configuración de nodos...");
        cargarConfiguracion();
        actualizarNodos();
    }

    public int obtenerNumeroNodosActivos() {
        return nodosActivos.size();
    }

    public int obtenerNumeroNodosConfigurados() {
        return direccionesNodos.size();
    }

    public InfoNodo obtenerInfoNodoParaTrabajo() {
        if (infoNodos.isEmpty()) {
            actualizarNodos();
            if (infoNodos.isEmpty()) {
                throw new RuntimeException("No hay nodos disponibles.");
            }
        }
        int indice = indiceRoundRobin.getAndIncrement() % infoNodos.size();
        return infoNodos.get(indice);
    }

    public InfoNodo obtenerNodoRespaldo(int numeroNodoPrincipal) {
        if (infoNodos.isEmpty()) {
            actualizarNodos();
            if (infoNodos.isEmpty()) {
                throw new RuntimeException("No hay nodos disponibles.");
            }
        }
        
        // Si solo hay un nodo, retorna null (no hay respaldo)
        if (infoNodos.size() <= 1) {
            return null;
        }
        
        // Busca un nodo diferente al principal
        for (InfoNodo info : infoNodos) {
            if (info.getNumeroNodo() != numeroNodoPrincipal) {
                return info;
            }
        }
        
        return null;
    }

    public InfoNodo obtenerNodoPorNumero(int numeroNodo) {
        if (infoNodos.isEmpty()) {
            actualizarNodos();
        }
        
        return infoNodos.stream()
                .filter(info -> info.getNumeroNodo() == numeroNodo)
                .findFirst()
                .orElse(null);
    }

    public List<InfoNodo> obtenerTodosLosNodos() {
        if (infoNodos.isEmpty()) {
            actualizarNodos();
        }
        return new ArrayList<>(infoNodos);
    }
}