package com.codespace.simulator.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Respuesta 201 para POST /api/usuarios/registro.
 * NUNCA expone password_hash.
 */
public class RegistroResponse {

    private Integer id;
    private String usuario;
    private String rol;
    private boolean activo;
    private String mensaje;

    public RegistroResponse(Integer id, String usuario, String rol, boolean activo, String mensaje) {
        this.id      = id;
        this.usuario = usuario;
        this.rol     = rol;
        this.activo  = activo;
        this.mensaje = mensaje;
    }

    public Integer getId()      { return id; }
    public String  getUsuario() { return usuario; }
    public String  getRol()     { return rol; }
    @JsonProperty("activo")
    public boolean isActivo()   { return activo; }
    public String  getMensaje() { return mensaje; }
}
