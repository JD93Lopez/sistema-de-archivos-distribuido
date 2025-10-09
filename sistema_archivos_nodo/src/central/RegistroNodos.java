package central;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import nodo.InterfazRMI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RegistroNodos {
    private static final int PUERTO_BASE = 1099;
    private static final int MAX_NODOS = 100;

    private List<InterfazRMI> nodosActivos = new ArrayList<>();
    private AtomicInteger indiceRoundRobin = new AtomicInteger(0);

    public void actualizarNodos() {
        List<InterfazRMI> nuevosNodos = new ArrayList<>();
        for (int i = 0; i < MAX_NODOS; i++) {
            int puerto = PUERTO_BASE + i;
            try {
                Registry registry = LocateRegistry.getRegistry("localhost", puerto);
                InterfazRMI nodo = (InterfazRMI) registry.lookup("NodoDistribuido");
                nodo.ping();
                nuevosNodos.add(nodo);
                System.out.println("Nodo detectado en puerto: " + puerto);
            } catch (Exception e) {
                break;
            }
        }
        this.nodosActivos = nuevosNodos;
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
}