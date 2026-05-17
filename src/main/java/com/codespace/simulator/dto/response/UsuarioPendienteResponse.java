package com.codespace.simulator.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

/**
 * Item del listado GET /api/usuarios/pendientes.
 * NUNCA expone password_hash.
 */
public class UsuarioPendienteResponse {

    private Integer id;
    private String nombre;
    private String usuario;
    private String rol;

    @JsonProperty("creado_en")
    private OffsetDateTime creadoEn;

    public UsuarioPendienteResponse(Integer id,
                                    String nombre,
                                    String usuario,
                                    String rol,
                                    OffsetDateTime creadoEn) {
        this.id       = id;
        this.nombre   = nombre;
        this.usuario  = usuario;
        this.rol      = rol;
        this.creadoEn = creadoEn;
    }

    public Integer        getId()       { return id; }
    public String         getNombre()   { return nombre; }
    public String         getUsuario()  { return usuario; }
    public String         getRol()      { return rol; }
    public OffsetDateTime getCreadoEn() { return creadoEn; }
}
