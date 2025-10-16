package central;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import nodo.InterfazRMI;
import java.util.Arrays;

public class TestConexionNodos {
    public static void main(String[] args) {
        // Configurar propiedades del sistema para RMI distribuido
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.rmi.server.useCodebaseOnly", "false");
        System.setProperty("sun.rmi.transport.tcp.responseTimeout", "10000");
        System.setProperty("sun.rmi.transport.tcp.handshakeTimeout", "10000");
        
        String[] nodos = {"192.168.0.13:1099", "192.168.0.14:1099", "192.168.0.15:1099"};
        
        System.out.println("=== Prueba de Conectividad RMI ===");
        System.out.println("Configuración IPv4: " + System.getProperty("java.net.preferIPv4Stack"));
        System.out.println();
        
        for (String direccion : nodos) {
            System.out.println("🔍 Probando conexión a: " + direccion);
            
            try {
                String[] partes = direccion.split(":");
                String host = partes[0].trim();
                int puerto = Integer.parseInt(partes[1].trim());
                
                // Paso 1: Intentar obtener el registro RMI
                System.out.println("  1. Conectando al registro RMI...");
                Registry registry = LocateRegistry.getRegistry(host, puerto);
                
                // Paso 2: Listar servicios disponibles
                System.out.println("  2. Listando servicios disponibles...");
                String[] nombres = registry.list();
                System.out.println("     Servicios encontrados: " + Arrays.toString(nombres));
                
                if (nombres.length == 0) {
                    System.out.println("  ❌ No hay servicios registrados en el nodo");
                    continue;
                }
                
                // Paso 3: Buscar el servicio NodoDistribuido
                boolean servicioEncontrado = Arrays.asList(nombres).contains("NodoDistribuido");
                if (!servicioEncontrado) {
                    System.out.println("  ❌ Servicio 'NodoDistribuido' no encontrado");
                    System.out.println("     Servicios disponibles: " + Arrays.toString(nombres));
                    continue;
                }
                
                // Paso 4: Obtener referencia al servicio
                System.out.println("  3. Obteniendo referencia al servicio...");
                InterfazRMI nodo = (InterfazRMI) registry.lookup("NodoDistribuido");
                
                // Paso 5: Hacer ping
                System.out.println("  4. Haciendo ping al nodo...");
                nodo.ping();
                
                // Paso 6: Obtener métricas del nodo
                System.out.println("  5. Obteniendo métricas del nodo...");
                long espacioDisponible = nodo.obtenerEspacioDisponible();
                int cargaTrabajo = nodo.obtenerCargaTrabajo();
                
                System.out.println("  ✅ ¡Conexión exitosa!");
                System.out.println("     Espacio disponible: " + (espacioDisponible / (1024 * 1024)) + " MB");
                System.out.println("     Carga de trabajo: " + cargaTrabajo);
                
            } catch (java.rmi.ConnectException e) {
                System.out.println("  ❌ Error de conexión RMI:");
                System.out.println("     " + e.getMessage());
                System.out.println("     Posibles causas:");
                System.out.println("     - El nodo no está ejecutándose");
                System.out.println("     - Firewall bloqueando la conexión");
                System.out.println("     - Configuración incorrecta de java.rmi.server.hostname en el nodo");
                
            } catch (java.rmi.NotBoundException e) {
                System.out.println("  ❌ Servicio no encontrado:");
                System.out.println("     " + e.getMessage());
                System.out.println("     El nodo está ejecutándose pero no registró el servicio 'NodoDistribuido'");
                
            } catch (NumberFormatException e) {
                System.out.println("  ❌ Puerto inválido en configuración: " + direccion);
                
            } catch (Exception e) {
                System.out.println("  ❌ Error general:");
                System.out.println("     " + e.getClass().getSimpleName() + ": " + e.getMessage());
                e.printStackTrace();
            }
            
            System.out.println();
        }
        
        System.out.println("=== Fin de la prueba ===");
    }
}