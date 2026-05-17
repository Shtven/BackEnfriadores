package com.codespace.simulator.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

/**
 * Respuesta 200 para PATCH /api/usuarios/{id}/aprobar.
 */
public class AprobacionResponse {

    private Integer id;
    private String usuario;
    private boolean activo;

    @JsonProperty("aprobado_por")
    private Integer aprobadoPor;

    @JsonProperty("timestamp_aprobacion")
    private OffsetDateTime timestampAprobacion;

    private String mensaje;

    public AprobacionResponse(Integer id,
                              String usuario,
                              boolean activo,
                              Integer aprobadoPor,
                              OffsetDateTime timestampAprobacion,
                              String mensaje) {
        this.id                  = id;
        this.usuario             = usuario;
        this.activo              = activo;
        this.aprobadoPor         = aprobadoPor;
        this.timestampAprobacion = timestampAprobacion;
        this.mensaje             = mensaje;
    }

    public Integer        getId()                 { return id; }
    public String         getUsuario()            { return usuario; }
    @JsonProperty("activo")
    public boolean        isActivo()              { return activo; }
    public Integer        getAprobadoPor()        { return aprobadoPor; }
    public OffsetDateTime getTimestampAprobacion(){ return timestampAprobacion; }
    public String         getMensaje()            { return mensaje; }
}
