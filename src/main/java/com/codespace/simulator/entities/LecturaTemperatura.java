package com.codespace.simulator.entities;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "lecturas_temperatura")
public class LecturaTemperatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cuarto_id", nullable = false)
    private Integer cuartoId;

    @Column(name = "sensor_id", nullable = false, length = 100)
    private String sensorId;

    @Column(name = "temperatura", nullable = false)
    private Double temperatura;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "estado_alarma", nullable = false, length = 20)
    private String estadoAlarma;

    @Column(name = "origen", length = 20)
    private String origen;

    @Column(name = "timestamp_evento")
    private Instant timestampEvento;

    @Column(name = "timestamp_registro", nullable = false, updatable = false)
    private Instant timestampRegistro;

    @Column(name = "topic", length = 255)
    private String topic;

    @PrePersist
    protected void onCreate() {
        this.timestampRegistro = Instant.now();
        if (this.timestamp == null) this.timestamp = Instant.now();
        if (this.estadoAlarma == null) this.estadoAlarma = "normal";
    }

    public LecturaTemperatura() {}

    public LecturaTemperatura(Integer cuartoId, String sensorId, Double temperatura, Instant timestamp, String topic) {
        this.cuartoId    = cuartoId;
        this.sensorId    = sensorId;
        this.temperatura = temperatura;
        this.timestamp   = timestamp;
        this.timestampEvento = timestamp;
        this.topic       = topic;
        this.estadoAlarma = "normal";
        this.origen       = "mqtt";
    }

    // getters y setters de los campos nuevos
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getEstadoAlarma() { return estadoAlarma; }
    public void setEstadoAlarma(String estadoAlarma) { this.estadoAlarma = estadoAlarma; }

    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Double getTemperatura() {
        return temperatura;
    }

    public void setTemperatura(Double temperatura) {
        this.temperatura = temperatura;
    }

    public Instant getTimestampEvento() {
        return timestampEvento;
    }

    public void setTimestampEvento(Instant timestampEvento) {
        this.timestampEvento = timestampEvento;
    }

    public Instant getTimestampRegistro() {
        return timestampRegistro;
    }

    public void setTimestampRegistro(Instant timestampRegistro) {
        this.timestampRegistro = timestampRegistro;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }
}