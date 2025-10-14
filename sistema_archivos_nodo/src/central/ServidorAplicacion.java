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
        
        this.processingPool = Executors.newFixedThreadPool(15);
        this.running = true;
        this.registroNodos = new RegistroNodos();

        for (int i = 0; i < 3; i++) {
            executorService.submit(this::coordinarNodos);
        }
        
        executorService.submit(this::actualizarMetricasNodos);
    }
    
    private void actualizarMetricasNodos() {
        while (running) {
            try {
                Thread.sleep(5000);
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

    public Archivo descargarArchivo(String nombre, String ruta, int idUsuario) {
        System.out.println("Solicitud de descarga para: " + nombre + " en ruta: " + ruta);
        return procesarLeerArchivo(nombre, ruta, idUsuario);
    }

    // // Método sobrecargado para compatibilidad hacia atrás
    // public Archivo descargarArchivo(String nombre, int idUsuario) {
    //     System.out.println("Solicitud de descarga para: " + nombre + " (sin ruta específica)");
    //     return procesarLeerArchivo(nombre, null, idUsuario);
    // }

    public void moverArchivo(String origen, String destino, int idUsuario) {
        Archivo archivoTemp = new Archivo("temp", origen, new byte[0]);
        archivoTemp.setIdUsuario(idUsuario);
        Solicitud solicitud = new Solicitud(TipoSolicitud.MOVER, archivoTemp, new ArrayList<>());
        solicitud.setRutaDestino(destino);
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
                procesarLeerArchivo(solicitud.getArchivo().getNombre(), solicitud.getArchivo().getRuta(), solicitud.getArchivo().getIdUsuario());
                break;
            case MOVER:
                procesarMoverArchivo(solicitud.getArchivo().getRuta(), solicitud.getRutaDestino(), solicitud.getArchivo().getIdUsuario());
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

            InfoNodo nodoPrincipal = registroNodos.obtenerInfoNodoParaTrabajo();
            
            InfoNodo nodoRespaldo = registroNodos.obtenerNodoRespaldo(nodoPrincipal.getNumeroNodo());

            Archivo archivoConRutaCompleta = new Archivo(archivo.getNombre(), rutaCompleta, archivo.getContenido());
            
            Integer numeroNodoRespaldo = (nodoRespaldo != null) ? nodoRespaldo.getNumeroNodo() : null;
            servidorBaseDatos.guardarArchivo(archivoConRutaCompleta, idUsuario, 
                                                   nodoPrincipal.getNumeroNodo(), numeroNodoRespaldo);

            List<InfoNodo> nodosParaAlmacenar = new ArrayList<>();
            nodosParaAlmacenar.add(nodoPrincipal);
            if (nodoRespaldo != null) {
                nodosParaAlmacenar.add(nodoRespaldo);
            }

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

    private Archivo procesarLeerArchivo(String nombre, String ruta, int idUsuario) {
        try {
            Archivo archivoConNodo = null;
            
            // Primero intentar buscar por nombre y ruta específica si se proporciona ruta
            if (ruta != null && !ruta.isEmpty()) {
                archivoConNodo = servidorBaseDatos.buscarArchivoPorNombreYRuta(nombre, ruta, idUsuario);
            }
            
            // Si no se encontró con ruta específica o no se proporcionó ruta, buscar solo por nombre
            if (archivoConNodo == null) {
                List<Archivo> archivosConNodo = servidorBaseDatos.consultarArchivosUsuarioConNodo(idUsuario);
                archivoConNodo = archivosConNodo.stream()
                    .filter(a -> a.getNombre().equals(nombre))
                    .findFirst()
                    .orElse(null);
            }
            
            // Si no se encontró como archivo propio, buscar en archivos compartidos
            if (archivoConNodo == null) {
                archivoConNodo = servidorBaseDatos.buscarArchivoCompartido(nombre, ruta, idUsuario);
            }
            
            if (archivoConNodo == null) {
                throw new Exception("Archivo no encontrado: " + nombre + 
                    (ruta != null && !ruta.isEmpty() ? " en ruta " + ruta : ""));
            }

            List<InfoNodo> nodosParaLectura = new ArrayList<>();
            
            InfoNodo nodoPrincipal = registroNodos.obtenerNodoPorNumero(archivoConNodo.getNodo());
            if (nodoPrincipal != null) {
                nodosParaLectura.add(nodoPrincipal);
            }
            
            if (archivoConNodo.tieneRespaldo()) {
                InfoNodo nodoRespaldo = registroNodos.obtenerNodoPorNumero(archivoConNodo.getNodoRespaldo());
                if (nodoRespaldo != null) {
                    nodosParaLectura.add(nodoRespaldo);
                }
            }

            nodosParaLectura.sort((n1, n2) -> {
                try {
                    n1.asegurarMetricasActualizadas();
                    n2.asegurarMetricasActualizadas();
                    return Integer.compare(n1.getMetricas().getCargaTrabajo(), n2.getMetricas().getCargaTrabajo());
                } catch (Exception e) {
                    return 0;
                }
            });

            for (InfoNodo nodo : nodosParaLectura) {
                try {
                    Archivo archivoLeido = nodo.getInterfazRMI().leerArchivo(archivoConNodo.getNombre(), archivoConNodo.getRuta());
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

    private void procesarMoverArchivo(String origen, String destino, int idUsuario) {
        try {
            String nombreUsuario = servidorBaseDatos.obtenerNombreUsuario(idUsuario);
            String rutaOrigenCompleta = "/" + nombreUsuario + origen;
            String rutaDestinoCompleta = "/" + nombreUsuario + destino;
            
            List<Archivo> archivosConNodo = servidorBaseDatos.consultarArchivosUsuarioConNodo(idUsuario);
            Archivo archivoOrigen = archivosConNodo.stream()
                .filter(a -> (a.getRuta() + "/" + a.getNombre()).equals(rutaOrigenCompleta))
                .findFirst()
                .orElseThrow(() -> new Exception("Archivo no encontrado en la base de datos: " + rutaOrigenCompleta));

            String nombreArchivoDestino = rutaDestinoCompleta.substring(rutaDestinoCompleta.lastIndexOf("/") + 1);
            String rutaDirectorioDestino = rutaDestinoCompleta.substring(0, rutaDestinoCompleta.lastIndexOf("/"));
            
            Archivo archivoExistenteDestino = servidorBaseDatos.buscarArchivoPorNombreYRuta(nombreArchivoDestino, rutaDirectorioDestino, idUsuario);
            
            if (!rutaDirectorioDestino.equals("/" + nombreUsuario)) {
                String nombreDirectorioDestino = rutaDirectorioDestino.substring(rutaDirectorioDestino.lastIndexOf("/") + 1);
                int idDirectorioPadre = servidorBaseDatos.obtenerIdDirectorioUsuario(idUsuario);
                
                int idDirectorioDestino = servidorBaseDatos.obtenerIdDirectorioPorRuta(rutaDirectorioDestino, idUsuario);
                if (idDirectorioDestino == -1) {
                    servidorBaseDatos.guardarDirectorio(nombreDirectorioDestino, destino.substring(0, destino.lastIndexOf("/")), idUsuario, idDirectorioPadre);
                    System.out.println("Directorio de destino creado: " + rutaDirectorioDestino);
                }
            }

            // Obtener nodos donde está almacenado el archivo origen
            InfoNodo nodoPrincipalOrigen = registroNodos.obtenerNodoPorNumero(archivoOrigen.getNodo());
            InfoNodo nodoRespaldoOrigen = null;
            if (archivoOrigen.tieneRespaldo()) {
                nodoRespaldoOrigen = registroNodos.obtenerNodoPorNumero(archivoOrigen.getNodoRespaldo());
            }

            if (archivoExistenteDestino != null) {
                System.out.println("Archivo existente encontrado en destino, será reemplazado: " + rutaDestinoCompleta);
                
                InfoNodo nodoPrincipalDestino = registroNodos.obtenerNodoPorNumero(archivoExistenteDestino.getNodo());
                InfoNodo nodoRespaldoDestino = null;
                if (archivoExistenteDestino.tieneRespaldo()) {
                    nodoRespaldoDestino = registroNodos.obtenerNodoPorNumero(archivoExistenteDestino.getNodoRespaldo());
                }

                List<InfoNodo> nodosParaEliminar = new ArrayList<>();
                if (nodoPrincipalDestino != null) nodosParaEliminar.add(nodoPrincipalDestino);
                if (nodoRespaldoDestino != null) nodosParaEliminar.add(nodoRespaldoDestino);

                nodosParaEliminar.parallelStream().forEach(nodo -> {
                    try {
                        nodo.getInterfazRMI().eliminarArchivo(rutaDestinoCompleta);
                        System.out.println("Archivo existente eliminado en nodo " + nodo.getNumeroNodo() + ": " + rutaDestinoCompleta);
                    } catch (Exception e) {
                        System.err.println("Error eliminando archivo existente en nodo " + nodo.getNumeroNodo() + ": " + e.getMessage());
                    }
                });
                
                int idArchivoExistente = servidorBaseDatos.obtenerIdArchivoPorNombre(nombreArchivoDestino);
                if (idArchivoExistente != -1) {
                    servidorBaseDatos.eliminarArchivo(idArchivoExistente);
                    System.out.println("Archivo existente eliminado de la BD: " + nombreArchivoDestino);
                }
            }

            // Mover el archivo origen a la ubicación de destino
            List<InfoNodo> nodosParaMover = new ArrayList<>();
            if (nodoPrincipalOrigen != null) nodosParaMover.add(nodoPrincipalOrigen);
            if (nodoRespaldoOrigen != null) nodosParaMover.add(nodoRespaldoOrigen);

            nodosParaMover.parallelStream().forEach(nodo -> {
                try {
                    nodo.getInterfazRMI().moverArchivo(rutaOrigenCompleta, rutaDestinoCompleta);
                    System.out.println("Archivo movido en nodo " + nodo.getNumeroNodo() + ": " + rutaOrigenCompleta + " -> " + rutaDestinoCompleta);
                } catch (Exception e) {
                    System.err.println("Error moviendo archivo en nodo " + nodo.getNumeroNodo() + ": " + e.getMessage());
                }
            });

            // Actualizar la base de datos
            int idDirectorioDestino = servidorBaseDatos.obtenerIdDirectorioPorRuta(rutaDirectorioDestino, idUsuario);
            if (idDirectorioDestino == -1) {
                idDirectorioDestino = servidorBaseDatos.obtenerIdDirectorioUsuario(idUsuario);
            }
            
            // Actualizar el archivo origen con la nueva ubicación y nombre
            servidorBaseDatos.actualizarArchivoCompleto(archivoOrigen.getNombre(), nombreArchivoDestino, rutaDirectorioDestino, idDirectorioDestino, idUsuario);
            System.out.println("Archivo movido exitosamente de " + origen + " a " + destino);

        } catch (Exception e) {
            System.err.println("Error al mover el archivo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void procesarEliminarArchivo(String nombre, int idUsuario) {
        try {
            String nombreUsuario = servidorBaseDatos.obtenerNombreUsuario(idUsuario);
            String rutaCompleta = "/" + nombreUsuario + nombre;
            
            List<Archivo> archivosConNodo = servidorBaseDatos.consultarArchivosUsuarioConNodo(idUsuario);
            Archivo archivoConNodo = archivosConNodo.stream()
                .filter(a -> (a.getRuta() + "/" + a.getNombre()).equals(rutaCompleta))
                .findFirst()
                .orElse(null);
            
            if (archivoConNodo != null) {
                
                InfoNodo nodoPrincipal = registroNodos.obtenerNodoPorNumero(archivoConNodo.getNodo());
                InfoNodo nodoRespaldo = null;
                if (archivoConNodo.tieneRespaldo()) {
                    nodoRespaldo = registroNodos.obtenerNodoPorNumero(archivoConNodo.getNodoRespaldo());
                }

                String rutaArchivoCompleta = archivoConNodo.getRuta() + "/" + archivoConNodo.getNombre();

                
                List<InfoNodo> nodosParaEliminar = new ArrayList<>();
                if (nodoPrincipal != null) nodosParaEliminar.add(nodoPrincipal);
                if (nodoRespaldo != null) nodosParaEliminar.add(nodoRespaldo);

                nodosParaEliminar.parallelStream().forEach(nodo -> {
                    try {
                        nodo.getInterfazRMI().eliminarArchivo(rutaArchivoCompleta);
                        System.out.println("Archivo eliminado en nodo " + nodo.getNumeroNodo() + ": " + rutaArchivoCompleta);
                    } catch (Exception e) {
                        System.err.println("Error eliminando archivo en nodo " + nodo.getNumeroNodo() + ": " + e.getMessage());
                    }
                });

                int idArchivo = servidorBaseDatos.obtenerIdArchivoPorNombre(archivoConNodo.getNombre());
                if (idArchivo != -1) {
                    servidorBaseDatos.eliminarArchivo(idArchivo);
                }

                System.out.println("Archivo eliminado exitosamente: " + nombre);
            } else {
                System.out.println("Archivo no encontrado o no pertenece al usuario: " + nombre);
            }
        } catch (Exception e) {
            System.err.println("Error al eliminar el archivo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void procesarCompartirArchivo(Archivo archivo, Usuario usuario) {
        try {
            int idArchivo = servidorBaseDatos.obtenerIdArchivoPorNombre(archivo.getNombre());
            if (idArchivo == -1) {
                throw new Exception("Archivo no encontrado: " + archivo.getNombre());
            }

            Usuario usuarioReceptor = servidorBaseDatos.buscarUsuarioPorEmail(usuario.getEmail());
            if (usuarioReceptor == null) {
                throw new Exception("Usuario receptor no encontrado: " + usuario.getEmail());
            }

            servidorBaseDatos.compartirArchivo(idArchivo, archivo.getIdUsuario(), 
                                              Integer.parseInt(usuarioReceptor.getId()));

            InfoNodo infoNodo = registroNodos.obtenerInfoNodoParaTrabajo();
            infoNodo.getInterfazRMI().compartirArchivo(archivo, usuario);

            System.out.println("Archivo '" + archivo.getNombre() + "' compartido exitosamente con el usuario: " + 
                             usuario.getNombre() + " (" + usuario.getEmail() + ")");

        } catch (Exception e) {
            System.err.println("Error al compartir el archivo: " + e.getMessage());
            e.printStackTrace();
        }
    }
}