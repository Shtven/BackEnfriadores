package com.codespace.simulator.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PresenciaEvent {

    @JsonProperty("cuarto_id")
    private Integer cuartoId;

    @JsonProperty("sensor_id")
    private String sensorId;

    @JsonProperty("presencia")
    private Boolean presencia;

    @JsonProperty("timestamp")
    private Instant timestamp;

    private String topic;

    public Integer getCuartoId() {
        return cuartoId;
    }

    public void setCuartoId(Integer cuartoId) {
        this.cuartoId = cuartoId;
    }

    public String getSensorId() {
        return sensorId;
    }

    public void setSensorId(String sensorId) {
        this.sensorId = sensorId;
    }

    public Boolean getPresencia() {
        return presencia;
    }

    public void setPresencia(Boolean presencia) {
        this.presencia = presencia;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }
}