package nodo;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ServidorRMI {
    public static void main(String[] args) {
        try {
            NodoProcesamiento nodo = new NodoProcesamiento();

            Registry registry = LocateRegistry.createRegistry(1099);

            registry.rebind("NodoDistribuido", nodo);

            System.out.println("Nodo distribuido listo y esperando solicitudes...");
        } catch (Exception e) {
            System.err.println("Error al iniciar el servidor RMI: " + e.getMessage());
            e.printStackTrace();
        }
    }
}