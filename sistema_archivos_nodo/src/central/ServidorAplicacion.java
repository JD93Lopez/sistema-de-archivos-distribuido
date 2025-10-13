package central;

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
    private ExecutorService processingPool;
    private volatile boolean running;
    private RegistroNodos registroNodos;

    public ServidorAplicacion() {
        this.colaSolicitudes = new LinkedBlockingQueue<>();
        this.servidorBaseDatos = new ServidorBaseDatos();
        this.executorService = Executors.newFixedThreadPool(3);
        // Pool dedicado para procesamiento paralelo de solicitudes
        this.processingPool = Executors.newFixedThreadPool(15);
        this.running = true;
        this.registroNodos = new RegistroNodos();

        // Iniciar múltiples hilos coordinadores para mejor distribución
        for (int i = 0; i < 3; i++) {
            executorService.submit(this::coordinarNodos);
        }
        
        // Iniciar hilo para actualizar métricas de nodos periódicamente
        executorService.submit(this::actualizarMetricasNodos);
    }
    
    private void actualizarMetricasNodos() {
        while (running) {
            try {
                Thread.sleep(5000); // Actualizar cada 5 segundos
                registroNodos.actualizarNodos();
                System.out.println("Métricas de nodos actualizadas");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Error actualizando métricas: " + e.getMessage());
            }
        }
    }

    public void crearDirectorio(String ruta, int idUsuario) {
        System.out.println("Crear directorio llamado: " + ruta);
        Archivo archivoTemp = new Archivo("directorio", ruta, new byte[0]);
        archivoTemp.setIdUsuario(idUsuario);
        Solicitud solicitud = new Solicitud(TipoSolicitud.CREAR_DIRECTORIO, archivoTemp, new ArrayList<>());
        colaSolicitudes.offer(solicitud);
        System.out.println("Solicitud de crear directorio encolada para: " + ruta);
    }

    public void subirArchivo(Archivo archivo) {
        Solicitud solicitud = new Solicitud(TipoSolicitud.ALMACENAR, archivo, new ArrayList<>());
        colaSolicitudes.offer(solicitud);
        System.out.println("Solicitud de almacenamiento encolada para: " + archivo.getNombre());
    }

    public Archivo descargarArchivo(String nombre, int idUsuario) {
        System.out.println("Solicitud de descarga para: " + nombre);
        return procesarLeerArchivo(nombre, idUsuario);
    }

    public void moverArchivo(String origen, String destino, int idUsuario) {
        Archivo archivoTemp = new Archivo("temp", origen, new byte[0]);
        archivoTemp.setIdUsuario(idUsuario);
        Solicitud solicitud = new Solicitud(TipoSolicitud.MOVER, archivoTemp, new ArrayList<>());
        colaSolicitudes.offer(solicitud);
        System.out.println("Solicitud de mover archivo encolada de " + origen + " a " + destino);
    }

    public void eliminarArchivo(String nombre, int idUsuario) {
        Archivo archivoTemp = new Archivo(nombre, "", new byte[0]);
        archivoTemp.setIdUsuario(idUsuario);
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

    public ArbolEspacio consultarEspacioConsumido(int idUsuario) {
        ArbolEspacio arbolEspacio = new ArbolEspacio(1_000_000_000L);

        String query = "SELECT nombre, ruta, tamano FROM Archivo WHERE Directorio_User_idUser = ?";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, idUsuario);
            ResultSet rs = stmt.executeQuery();

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
                System.out.println("Solicitud recibida: " + solicitud);
                
                // Procesar la solicitud en el pool de procesamiento para no bloquear la coordinación
                processingPool.submit(() -> {
                    try {
                        procesarSolicitudEnParalelo(solicitud);
                    } catch (Exception e) {
                        System.err.println("Error procesando solicitud en paralelo: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Error en coordinador de nodos: " + e.getMessage());
            }
        }
    }

    private void procesarSolicitudEnParalelo(Solicitud solicitud) {
        System.out.println("Procesando solicitud en paralelo: " + solicitud);
        switch (solicitud.getTipo()) {
            case CREAR_DIRECTORIO:
                procesarCrearDirectorio(solicitud.getArchivo().getRuta(), solicitud.getArchivo());
                break;
            case ALMACENAR:
                procesarAlmacenarArchivo(solicitud.getArchivo());
                break;
            case LEER:
                procesarLeerArchivo(solicitud.getArchivo().getNombre(), solicitud.getArchivo().getIdUsuario());
                break;
            case MOVER:
                procesarMoverArchivo(solicitud.getArchivo().getRuta(), "/nuevo/destino");
                break;
            case ELIMINAR:
                procesarEliminarArchivo(solicitud.getArchivo().getNombre(), solicitud.getArchivo().getIdUsuario());
                break;
            case COMPARTIR:
                if (!solicitud.getUsuarios().isEmpty()) {
                    procesarCompartirArchivo(solicitud.getArchivo(), solicitud.getUsuarios().get(0));
                }
                break;
        }
    }

    private void procesarCrearDirectorio(String ruta, Archivo archivo) {
        try {
            System.out.println("Creando directorio en ruta: " + ruta);
            int idUsuario = archivo.getIdUsuario();
            int idDirectorioUsuario = servidorBaseDatos.obtenerIdDirectorioUsuario(idUsuario);
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

            // Crear directorio en todos los nodos activos en paralelo
            List<InfoNodo> todosLosNodos = registroNodos.obtenerTodosLosNodos();
            todosLosNodos.parallelStream().forEach(nodo -> {
                try {
                    nodo.getInterfazRMI().crearDirectorio(rutaCompleta);
                    System.out.println("Directorio creado en nodo " + nodo.getNumeroNodo() + ": " + rutaCompleta);
                } catch (Exception e) {
                    System.err.println("Error al crear directorio en nodo " + nodo.getNumeroNodo() + ": " + e.getMessage());
                }
            });

            servidorBaseDatos.guardarDirectorio(nombreDirectorio, ruta, idUsuario, idDirectorioPadre);
            System.out.println("Directorio creado exitosamente en base de datos: " + rutaCompleta);
        } catch (Exception e) {
            System.err.println("Error al crear directorio: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void procesarAlmacenarArchivo(Archivo archivo) {
        try {
            int idUsuario = archivo.getIdUsuario();
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

            String rutaDirectorio = rutaCompleta;
            String nombreDirectorio = rutaDirectorio.substring(rutaDirectorio.lastIndexOf("/") + 1);
            int idDirectorioPadre = servidorBaseDatos.obtenerIdDirectorioUsuario(idUsuario);
            servidorBaseDatos.guardarDirectorio(nombreDirectorio, rutaDirectorio, idUsuario, idDirectorioPadre);
            System.out.println("Directorio creado para el archivo: " + rutaDirectorio);

            // Usar el método original para obtener el mejor nodo disponible
            InfoNodo nodoPrincipal = registroNodos.obtenerInfoNodoParaTrabajo();
            
            // Obtener nodo de respaldo (diferente al principal)
            InfoNodo nodoRespaldo = registroNodos.obtenerNodoRespaldo(nodoPrincipal.getNumeroNodo());

            // Crear archivo con ruta completa
            Archivo archivoConRutaCompleta = new Archivo(archivo.getNombre(), rutaCompleta, archivo.getContenido());
            
            // Guardar en base de datos con información de nodos
            Integer numeroNodoRespaldo = (nodoRespaldo != null) ? nodoRespaldo.getNumeroNodo() : null;
            servidorBaseDatos.guardarArchivoConNodos(archivoConRutaCompleta, idUsuario, 
                                                   nodoPrincipal.getNumeroNodo(), numeroNodoRespaldo);

            // Almacenar en paralelo en nodo principal y respaldo
            List<InfoNodo> nodosParaAlmacenar = new ArrayList<>();
            nodosParaAlmacenar.add(nodoPrincipal);
            if (nodoRespaldo != null) {
                nodosParaAlmacenar.add(nodoRespaldo);
            }

            // Procesar almacenamiento en paralelo usando streams
            nodosParaAlmacenar.parallelStream().forEach(nodo -> {
                try {
                    nodo.getInterfazRMI().almacenarArchivo(archivoConRutaCompleta);
                    System.out.println("Archivo almacenado en nodo " + nodo.getNumeroNodo() + ": " + archivo.getNombre());
                } catch (Exception e) {
                    System.err.println("Error almacenando en nodo " + nodo.getNumeroNodo() + ": " + e.getMessage());
                }
            });

            System.out.println("Archivo almacenado exitosamente: " + archivo.getNombre() + " en ruta: " + rutaCompleta);
        } catch (Exception e) {
            System.err.println("Error al almacenar el archivo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Archivo procesarLeerArchivo(String nombre, int idUsuario) {
        try {
            // Buscar archivo en base de datos con información de nodos
            List<ArchivoConNodo> archivosConNodo = servidorBaseDatos.consultarArchivosUsuarioConNodo(idUsuario);
            ArchivoConNodo archivoConNodo = archivosConNodo.stream()
                .filter(a -> a.getNombre().equals(nombre))
                .findFirst()
                .orElseThrow(() -> new Exception("Archivo no encontrado en la base de datos"));

            String rutaCompleta = archivoConNodo.getRuta() + "/" + archivoConNodo.getNombre();

            // Crear lista de nodos candidatos para lectura
            List<InfoNodo> nodosParaLectura = new ArrayList<>();
            
            // Agregar nodo principal
            InfoNodo nodoPrincipal = registroNodos.obtenerNodoPorNumero(archivoConNodo.getNumeroNodo());
            if (nodoPrincipal != null) {
                nodosParaLectura.add(nodoPrincipal);
            }
            
            // Agregar nodo de respaldo si existe
            if (archivoConNodo.tieneRespaldo()) {
                InfoNodo nodoRespaldo = registroNodos.obtenerNodoPorNumero(archivoConNodo.getNumeroNodoRespaldo());
                if (nodoRespaldo != null) {
                    nodosParaLectura.add(nodoRespaldo);
                }
            }

            // Ordenar nodos por carga de trabajo (menor carga primero)
            nodosParaLectura.sort((n1, n2) -> {
                try {
                    n1.asegurarMetricasActualizadas();
                    n2.asegurarMetricasActualizadas();
                    return Integer.compare(n1.getMetricas().getCargaTrabajo(), n2.getMetricas().getCargaTrabajo());
                } catch (Exception e) {
                    return 0;
                }
            });

            // Intentar leer desde los nodos disponibles (empezando por el menos cargado)
            for (InfoNodo nodo : nodosParaLectura) {
                try {
                    Archivo archivoLeido = nodo.getInterfazRMI().leerArchivo(rutaCompleta);
                    System.out.println("Archivo descargado exitosamente desde nodo " + 
                                     nodo.getNumeroNodo() + " (carga: " + nodo.getMetricas().getCargaTrabajo() + "): " + nombre);
                    return archivoLeido;
                } catch (Exception e) {
                    System.err.println("Error al leer desde nodo " + nodo.getNumeroNodo() + ": " + e.getMessage());
                }
            }
            
            throw new Exception("No se pudo leer el archivo desde ningún nodo disponible");

        } catch (Exception e) {
            System.err.println("Error al leer el archivo: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private void procesarMoverArchivo(String origen, String destino) {
        try {
            InterfazRMI nodo = registroNodos.obtenerNodoParaTrabajo();
            nodo.moverArchivo(origen, destino);
            System.out.println("Archivo movido exitosamente de " + origen + " a " + destino);
        } catch (Exception e) {
            System.err.println("Error al mover el archivo: " + e.getMessage());
        }
    }

    private void procesarEliminarArchivo(String nombre, int idUsuario) {
        try {
            // Only get files that belong to the specific user
            List<Archivo> archivos = servidorBaseDatos.consultarArchivosUsuario(idUsuario);
            Archivo archivo = archivos.stream()
                .filter(a -> a.getNombre().equals(nombre))
                .findFirst()
                .orElse(null);
            
            if (archivo != null) {
                int idArchivo = servidorBaseDatos.obtenerIdArchivoPorNombre(nombre);
                if (idArchivo != -1) {
                    servidorBaseDatos.eliminarArchivo(idArchivo);
                }

                InterfazRMI nodo = registroNodos.obtenerNodoParaTrabajo();
                nodo.eliminarArchivo(nombre);

                System.out.println("Archivo eliminado exitosamente: " + nombre);
            } else {
                System.out.println("Archivo no encontrado o no pertenece al usuario: " + nombre);
            }
        } catch (Exception e) {
            System.err.println("Error al eliminar el archivo: " + e.getMessage());
        }
    }

    private void procesarCompartirArchivo(Archivo archivo, Usuario usuario) {
        try {
            InterfazRMI nodo = registroNodos.obtenerNodoParaTrabajo();
            nodo.compartirArchivo(archivo, usuario);
            System.out.println("Archivo compartido exitosamente con el usuario: " + usuario.getNombre());
        } catch (Exception e) {
            System.err.println("Error al compartir el archivo: " + e.getMessage());
        }
    }
}