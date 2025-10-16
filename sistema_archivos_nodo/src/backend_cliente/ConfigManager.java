package backend_cliente;

import java.io.*;
import java.util.Properties;

public class ConfigManager {
    private static final String CONFIG_FILE = "config.properties";
    private static final String DEFAULT_SOAP_URL = "http://10.152.190.35:8080/ServicioSOAP";
    
    private Properties properties;
    private static ConfigManager instance;
    
    private ConfigManager() {
        loadConfig();
    }
    
    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }
    
    private void loadConfig() {
        properties = new Properties();
        
        // Buscar primero en el directorio actual
        File configFile = new File(CONFIG_FILE);
        if (!configFile.exists()) {
            // Si no existe, buscar en el directorio padre
            configFile = new File("../" + CONFIG_FILE);
        }
        
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                properties.load(fis);
                System.out.println(" Configuración cargada desde: " + configFile.getAbsolutePath());
            } catch (IOException e) {
                System.err.println(" Error al cargar configuración: " + e.getMessage());
                System.out.println("Usando configuración por defecto");
            }
        } else {
            System.out.println(" Archivo config.properties no encontrado. Usando configuración por defecto");
        }
    }
    
    public String getSoapServerUrl() {
        return properties.getProperty("soap.server.url", DEFAULT_SOAP_URL);
    }
    
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    public void printConfig() {
        System.out.println("CONFIGURACIÓN ACTUAL");
        System.out.println("URL Servidor SOAP: " + getSoapServerUrl());
        System.out.println("");
    }
}