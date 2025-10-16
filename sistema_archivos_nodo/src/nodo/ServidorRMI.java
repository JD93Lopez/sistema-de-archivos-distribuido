package nodo;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class ServidorRMI {
    
    private static String obtenerIPLocal() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                
                if (iface.isLoopback() || !iface.isUp()) {
                    continue;
                }
                
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    
                    if (!addr.isLoopbackAddress() && addr.getHostAddress().indexOf(':') == -1) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error obteniendo IP local: " + e.getMessage());
        }
        return "127.0.0.1";
    }
    
    public static void main(String[] args) {
        String ipLocal = obtenerIPLocal();
        System.out.println("IP local detectada: " + ipLocal);
        
        System.setProperty("java.rmi.server.hostname", ipLocal);
        
        int puertoBase = 1099;
        int puerto = puertoBase;

        while (puerto <= 65535) {
            try {
                Registry registry = LocateRegistry.createRegistry(puerto);
                NodoProcesamiento nodo = new NodoProcesamiento();
                registry.rebind("NodoDistribuido", nodo);
                System.out.println("Nodo en " + ipLocal + ":" + puerto);
                System.out.println(ipLocal + ":" + puerto + "/NodoDistribuido");
                return;
            } catch (Exception e) {
                System.err.println("Puerto " + puerto + " ocupado. Intentando el siguiente...");
                puerto++;
            }
        }
        System.err.println("No se encontró un puerto disponible entre 1099 y 65535.");
    }
}