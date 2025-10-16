package nodo;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class ServidorRMI {
    public static void main(String[] args) {
        try {
            // Configurar propiedades del sistema para RMI distribuido
            System.setProperty("java.net.preferIPv4Stack", "true");
            System.setProperty("java.rmi.server.useCodebaseOnly", "false");
            
            // Obtener la IP real de la máquina (no localhost)
            String hostIP = obtenerIPReal();
            System.setProperty("java.rmi.server.hostname", hostIP);
            
            System.out.println("=== Iniciando Servidor RMI Distribuido ===");
            System.out.println("IP del servidor: " + hostIP);
            System.out.println("Configuración IPv4: " + System.getProperty("java.net.preferIPv4Stack"));
            System.out.println("Hostname configurado: " + System.getProperty("java.rmi.server.hostname"));
            
            int puertoBase = 1099;
            int puerto = puertoBase;

            while (puerto <= 65535) {
                try {
                    System.out.println("Intentando crear registro RMI en puerto: " + puerto);
                    Registry registry = LocateRegistry.createRegistry(puerto);
                    
                    NodoProcesamiento nodo = new NodoProcesamiento();
                    registry.rebind("NodoDistribuido", nodo);
                    
                    System.out.println("✅ Nodo distribuido listo!");
                    System.out.println("   Puerto: " + puerto);
                    System.out.println("   IP: " + hostIP);
                    System.out.println("   Servicio: NodoDistribuido");
                    System.out.println("=======================================");
                    System.out.println("Presiona Ctrl+C para detener el servidor");
                    
                    // Mantener el servidor activo
                    while (true) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            System.out.println("Servidor interrumpido. Cerrando...");
                            break;
                        }
                    }
                    return;
                    
                } catch (Exception e) {
                    if (e.getMessage() != null && e.getMessage().contains("Address already in use")) {
                        System.err.println("Puerto " + puerto + " ocupado. Intentando el siguiente...");
                        puerto++;
                    } else {
                        System.err.println("Error al crear servidor en puerto " + puerto + ": " + e.getMessage());
                        puerto++;
                    }
                }
            }
            System.err.println("❌ No se encontró un puerto disponible entre 1099 y 65535.");
            
        } catch (Exception e) {
            System.err.println("❌ Error general al inicializar el servidor RMI: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static String obtenerIPReal() {
        try {
            // Intentar obtener IP que no sea localhost
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;
                
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr.isSiteLocalAddress() && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
            
            // Fallback a IP local si no se encuentra otra
            return InetAddress.getLocalHost().getHostAddress();
            
        } catch (Exception e) {
            System.err.println("Error obteniendo IP: " + e.getMessage());
            return "localhost";
        }
    }
}