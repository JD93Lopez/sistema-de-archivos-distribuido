package nodo;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.Comparator;
import central.Archivo;
import central.Usuario;
import central.TipoSolicitud;
import java.util.concurrent.atomic.AtomicInteger;

public class NodoProcesamiento extends UnicastRemoteObject implements InterfazRMI {

    private String directorioRaiz;
    private ExecutorService poolHilos;
    private PriorityBlockingQueue<Tarea> colaTareas;
    private static final int NUMERO_HILOS = 5;
    private AtomicInteger tareasEjecutandose = new AtomicInteger(0);

    public NodoProcesamiento() throws Exception {
        super();
        this.directorioRaiz = "C:\\Users\\juand\\Desktop\\Code\\Distribuidos\\sistema-de-archivos-distribuido\\sistema_archivos_nodo\\src\\out\\almacenamiento";
        this.poolHilos = Executors.newFixedThreadPool(NUMERO_HILOS);
        this.colaTareas = new PriorityBlockingQueue<>(100, Comparator.comparingInt(Tarea::getPrioridad).reversed());
        
        File rootDir = new File(directorioRaiz);
        if (!rootDir.exists()) {
            rootDir.mkdirs();
        }
        
        iniciarProcesadorTareas();
    }

    private void iniciarProcesadorTareas() {
        for (int i = 0; i < NUMERO_HILOS; i++) {
            poolHilos.submit(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Tarea tarea = colaTareas.take();
                        tareasEjecutandose.incrementAndGet();
                        System.out.println("Procesando tarea: " + tarea.getDescripcion() + " (prioridad: " + tarea.getPrioridad() + ") en hilo: " + Thread.currentThread().getName());
                        System.out.println("Tareas ejecutándose: " + tareasEjecutandose.get() + ", Tareas pendientes: " + colaTareas.size());
                        ejecutarTarea(tarea);
                        System.out.println("Tarea completada: " + tarea.getDescripcion());
                        tareasEjecutandose.decrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        System.err.println("Error al ejecutar tarea: " + e.getMessage());
                        e.printStackTrace();
                        tareasEjecutandose.decrementAndGet();
                    }
                }
            });
        }
    }

    private void ejecutarTarea(Tarea tarea) throws Exception {
        // Thread.sleep(200);
        
        switch (tarea.getTipoTarea()) {
            case CREAR_DIRECTORIO:
                Path rutaCompleta = Paths.get(directorioRaiz, tarea.getRuta());
                Files.createDirectories(rutaCompleta);
                System.out.println("Directorio creado: " + rutaCompleta);
                break;
                
            case ALMACENAR:
                Path rutaArchivoCompleta = Paths.get(directorioRaiz, tarea.getArchivo().getRuta(), tarea.getArchivo().getNombre());
                Files.createDirectories(rutaArchivoCompleta.getParent());
                try (FileOutputStream fos = new FileOutputStream(rutaArchivoCompleta.toFile())) {
                    fos.write(tarea.getArchivo().getContenido());
                }
                System.out.println("Archivo almacenado: " + rutaArchivoCompleta);
                break;
                
            case MOVER:
                Path rutaOrigen = Paths.get(directorioRaiz, tarea.getRutaOrigen());
                Path rutaDestino = Paths.get(directorioRaiz, tarea.getRutaDestino());
                System.out.println("Archivo movido de " + rutaOrigen + " a " + rutaDestino);
                break;
                
            case ELIMINAR:
                Path rutaEliminar = Paths.get(directorioRaiz, tarea.getNombreArchivo());
                System.out.println("Archivo eliminado: " + rutaEliminar);
                break;
                
            case COMPARTIR:
                System.out.println("Compartiendo archivo '" + tarea.getArchivo().getNombre() + "' con el usuario " + tarea.getUsuario().getNombre());
                break;
                
            default:
                System.err.println("Tipo de tarea no reconocido: " + tarea.getTipoTarea());
                break;
        }
    }

    @Override
    public void crearDirectorio(String ruta) throws java.rmi.RemoteException {
        Tarea tarea = new Tarea("Crear directorio: " + ruta, 3);
        tarea.setTipoTarea(TipoSolicitud.CREAR_DIRECTORIO);
        tarea.setRuta(ruta);
        colaTareas.offer(tarea);
        System.out.println("Tarea de crear directorio encolada: " + ruta);
    }

    @Override
    public void almacenarArchivo(Archivo archivo) throws java.rmi.RemoteException {
        Tarea tarea = new Tarea("Almacenar archivo: " + archivo.getNombre(), 2);
        tarea.setTipoTarea(TipoSolicitud.ALMACENAR);
        tarea.setArchivo(archivo);
        colaTareas.offer(tarea);
        System.out.println("Tarea de almacenar archivo encolada: " + archivo.getNombre());
    }

    @Override
    public Archivo leerArchivo(String nombre) throws java.rmi.RemoteException {
        try {
            Path rutaCompleta = Paths.get(directorioRaiz, nombre);
            System.out.println("Leyendo archivo desde: " + rutaCompleta);
            
            if (!Files.exists(rutaCompleta)) {
                throw new java.rmi.RemoteException("El archivo no existe: " + rutaCompleta);
            }
            
            byte[] contenido = Files.readAllBytes(rutaCompleta);
            
            String nombreArchivo = rutaCompleta.getFileName().toString();
            
            String rutaDirectorio = rutaCompleta.getParent().toString().replace(directorioRaiz, "");
            if (rutaDirectorio.startsWith("\\") || rutaDirectorio.startsWith("/")) {
                rutaDirectorio = rutaDirectorio.substring(1);
            }
            
            Archivo archivo = new Archivo(nombreArchivo, rutaDirectorio, contenido);
            System.out.println("Archivo leído exitosamente: " + nombreArchivo + " (" + contenido.length + " bytes)");
            
            return archivo;
        } catch (Exception e) {
            System.err.println("Error al leer el archivo: " + e.getMessage());
            throw new java.rmi.RemoteException("Error al leer el archivo: " + e.getMessage());
        }
    }

    @Override
    public void moverArchivo(String origen, String destino) throws java.rmi.RemoteException {
        System.out.println("Tarea de mover archivo encolada: " + origen + " -> " + destino);
    }

    @Override
    public void eliminarArchivo(String nombre) throws java.rmi.RemoteException {
        System.out.println("Tarea de eliminar archivo encolada: " + nombre);
    }

    @Override
    public void compartirArchivo(Archivo archivo, Usuario usuario) throws java.rmi.RemoteException {
        System.out.println("Tarea de compartir archivo encolada: " + archivo.getNombre());
    }

    public void ejecutarTareaEnHilo(Tarea tarea) throws java.rmi.RemoteException {
        colaTareas.offer(tarea);
        System.out.println("Tarea personalizada encolada: " + tarea.getDescripcion());
    }

    @Override
    public void ping() throws RemoteException {
        // System.out.println("Ping recibido");
    }

    @Override
    public long obtenerEspacioDisponible() throws RemoteException {
        try {
            File directorio = new File(directorioRaiz);
            
            return directorio.getFreeSpace();
        } catch (Exception e) {
            System.err.println("Error al obtener espacio disponible: " + e.getMessage());
            return 0;
        }
    }

    @Override
    public int obtenerCargaTrabajo() throws RemoteException {
        
        int tareasActuales = tareasEjecutandose.get();
        int tareasPendientes = colaTareas.size();
        int cargaTotal = tareasActuales + tareasPendientes;
        
        return cargaTotal;
    }

}