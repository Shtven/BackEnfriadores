package com.codespace.simulator.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LoginResponse {

    @JsonProperty("operador_id")
    private Integer operadorId;

    @JsonProperty("nombre")
    private String nombre;

    @JsonProperty("rol")
    private String rol;

    @JsonProperty("jwt_token")
    private String jwtToken;

    public LoginResponse(Integer operadorId, String nombre, String rol, String jwtToken) {
        this.operadorId = operadorId;
        this.nombre     = nombre;
        this.rol        = rol;
        this.jwtToken   = jwtToken;
    }

    public Integer getOperadorId() { return operadorId; }
    public String getNombre()      { return nombre; }
    public String getRol()         { return rol; }
    public String getJwtToken()    { return jwtToken; }
}