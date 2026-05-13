package com.codespace.simulator.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SilencioPayload {

    @JsonProperty("cuarto")
    private Integer cuarto;

    @JsonProperty("operador_id")
    private Integer operadorId;

    public Integer getCuarto()    { return cuarto; }
    public Integer getOperadorId() { return operadorId; }
}