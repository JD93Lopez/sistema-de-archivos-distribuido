package central;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ServidorBaseDatos {

    public void guardarArchivo(Archivo archivo, int idUsuario) throws SQLException {
        String query = "INSERT INTO Archivo (nombre, ruta, tamano, fecha_crea, nodo, Directorio_idDirectorio, Directorio_User_idUser) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConexionDB.getConnection();
            PreparedStatement stmt = conn.prepareStatement(query)) {
            
            String rutaDirectorio = archivo.getRuta();
            int idDirectorio = obtenerIdDirectorioPorRuta(rutaDirectorio, idUsuario);
            
            if (idDirectorio == -1) {
                idDirectorio = obtenerIdDirectorioRaiz(idUsuario);
            }
                
            stmt.setString(1, archivo.getNombre());
            stmt.setString(2, archivo.getRuta());
            stmt.setLong(3, archivo.getContenido().length);
            stmt.setDate(4, new java.sql.Date(System.currentTimeMillis()));
            stmt.setInt(5, 1);
            stmt.setInt(6, idDirectorio);
            stmt.setInt(7, idUsuario);

            stmt.executeUpdate();
            System.out.println("Archivo guardado en la base de datos: " + archivo.getNombre());
        }
    }

    public void eliminarArchivo(int idArchivo) throws SQLException {
        String query = "DELETE FROM Archivo WHERE idFile = ?";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, idArchivo);
            stmt.executeUpdate();
            System.out.println("Archivo eliminado de la base de datos: " + idArchivo);
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
        System.out.println("Conectando a la base de datos...");
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