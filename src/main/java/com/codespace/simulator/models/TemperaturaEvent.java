package com.codespace.simulator.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TemperaturaEvent {

    @JsonProperty("cuarto_id")
    private Integer cuartoId;

    @JsonProperty("sensor_id")
    private Integer sensorId;

    @JsonProperty("temperatura")
    private Double temperatura;

    @JsonProperty("timestamp")
    private Instant timestamp;

    // Inyectado programáticamente (no viene en el JSON)
    private String topic;

    public Integer getCuartoId() {
        return cuartoId;
    }

    public void setCuartoId(Integer cuartoId) {
        this.cuartoId = cuartoId;
    }

    public Integer getSensorId() {
        return sensorId;
    }

    public void setSensorId(Integer sensorId) {
        this.sensorId = sensorId;
    }

    public Double getTemperatura() {
        return temperatura;
    }

    public void setTemperatura(Double temperatura) {
        this.temperatura = temperatura;
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