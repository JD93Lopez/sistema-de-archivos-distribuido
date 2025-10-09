package backend_cliente;

import java.io.Serializable;

public class Usuario implements Serializable {
    private String nombre;
    private String email;

    public Usuario() {}

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}