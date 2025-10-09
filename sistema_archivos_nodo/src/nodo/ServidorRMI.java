package nodo;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ServidorRMI {
    public static void main(String[] args) {
        int puertoBase = 1099;
        int puerto = puertoBase;

        while (puerto <= 65535) {
            try {
                Registry registry = LocateRegistry.createRegistry(puerto);
                NodoProcesamiento nodo = new NodoProcesamiento();
                registry.rebind("NodoDistribuido", nodo);
                System.out.println("Nodo distribuido listo en puerto: " + puerto);
                return;
            } catch (Exception e) {
                System.err.println("Puerto " + puerto + " ocupado. Intentando el siguiente...");
                puerto++;
            }
        }
        System.err.println("No se encontró un puerto disponible entre 1099 y 65535.");
    }
}