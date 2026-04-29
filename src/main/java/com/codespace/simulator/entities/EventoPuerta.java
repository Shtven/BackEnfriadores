package com.codespace.simulator.entities;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "eventos_puerta")
public class EventoPuerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cuarto_id", nullable = false)
    private Integer cuartoId;

    @Column(name = "timestamp", nullable = true)
    private Instant timestamp;

    @Column(name = "accion", nullable = false, length = 30)
    private String accion;

    @Column(name = "origen", nullable = false, length = 20)
    private String origen;

    @Column(name = "sensor_id", nullable = false, length = 100)
    private String sensorId;

    @Column(name = "temperatura")
    private Double temperatura;

    @Column(name = "presencia")
    private Boolean presencia;

    @Column(name = "alarma_id")
    private Integer alarmaId;

    @PrePersist
    protected void onCreate() {
        if (this.timestamp == null) this.timestamp = Instant.now();
        if (this.sensorId  == null) this.sensorId  = "sistema";
    }

    public EventoPuerta() {}

    // Constructor para eventos automáticos (sin sensor físico)
    public EventoPuerta(Integer cuartoId, String accion, String origen,
                        Double temperatura, Boolean presencia) {
        this.cuartoId    = cuartoId;
        this.accion      = accion;
        this.origen      = origen;
        this.temperatura = temperatura;
        this.presencia   = presencia;
        this.sensorId    = "sistema";
        this.timestamp   = Instant.now();
    }

    // Constructor para eventos de sensor físico
    public EventoPuerta(Integer cuartoId, String accion, String origen,
                        Double temperatura, Boolean presencia, String sensorId) {
        this(cuartoId, accion, origen, temperatura, presencia);
        this.sensorId = sensorId;
    }

    // ── Getters y Setters ─────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getCuartoId() { return cuartoId; }
    public void setCuartoId(Integer cuartoId) { this.cuartoId = cuartoId; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }

    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }

    public String getSensorId() { return sensorId; }
    public void setSensorId(String sensorId) { this.sensorId = sensorId; }

    public Double getTemperatura() { return temperatura; }
    public void setTemperatura(Double temperatura) { this.temperatura = temperatura; }

    public Boolean getPresencia() { return presencia; }
    public void setPresencia(Boolean presencia) { this.presencia = presencia; }

    public Integer getAlarmaId() { return alarmaId; }
    public void setAlarmaId(Integer alarmaId) { this.alarmaId = alarmaId; }
}