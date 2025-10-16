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
    private static final String ARCHIVO_CONFIG = "C:\\Users\\juand\\Desktop\\Code\\Distribuidos\\sistema-de-archivos-distribuido\\sistema_archivos_nodo\\nodos.config";
    
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
        
        // Configurar propiedades del sistema para RMI distribuido
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.rmi.server.useCodebaseOnly", "false");
        
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
                
                String host = partes[0].trim();
                int puerto = Integer.parseInt(partes[1].trim());
                
                System.out.println("Intentando conectar a: " + host + ":" + puerto);
                
                // Configurar timeout para conexiones RMI
                System.setProperty("sun.rmi.transport.tcp.responseTimeout", "10000");
                System.setProperty("sun.rmi.transport.tcp.handshakeTimeout", "10000");
                
                Registry registry = LocateRegistry.getRegistry(host, puerto);
                
                // Primero verificar que el registro esté disponible
                String[] servicios = registry.list();
                System.out.println("Servicios disponibles en " + direccion + ": " + java.util.Arrays.toString(servicios));
                
                InterfazRMI nodo = (InterfazRMI) registry.lookup("NodoDistribuido");
                
                // Hacer ping para verificar conectividad
                nodo.ping();
                
                int numeroNodo = i + 1;
                InfoNodo infoNodo = new InfoNodo(numeroNodo, nodo, direccion);
                
                nuevosNodos.add(nodo);
                nuevosInfoNodos.add(infoNodo);
                System.out.println("✓ Nodo " + numeroNodo + " conectado exitosamente: " + direccion);
                
            } catch (java.rmi.ConnectException e) {
                System.err.println("✗ Error de conexión RMI a " + direccion + ": " + e.getMessage());
                System.err.println("  Verificar que el nodo esté ejecutándose y sea accesible desde esta máquina");
            } catch (java.rmi.NotBoundException e) {
                System.err.println("✗ Servicio 'NodoDistribuido' no encontrado en " + direccion);
                System.err.println("  Verificar que el nodo haya registrado el servicio correctamente");
            } catch (NumberFormatException e) {
                System.err.println("✗ Puerto inválido en configuración: " + direccion);
            } catch (Exception e) {
                System.err.println("✗ Error general conectando a " + direccion + ": " + e.getClass().getSimpleName() + " - " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        this.nodosActivos = nuevosNodos;
        this.infoNodos = nuevosInfoNodos;
        this.indiceRoundRobin.set(0);
        
        System.out.println("Resumen de conectividad: " + nuevosNodos.size() + "/" + direccionesNodos.size() + " nodos conectados");
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
        
        return obtenerNodoMasLibre();
    }
    
    private InfoNodo obtenerNodoMasLibre() {
        List<InfoNodo> mejoresNodos = new ArrayList<>();
        double mejorPuntaje = -1;
        
        for (InfoNodo nodo : infoNodos) {
            try {
                double puntaje = nodo.getPuntajeEficiencia();
                System.out.println("Nodo " + nodo.getNumeroNodo() + 
                                 " (" + nodo.getDireccion() + 
                                 ") - Puntaje: " + String.format("%.2f", puntaje) +
                                 " - " + nodo.getMetricas());
                
                if (puntaje > mejorPuntaje) {
                    mejorPuntaje = puntaje;
                    mejoresNodos.clear();
                    mejoresNodos.add(nodo);
                } else if (puntaje == mejorPuntaje) {
                    mejoresNodos.add(nodo);
                }
            } catch (Exception e) {
                System.err.println("Error al evaluar nodo " + nodo.getNumeroNodo() + ": " + e.getMessage());
            }
        }
        
        if (mejoresNodos.isEmpty()) {
            return null;
        }
        
        if (mejoresNodos.size() > 1) {
            int indiceAleatorio = (int) (Math.random() * mejoresNodos.size());
            return mejoresNodos.get(indiceAleatorio);
        }
        
        return mejoresNodos.get(0);
    }

    public InfoNodo obtenerNodoRespaldo(int numeroNodoPrincipal) {
        if (infoNodos.isEmpty()) {
            actualizarNodos();
            if (infoNodos.isEmpty()) {
                throw new RuntimeException("No hay nodos disponibles.");
            }
        }
        
        if (infoNodos.size() <= 1) {
            return null;
        }
        
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