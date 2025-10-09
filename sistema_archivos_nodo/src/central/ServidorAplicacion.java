package central;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;
import java.util.ArrayList;

import nodo.InterfazRMI;

public class ServidorAplicacion {

    private Queue<Solicitud> colaSolicitudes;
    private ServidorBaseDatos servidorBaseDatos;
    private ExecutorService executorService;
    private volatile boolean running;

    public ServidorAplicacion() {
        this.colaSolicitudes = new LinkedBlockingQueue<>();
        this.servidorBaseDatos = new ServidorBaseDatos();
        this.executorService = Executors.newFixedThreadPool(2); 
        this.running = true;
        
        executorService.submit(this::coordinarNodos);
    }

    public void crearDirectorio(String ruta) {
        
        System.out.println("Crear directorio llamado: " + ruta);
        Archivo archivoTemp = new Archivo("directorio", ruta, new byte[0]);
        Solicitud solicitud = new Solicitud(TipoSolicitud.CREAR_DIRECTORIO, archivoTemp, new ArrayList<>());
        colaSolicitudes.offer(solicitud);
        System.out.println("Solicitud de crear directorio encolada para: " + ruta);
    }

    public void subirArchivo(Archivo archivo) {
        
        Solicitud solicitud = new Solicitud(TipoSolicitud.ALMACENAR, archivo, new ArrayList<>());
        colaSolicitudes.offer(solicitud);
        System.out.println("Solicitud de almacenamiento encolada para: " + archivo.getNombre());
    }

    public Archivo descargarArchivo(String nombre) {
        System.out.println("Solicitud de descarga para: " + nombre);
        // Procesar directamente en lugar de encolar
        return procesarLeerArchivo(nombre);
    }

    public void moverArchivo(String origen, String destino) {
        
        Archivo archivoTemp = new Archivo("temp", origen, new byte[0]);
        Solicitud solicitud = new Solicitud(TipoSolicitud.MOVER, archivoTemp, new ArrayList<>());
        colaSolicitudes.offer(solicitud);
        System.out.println("Solicitud de mover archivo encolada de " + origen + " a " + destino);
    }

    public void eliminarArchivo(String nombre) {
        
        Archivo archivoTemp = new Archivo(nombre, "", new byte[0]);
        Solicitud solicitud = new Solicitud(TipoSolicitud.ELIMINAR, archivoTemp, new ArrayList<>());
        colaSolicitudes.offer(solicitud);
        System.out.println("Solicitud de eliminación encolada para: " + nombre);
    }

    public void compartirArchivo(Archivo archivo, Usuario usuario) {
        
        List<Usuario> usuarios = new ArrayList<>();
        usuarios.add(usuario);
        Solicitud solicitud = new Solicitud(TipoSolicitud.COMPARTIR, archivo, usuarios);
        colaSolicitudes.offer(solicitud);
        System.out.println("Solicitud de compartir archivo encolada para: " + archivo.getNombre() + 
                          " con usuario: " + usuario.getNombre());
    }

    public ArbolEspacio consultarEspacioConsumido() {
        ArbolEspacio arbolEspacio = new ArbolEspacio(1_000_000_000L);//hardcodeado 1GB

        String query = "SELECT nombre, ruta, tamano FROM Archivo";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String nombre = rs.getString("nombre");
                String ruta = rs.getString("ruta");
                long tamano = rs.getLong("tamano");
                
                arbolEspacio.agregarArchivo(ruta + "/" + nombre, tamano);
            }
        } catch (SQLException e) {
            System.err.println("Error al consultar el espacio consumido: " + e.getMessage());
            return null;
        }

        
        arbolEspacio.calcularEspacioUsado();
        return arbolEspacio;
    }

    private void coordinarNodos() {
        while (running) {
            try {
                Solicitud solicitud = ((LinkedBlockingQueue<Solicitud>) colaSolicitudes).take();
                
                System.out.println("Procesando solicitud: " + solicitud);
                
                // Procesar la solicitud según su tipo
                switch (solicitud.getTipo()) {
                    case CREAR_DIRECTORIO:
                        procesarCrearDirectorio(solicitud.getArchivo().getRuta());
                        break;
                    case ALMACENAR:
                        procesarAlmacenarArchivo(solicitud.getArchivo());
                        break;
                    case LEER:
                        procesarLeerArchivo(solicitud.getArchivo().getNombre());
                        break;
                    case MOVER:
                        procesarMoverArchivo(solicitud.getArchivo().getRuta(), "/nuevo/destino");
                        break;
                    case ELIMINAR:
                        procesarEliminarArchivo(solicitud.getArchivo().getNombre());
                        break;
                    case COMPARTIR:
                        if (!solicitud.getUsuarios().isEmpty()) {
                            procesarCompartirArchivo(solicitud.getArchivo(), solicitud.getUsuarios().get(0));
                        }
                        break;
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Error procesando solicitud: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void procesarCrearDirectorio(String ruta) {
        try {
            System.out.println("Creando directorio en ruta: " + ruta);
            
            int idUsuario = 1; // Hardcodeado TODO cambiar
            System.out.println("Usuario ID para crear directorio: " + idUsuario);
            
            int idDirectorioUsuario = servidorBaseDatos.obtenerIdDirectorioUsuario(idUsuario);
            System.out.println("Directorio base del usuario ID: " + idDirectorioUsuario);
            
            String[] partesRuta = ruta.split("/");
            String nombreDirectorio = partesRuta[partesRuta.length - 1];
            
            int idDirectorioPadre = idDirectorioUsuario;
            
            if (partesRuta.length > 2) {
                String rutaPadre = ruta.substring(0, ruta.lastIndexOf("/"));
                if (!rutaPadre.isEmpty()) {
                    String nombreUsuario = servidorBaseDatos.obtenerNombreUsuario(idUsuario);
                    String rutaPadreCompleta = "/" + nombreUsuario + rutaPadre;
                    
                    int idPadreExistente = servidorBaseDatos.obtenerIdDirectorioPorRuta(rutaPadreCompleta, idUsuario);
                    if (idPadreExistente != -1) {
                        idDirectorioPadre = idPadreExistente;
                    }
                }
            }
            
            String nombreUsuario = servidorBaseDatos.obtenerNombreUsuario(idUsuario);
            String rutaCompleta = "/" + nombreUsuario + ruta;
            
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            InterfazRMI nodo = (InterfazRMI) registry.lookup("NodoDistribuido");
            nodo.crearDirectorio(rutaCompleta);
            
            servidorBaseDatos.guardarDirectorio(nombreDirectorio, ruta, idUsuario, idDirectorioPadre);
            
            System.out.println("Directorio creado exitosamente en el nodo y base de datos: " + rutaCompleta);
            
        } catch (Exception e) {
            System.err.println("Error al crear directorio: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void procesarAlmacenarArchivo(Archivo archivo) {
        try {
            int idUsuario = 1; // TODO
            
            servidorBaseDatos.obtenerIdDirectorioUsuario(idUsuario);
            
            String nombreUsuario = servidorBaseDatos.obtenerNombreUsuario(idUsuario);
            String rutaOriginal = archivo.getRuta();
            String rutaCompleta;
            
            if (rutaOriginal.startsWith("/")) {
                if (!rutaOriginal.startsWith("/" + nombreUsuario)) {
                    rutaCompleta = "/" + nombreUsuario + rutaOriginal;
                } else {
                    rutaCompleta = rutaOriginal;
                }
            } else {
                rutaCompleta = "/" + nombreUsuario + "/" + rutaOriginal;
            }
            
            Archivo archivoConRutaCompleta = new Archivo(archivo.getNombre(), rutaCompleta, archivo.getContenido());
            
            servidorBaseDatos.guardarArchivo(archivoConRutaCompleta, idUsuario);

            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            InterfazRMI nodo = (InterfazRMI) registry.lookup("NodoDistribuido");
            nodo.almacenarArchivo(archivoConRutaCompleta);

            System.out.println("Archivo almacenado exitosamente: " + archivo.getNombre() + " en ruta: " + rutaCompleta);
        } catch (Exception e) {
            System.err.println("Error al almacenar el archivo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Archivo procesarLeerArchivo(String nombre) {
        try {
            // Buscar el archivo en la base de datos
            List<Archivo> archivos = servidorBaseDatos.consultarArchivosUsuario(1);
            Archivo archivo = archivos.stream()
                .filter(a -> a.getNombre().equals(nombre))
                .findFirst()
                .orElseThrow(() -> new Exception("Archivo no encontrado en la base de datos"));

            // Construir la ruta completa para el nodo
            String rutaCompleta = archivo.getRuta() + "/" + archivo.getNombre();
            
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            InterfazRMI nodo = (InterfazRMI) registry.lookup("NodoDistribuido");
            
            // Leer el archivo desde el nodo
            Archivo archivoLeido = nodo.leerArchivo(rutaCompleta);
            
            System.out.println("Archivo descargado exitosamente: " + archivo.getNombre());
            return archivoLeido;
            
        } catch (Exception e) {
            System.err.println("Error al leer el archivo: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private void procesarMoverArchivo(String origen, String destino) {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            InterfazRMI nodo = (InterfazRMI) registry.lookup("NodoDistribuido");
            nodo.moverArchivo(origen, destino);

            System.out.println("Archivo movido exitosamente de " + origen + " a " + destino);
        } catch (Exception e) {
            System.err.println("Error al mover el archivo: " + e.getMessage());
        }
    }

    private void procesarEliminarArchivo(String nombre) {
        try {
            int idArchivo = servidorBaseDatos.obtenerIdArchivoPorNombre(nombre);
            if (idArchivo != -1) {
                servidorBaseDatos.eliminarArchivo(idArchivo);
            }

            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            InterfazRMI nodo = (InterfazRMI) registry.lookup("NodoDistribuido");
            nodo.eliminarArchivo(nombre);

            System.out.println("Archivo eliminado exitosamente: " + nombre);
        } catch (Exception e) {
            System.err.println("Error al eliminar el archivo: " + e.getMessage());
        }
    }

    private void procesarCompartirArchivo(Archivo archivo, Usuario usuario) {
        try {
            
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            InterfazRMI nodo = (InterfazRMI) registry.lookup("NodoDistribuido");
            nodo.compartirArchivo(archivo, usuario);

            System.out.println("Archivo compartido exitosamente con el usuario: " + usuario.getNombre());
        } catch (Exception e) {
            System.err.println("Error al compartir el archivo: " + e.getMessage());
        }
    }

}