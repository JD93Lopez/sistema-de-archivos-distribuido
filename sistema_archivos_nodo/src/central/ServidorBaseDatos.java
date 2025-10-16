package central;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ServidorBaseDatos {

    public void guardarArchivo(Archivo archivo, int idUsuario, int numeroNodo, Integer numeroNodoRespaldo) throws SQLException {
        // Primero verificar si el archivo ya existe
        String queryVerificar = "SELECT idFile FROM Archivo WHERE nombre = ? AND ruta = ? AND Directorio_User_idUser = ?";
        
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement stmtVerificar = conn.prepareStatement(queryVerificar)) {
            
            stmtVerificar.setString(1, archivo.getNombre());
            stmtVerificar.setString(2, archivo.getRuta());
            stmtVerificar.setInt(3, idUsuario);
            
            ResultSet rs = stmtVerificar.executeQuery();
            
            if (rs.next()) {
                // El archivo ya existe, actualizarlo
                int idArchivo = rs.getInt("idFile");
                actualizarArchivo(idArchivo, archivo, numeroNodo, numeroNodoRespaldo);
                System.out.println("Archivo actualizado en la base de datos: " + archivo.getNombre() + 
                                 " en nodo " + numeroNodo + 
                                 (numeroNodoRespaldo != null ? " con respaldo en nodo " + numeroNodoRespaldo : ""));
            } else {
                // El archivo no existe, crearlo
                String query = "INSERT INTO Archivo (nombre, ruta, tamano, fecha_crea, nodo, nodo_respaldo, Directorio_idDirectorio, Directorio_User_idUser) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    
                    String rutaDirectorio = archivo.getRuta();
                    int idDirectorio = obtenerIdDirectorioPorRuta(rutaDirectorio, idUsuario);
                    
                    if (idDirectorio == -1) {
                        idDirectorio = obtenerIdDirectorioRaiz(idUsuario);
                    }
                        
                    stmt.setString(1, archivo.getNombre());
                    stmt.setString(2, archivo.getRuta());
                    stmt.setLong(3, archivo.getContenido().length);
                    stmt.setDate(4, new java.sql.Date(System.currentTimeMillis()));
                    stmt.setInt(5, numeroNodo);
                    if (numeroNodoRespaldo != null) {
                        stmt.setInt(6, numeroNodoRespaldo);
                    } else {
                        stmt.setNull(6, java.sql.Types.INTEGER);
                    }
                    stmt.setInt(7, idDirectorio);
                    stmt.setInt(8, idUsuario);

                    stmt.executeUpdate();
                    System.out.println("Archivo guardado en la base de datos: " + archivo.getNombre() + 
                                     " en nodo " + numeroNodo + 
                                     (numeroNodoRespaldo != null ? " con respaldo en nodo " + numeroNodoRespaldo : ""));
                }
            }
        }
    }

    private void actualizarArchivo(int idArchivo, Archivo archivo, int numeroNodo, Integer numeroNodoRespaldo) throws SQLException {
        String query = "UPDATE Archivo SET tamano = ?, fecha_crea = ?, nodo = ?, nodo_respaldo = ? WHERE idFile = ?";
        
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setLong(1, archivo.getContenido().length);
            stmt.setDate(2, new java.sql.Date(System.currentTimeMillis()));
            stmt.setInt(3, numeroNodo);
            if (numeroNodoRespaldo != null) {
                stmt.setInt(4, numeroNodoRespaldo);
            } else {
                stmt.setNull(4, java.sql.Types.INTEGER);
            }
            stmt.setInt(5, idArchivo);
            
            stmt.executeUpdate();
        }
    }

    public void eliminarArchivo(int idArchivo) throws SQLException {
        try (Connection conn = ConexionDB.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                String queryEliminarCompartir = "DELETE FROM Compartir WHERE Archivo_idFile = ?";
                try (PreparedStatement stmtCompartir = conn.prepareStatement(queryEliminarCompartir)) {
                    stmtCompartir.setInt(1, idArchivo);
                    int filasCompartirEliminadas = stmtCompartir.executeUpdate();
                    if (filasCompartirEliminadas > 0) {
                        System.out.println("Eliminados " + filasCompartirEliminadas + " registros de compartir para archivo ID: " + idArchivo);
                    }
                }
                
                String queryEliminarArchivo = "DELETE FROM Archivo WHERE idFile = ?";
                try (PreparedStatement stmtArchivo = conn.prepareStatement(queryEliminarArchivo)) {
                    stmtArchivo.setInt(1, idArchivo);
                    int filasArchivoEliminadas = stmtArchivo.executeUpdate();
                    if (filasArchivoEliminadas > 0) {
                        System.out.println("Archivo eliminado de la base de datos: " + idArchivo);
                    } else {
                        System.err.println("No se encontró el archivo con ID: " + idArchivo);
                    }
                }
                
                conn.commit();
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public List<Archivo> consultarArchivosUsuario(int idUsuario) throws SQLException {
        String query = "SELECT * FROM Archivo WHERE Directorio_User_idUser = ?";
        List<Archivo> archivos = new ArrayList<>();

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, idUsuario);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
            Archivo archivo = new Archivo(
                rs.getString("nombre"),
                rs.getString("ruta"),
                null 
            );
            
            archivos.add(archivo);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return archivos;
    }

    public List<Archivo> consultarArchivosUsuarioConNodo(int idUsuario) throws SQLException {
        String query = "SELECT nombre, ruta, nodo, nodo_respaldo FROM Archivo WHERE Directorio_User_idUser = ?";
        List<Archivo> archivos = new ArrayList<>();

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, idUsuario);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Archivo archivo = new Archivo();
                archivo.setNombre(rs.getString("nombre"));
                archivo.setRuta(rs.getString("ruta"));
                archivo.setNodo(rs.getInt("nodo"));
                archivo.setNodoRespaldo(rs.getObject("nodo_respaldo") != null ? rs.getInt("nodo_respaldo") : null);
                archivos.add(archivo);
            }
        }

        return archivos;
    }

    public ArbolEspacio consultarEstadoEspacio() throws SQLException {
        //TODO
        return new ArbolEspacio();
    }

    public int obtenerIdDirectorioPorRuta(String ruta, int idUsuario) throws SQLException {
        String query = "SELECT idDirectorio FROM Directorio WHERE ruta = ? AND User_idUser = ?";
        
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, ruta);
            stmt.setInt(2, idUsuario);
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("idDirectorio");
            }
        }
        return -1;
    }
    
    public int obtenerIdArchivoPorNombre(String nombre) throws SQLException {
        String query = "SELECT idFile FROM Archivo WHERE nombre = ?";
        
        try (Connection conn = ConexionDB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, nombre);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("idFile");
            }
        }
        return -1;
    }

    private int crearDirectorioRaiz(int idUsuario) throws SQLException {
        String query = "INSERT INTO Directorio (nombre, ruta, User_idUser, idPadre) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = ConexionDB.getConnection();
            PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, "root");
            stmt.setString(2, "/");
            stmt.setInt(3, idUsuario);
            stmt.setNull(4, java.sql.Types.INTEGER);
            
            stmt.executeUpdate();
            
            ResultSet generatedKeys = stmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                int idDirectorioRaiz = generatedKeys.getInt(1);
                System.out.println("Directorio raíz creado para usuario: " + idUsuario + " con ID: " + idDirectorioRaiz);
                return idDirectorioRaiz;
            }
        }
        throw new SQLException("No se pudo crear el directorio raíz");
    }

    private int crearDirectorioUsuario(int idUsuario, String nombreUsuario, int idDirectorioRaiz) throws SQLException {
        String query = "INSERT INTO Directorio (nombre, ruta, User_idUser, idPadre) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = ConexionDB.getConnection();
            PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, nombreUsuario);
            stmt.setString(2, "/" + nombreUsuario);
            stmt.setInt(3, idUsuario);
            stmt.setInt(4, idDirectorioRaiz);
            
            stmt.executeUpdate();
            
            ResultSet generatedKeys = stmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                int idDirectorioUsuario = generatedKeys.getInt(1);
                System.out.println("Directorio de usuario creado: " + nombreUsuario + " con ID: " + idDirectorioUsuario);
                return idDirectorioUsuario;
            }
        }
        throw new SQLException("No se pudo crear el directorio del usuario");
    }

    public String obtenerNombreUsuario(int idUsuario) throws SQLException {
        String query = "SELECT nombre FROM Usuario WHERE idUsuario = ?";
        
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, idUsuario);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getString("nombre");
            }
        }
        throw new SQLException("Usuario no encontrado con ID: " + idUsuario);
    }

    public int obtenerIdDirectorioUsuario(int idUsuario) throws SQLException {
        
        int idDirectorioRaiz = obtenerIdDirectorioRaiz(idUsuario);
        
        
        String nombreUsuario = obtenerNombreUsuario(idUsuario);
        
        
        String query = "SELECT idDirectorio FROM Directorio WHERE User_idUser = ? AND nombre = ? AND idPadre = ?";
        
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, idUsuario);
            stmt.setString(2, nombreUsuario);
            stmt.setInt(3, idDirectorioRaiz);
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("idDirectorio");
            } else {
                return crearDirectorioUsuario(idUsuario, nombreUsuario, idDirectorioRaiz);
            }
        }
    }

    public int obtenerIdDirectorioRaiz(int idUsuario) throws SQLException {
        String query = "SELECT idDirectorio FROM Directorio WHERE idPadre IS NULL LIMIT 1";
        
        try (Connection conn = ConexionDB.getConnection();
            PreparedStatement stmt = conn.prepareStatement(query)) {
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("idDirectorio");
            } else {
                return crearDirectorioRaiz(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void guardarDirectorio(String nombre, String ruta, int idUsuario, int idPadre) throws SQLException {
        
        String rutaCompleta;
        if (ruta.endsWith("/")) {
            rutaCompleta = ruta + nombre;
        } else if (ruta.isEmpty() || ruta.equals("/")) {
            rutaCompleta = nombre;
        } else {
            rutaCompleta = ruta + "/" + nombre;
        }
        crearJerarquiaDirectorios(rutaCompleta, idUsuario);
    }
    
    private void crearJerarquiaDirectorios(String rutaCompleta, int idUsuario) throws SQLException {
        String nombreUsuario = obtenerNombreUsuario(idUsuario);
        
        String rutaNormalizada;
        if (rutaCompleta.startsWith("/")) {
            if (!rutaCompleta.startsWith("/" + nombreUsuario)) {
                rutaNormalizada = "/" + nombreUsuario + rutaCompleta;
            } else {
                rutaNormalizada = rutaCompleta;
            }
        } else {
            rutaNormalizada = "/" + nombreUsuario + "/" + rutaCompleta;
        }
        
        String[] partes = rutaNormalizada.split("/");
        
        int idPadreActual = obtenerIdDirectorioUsuario(idUsuario);
        String rutaAcumulada = "/" + nombreUsuario;
        
        for (int i = 2; i < partes.length-1; i++) {
            String nombreDirectorio = partes[i];
            if (!nombreDirectorio.isEmpty()) {
                rutaAcumulada += "/" + nombreDirectorio;
                
                int idDirectorioExistente = obtenerIdDirectorioPorRuta(rutaAcumulada, idUsuario);
                
                if (idDirectorioExistente == -1) {
                    
                    idPadreActual = crearDirectorioEnJerarquia(nombreDirectorio, rutaAcumulada, idUsuario, idPadreActual);
                    // System.out.println("Directorio creado: " + nombreDirectorio + " en ruta: " + rutaAcumulada + " con padre ID: " + idPadreActual);
                } else {
                    
                    idPadreActual = idDirectorioExistente;
                    System.out.println("Directorio ya existe: " + nombreDirectorio + " en ruta: " + rutaAcumulada + " ID: " + idDirectorioExistente);
                }
            }
        }
    }
    
    private int crearDirectorioEnJerarquia(String nombre, String ruta, int idUsuario, int idPadre) throws SQLException {
        String query = "INSERT INTO Directorio (nombre, ruta, User_idUser, idPadre) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, nombre);
            stmt.setString(2, ruta);
            stmt.setInt(3, idUsuario);
            stmt.setInt(4, idPadre);
            
            stmt.executeUpdate();
            
            ResultSet generatedKeys = stmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                return generatedKeys.getInt(1);
            }
        }
        throw new SQLException("No se pudo crear el directorio: " + nombre);
    }

    public int registrarUsuario(Usuario usuario) throws SQLException {
        String query = "INSERT INTO Usuario (nombre, correo, contrasena) VALUES (?, ?, ?)";
        
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, usuario.getNombre());
            stmt.setString(2, usuario.getEmail());
            stmt.setString(3, usuario.getContrasena());
            
            stmt.executeUpdate();
            
            ResultSet generatedKeys = stmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                int userId = generatedKeys.getInt(1);
                System.out.println("Usuario registrado exitosamente con ID: " + userId);
                return userId;
            }
        }
        throw new SQLException("No se pudo registrar el usuario");
    }

    public Usuario buscarUsuarioPorEmail(String email) throws SQLException {
        String query = "SELECT idUsuario, nombre, correo, contrasena FROM Usuario WHERE correo = ?";
        
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return new Usuario(
                    String.valueOf(rs.getInt("idUsuario")),
                    rs.getString("nombre"),
                    rs.getString("correo"),
                    rs.getString("contrasena"),
                    null
                );
            }
        }
        return null;
    }

    public boolean existeUsuarioPorEmail(String email) throws SQLException {
        String query = "SELECT COUNT(*) FROM Usuario WHERE correo = ?";
        
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }
    
    public Usuario buscarUsuarioPorId(int userId) throws SQLException {
        String query = "SELECT idUsuario, nombre, correo FROM Usuario WHERE idUsuario = ?";
        
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return new Usuario(
                    String.valueOf(rs.getInt("idUsuario")),
                    rs.getString("nombre"),
                    rs.getString("correo"),
                    null,
                    null
                );
            }
        }
        return null;
    }

    public void actualizarRutaArchivo(String nombreArchivo, String nuevaRuta, int idUsuario) throws SQLException {
        String query = "UPDATE Archivo SET ruta = ? WHERE nombre = ? AND Directorio_User_idUser = ?";
        
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, nuevaRuta);
            stmt.setString(2, nombreArchivo);
            stmt.setInt(3, idUsuario);
            
            int filasAfectadas = stmt.executeUpdate();
            if (filasAfectadas > 0) {
                System.out.println("Ruta del archivo actualizada en BD: " + nombreArchivo + " -> " + nuevaRuta);
            } else {
                System.err.println("No se pudo actualizar la ruta del archivo: " + nombreArchivo);
            }
        }
    }

    public void actualizarRutaYDirectorioArchivo(String nombreArchivo, String nuevaRuta, int idDirectorioDestino, int idUsuario) throws SQLException {
        String query = "UPDATE Archivo SET ruta = ?, Directorio_idDirectorio = ? WHERE nombre = ? AND Directorio_User_idUser = ?";
        
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, nuevaRuta);
            stmt.setInt(2, idDirectorioDestino);
            stmt.setString(3, nombreArchivo);
            stmt.setInt(4, idUsuario);
            
            int filasAfectadas = stmt.executeUpdate();
            if (filasAfectadas > 0) {
                System.out.println("Ruta y directorio del archivo actualizados en BD: " + nombreArchivo + " -> " + nuevaRuta + " (DIR ID: " + idDirectorioDestino + ")");
            } else {
                System.err.println("No se pudo actualizar la ruta y directorio del archivo: " + nombreArchivo);
            }
        }
    }

    public void actualizarArchivoCompleto(String nombreArchivoOrigen, String nuevoNombre, String nuevaRuta, int idDirectorioDestino, int idUsuario) throws SQLException {
        String query = "UPDATE Archivo SET nombre = ?, ruta = ?, Directorio_idDirectorio = ? WHERE nombre = ? AND Directorio_User_idUser = ?";
        
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, nuevoNombre);
            stmt.setString(2, nuevaRuta);
            stmt.setInt(3, idDirectorioDestino);
            stmt.setString(4, nombreArchivoOrigen);
            stmt.setInt(5, idUsuario);
            
            int filasAfectadas = stmt.executeUpdate();
            if (filasAfectadas > 0) {
                System.out.println("Archivo actualizado completamente en BD: " + nombreArchivoOrigen + " -> " + nuevoNombre + " en " + nuevaRuta + " (DIR ID: " + idDirectorioDestino + ")");
            } else {
                System.err.println("No se pudo actualizar completamente el archivo: " + nombreArchivoOrigen);
            }
        }
    }

    public int obtenerIdArchivoPorNombreYRuta(String nombre, String ruta, int idUsuario) throws SQLException {
        String query = "SELECT idFile FROM Archivo WHERE nombre = ? AND ruta = ? AND Directorio_User_idUser = ?";
        
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, nombre);
            stmt.setString(2, ruta);
            stmt.setInt(3, idUsuario);
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("idFile");
            }
        }
        return -1;
    }

    public void compartirArchivo(int idArchivo, int idPropietario, int idReceptor) throws SQLException {
        String query = "INSERT INTO Compartir (Archivo_idFile, Propietario, Receptor) VALUES (?, ?, ?)";
        
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, idArchivo);
            stmt.setInt(2, idPropietario);
            stmt.setInt(3, idReceptor);
            
            int filasAfectadas = stmt.executeUpdate();
            if (filasAfectadas > 0) {
                System.out.println("Archivo compartido registrado en BD: archivo ID " + idArchivo + 
                                 " de usuario " + idPropietario + " a usuario " + idReceptor);
            } else {
                System.err.println("No se pudo registrar el compartir del archivo");
            }
        }
    }

    public Archivo buscarArchivoPorNombreYRuta(String nombre, String ruta, int idUsuario) throws SQLException {

        String nombreUsuario = obtenerNombreUsuario(idUsuario);
        String rutaCompleta;
        
        if (ruta != null && !ruta.isEmpty()) {
            if (ruta.startsWith("/")) {
                
                if (!ruta.startsWith("/" + nombreUsuario)) {
                    rutaCompleta = "/" + nombreUsuario + ruta;
                } else {
                    rutaCompleta = ruta;
                }
            } else {
                
                rutaCompleta = "/" + nombreUsuario + "/" + ruta;
            }
        } else {
            rutaCompleta = "/" + nombreUsuario;
        }
        
        String query = "SELECT nombre, ruta, nodo, nodo_respaldo FROM Archivo WHERE nombre = ? AND ruta = ? AND Directorio_User_idUser = ?";
        
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, nombre);
            stmt.setString(2, rutaCompleta);
            stmt.setInt(3, idUsuario);
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Archivo archivo = new Archivo();
                archivo.setNombre(rs.getString("nombre"));
                archivo.setRuta(rs.getString("ruta"));
                archivo.setNodo(rs.getInt("nodo"));
                archivo.setNodoRespaldo(rs.getObject("nodo_respaldo", Integer.class));
                
                return archivo;
            }
        }
        return null;
    }

    public Archivo buscarArchivoCompartido(String nombre, String ruta, int idUsuario) throws SQLException {

        String query = "SELECT a.nombre, a.ruta, a.nodo, a.nodo_respaldo " +
                      "FROM Archivo a " +
                      "INNER JOIN Compartir c ON a.idFile = c.Archivo_idFile " +
                      "WHERE a.nombre = ? AND c.Receptor = ?";
        
        if (ruta != null && !ruta.isEmpty()) {
            query += " AND a.ruta = ?";
        }
        
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, nombre);
            stmt.setInt(2, idUsuario);
            
            if (ruta != null && !ruta.isEmpty()) {
                
                stmt.setString(3, ruta);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    Archivo archivo = new Archivo();
                    archivo.setNombre(rs.getString("nombre"));
                    archivo.setRuta(rs.getString("ruta"));
                    archivo.setNodo(rs.getInt("nodo"));
                    archivo.setNodoRespaldo(rs.getObject("nodo_respaldo", Integer.class));
                    
                    return archivo;
                }
                
                String queryPatron = "SELECT a.nombre, a.ruta, a.nodo, a.nodo_respaldo " +
                                   "FROM Archivo a " +
                                   "INNER JOIN Compartir c ON a.idFile = c.Archivo_idFile " +
                                   "WHERE a.nombre = ? AND c.Receptor = ? AND a.ruta LIKE ?";
                
                try (PreparedStatement stmtPatron = conn.prepareStatement(queryPatron)) {
                    stmtPatron.setString(1, nombre);
                    stmtPatron.setInt(2, idUsuario);
                    stmtPatron.setString(3, "%" + ruta + "%");
                    
                    ResultSet rsPatron = stmtPatron.executeQuery();
                    if (rsPatron.next()) {
                        Archivo archivo = new Archivo();
                        archivo.setNombre(rsPatron.getString("nombre"));
                        archivo.setRuta(rsPatron.getString("ruta"));
                        archivo.setNodo(rsPatron.getInt("nodo"));
                        archivo.setNodoRespaldo(rsPatron.getObject("nodo_respaldo", Integer.class));
                        
                        return archivo;
                    }
                }
            } else {
                
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    Archivo archivo = new Archivo();
                    archivo.setNombre(rs.getString("nombre"));
                    archivo.setRuta(rs.getString("ruta"));
                    archivo.setNodo(rs.getInt("nodo"));
                    archivo.setNodoRespaldo(rs.getObject("nodo_respaldo", Integer.class));
                    
                    return archivo;
                }
            }
        }
        return null;
    }

    public List<Object[]> obtenerEstructuraCompleta(int idUsuario) throws SQLException {
        String query = "SELECT 'directorio' as tipo, d.nombre, d.ruta, 0 as tamano, d.idPadre " +
                      "FROM Directorio d " +
                      "WHERE d.User_idUser = ? " +
                      "UNION ALL " +
                      "SELECT 'archivo' as tipo, a.nombre, a.ruta, a.tamano, d.idDirectorio as idPadre " +
                      "FROM Archivo a " +
                      "INNER JOIN Directorio d ON a.Directorio_idDirectorio = d.idDirectorio " +
                      "WHERE a.Directorio_User_idUser = ? " +
                      "ORDER BY ruta, nombre";
        
        List<Object[]> elementos = new ArrayList<>();
        
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, idUsuario);
            stmt.setInt(2, idUsuario);
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Object[] elemento = new Object[5];
                elemento[0] = rs.getString("tipo");        // tipo
                elemento[1] = rs.getString("nombre");      // nombre
                elemento[2] = rs.getString("ruta");        // ruta
                elemento[3] = rs.getLong("tamano");        // tamano
                elemento[4] = rs.getInt("idPadre");        // idPadre
                elementos.add(elemento);
            }
        }
        
        return elementos;
    }
}



class ConexionDB {
    private static final String URL = "jdbc:mysql://localhost:3306/sdbd";
    private static final String USER = "root";
    private static final String PASSWORD = "root";
    
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            // System.out.println("Driver MySQL cargado correctamente");
        } catch (ClassNotFoundException e) {
            System.err.println("Error al cargar el driver MySQL: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
    
    public static void testConnection() {
        try (Connection conn = getConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("✓ Conexión a la base de datos exitosa!");
            }
        } catch (SQLException e) {
            System.err.println("✗ Error en la conexión a la base de datos: " + e.getMessage());
        }
    }
}