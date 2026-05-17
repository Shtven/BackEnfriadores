package com.codespace.simulator.dto.request;

/**
 * Payload para POST /api/usuarios/registro.
 * La validacion (largos, formato, unicidad) la hace UsuarioService.
 */
public class RegistroRequest {

    private String nombre;
    private String usuario;
    private String password;
    private String rol;

    public String getNombre()   { return nombre; }
    public String getUsuario()  { return usuario; }
    public String getPassword() { return password; }
    public String getRol()      { return rol; }

    public void setNombre(String nombre)     { this.nombre = nombre; }
    public void setUsuario(String usuario)   { this.usuario = usuario; }
    public void setPassword(String password) { this.password = password; }
    public void setRol(String rol)           { this.rol = rol; }
}
