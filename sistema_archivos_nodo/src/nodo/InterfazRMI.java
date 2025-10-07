package nodo;
import java.rmi.Remote;
import java.rmi.RemoteException;
import central.Archivo;
import central.Usuario;

public interface InterfazRMI extends Remote {
    void crearDirectorio(String ruta) throws RemoteException;
    void almacenarArchivo(Archivo archivo) throws RemoteException;
    Archivo leerArchivo(String nombre) throws RemoteException;
    void moverArchivo(String origen, String destino) throws RemoteException;
    void eliminarArchivo(String nombre) throws RemoteException;
    void compartirArchivo(Archivo archivo, Usuario usuario) throws RemoteException;
}