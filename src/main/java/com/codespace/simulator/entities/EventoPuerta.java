package com.codespace.simulator.entities;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "eventos_puerta")    // T-03-03: tabla visible en pgAdmin
public class EventoPuerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cuarto_id", nullable = false)
    private Integer cuartoId;

    @Column(name = "sensor_id", nullable = false, length = 100)
    private String sensorId;

    // T-03-03: tipo fijo "deteccion_presencia"
    @Column(name = "tipo", nullable = false, length = 50)
    private String tipo;

    @Column(name = "presencia")
    private Boolean presencia;

    @Column(name = "timestamp_evento")
    private Instant timestampEvento;

    @Column(name = "timestamp_registro", nullable = false, updatable = false)
    private Instant timestampRegistro;

    @Column(name = "topic", length = 255)
    private String topic;

    @PrePersist
    protected void onCreate() {
        this.timestampRegistro = Instant.now();
    }

    public EventoPuerta() {}

    public EventoPuerta(Integer cuartoId, String sensorId, String deteccionPresencia, Boolean aTrue, Instant timestamp, String topic) {
    }

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

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Boolean getPresencia() {
        return presencia;
    }

    public void setPresencia(Boolean presencia) {
        this.presencia = presencia;
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