package com.codespace.simulator.entities;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.OffsetDateTime;

@Entity
@Table(name = "lecturas_temperatura")
public class LecturaTemperatura {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cuarto_id",          nullable = false) private Integer cuartoId;
    @Column(name = "timestamp",          nullable = false) private OffsetDateTime timestamp;
    @Column(name = "temperatura",        nullable = false) private Double temperatura;
    @Column(name = "estado_alarma",      nullable = false) private String estadoAlarma;
    @Column(name = "origen")                               private String origen;
    @Column(name = "sensor_id")                            private Integer sensorId;
    @Column(name = "timestamp_evento")                     private OffsetDateTime timestampEvento;
    @Column(name = "timestamp_registro")                   private OffsetDateTime timestampRegistro;
    @Column(name = "topic")                                private String topic;

    // Agregar en LecturaTemperatura.java:
    public LecturaTemperatura(Integer cuartoId, Integer sensorId,
                              Double temperatura, Instant timestamp,
                              String topic) {
        this.cuartoId           = cuartoId;
        this.sensorId           = sensorId;
        this.temperatura        = temperatura;
        this.timestampEvento    = timestamp != null
                ? timestamp.atOffset(java.time.ZoneOffset.UTC) : null;
        this.timestamp          = OffsetDateTime.now();
        this.timestampRegistro  = OffsetDateTime.now();
        this.topic              = topic;
        this.estadoAlarma       = "normal";   // valor por defecto
        this.origen             = "simulador";
    }

    public LecturaTemperatura() {

    }

    // Getters
    public Long           getId()               { return id; }
    public Integer        getCuartoId()         { return cuartoId; }
    public OffsetDateTime getTimestamp()        { return timestamp; }
    public Double         getTemperatura()      { return temperatura; }
    public String         getEstadoAlarma()     { return estadoAlarma; }
    public String         getOrigen()           { return origen; }
    public Integer         getSensorId()         { return sensorId; }
    public OffsetDateTime getTimestampEvento()  { return timestampEvento; }
    public OffsetDateTime getTimestampRegistro(){ return timestampRegistro; }
    public String         getTopic()            { return topic; }

    // Setters
    public void setCuartoId(Integer v)          { this.cuartoId = v; }
    public void setTimestamp(OffsetDateTime v)   { this.timestamp = v; }
    public void setTemperatura(Double v)         { this.temperatura = v; }
    public void setEstadoAlarma(String v)        { this.estadoAlarma = v; }
    public void setOrigen(String v)              { this.origen = v; }
    public void setSensorId(Integer v)            { this.sensorId = v; }
    public void setTimestampEvento(OffsetDateTime v)  { this.timestampEvento = v; }
    public void setTimestampRegistro(OffsetDateTime v){ this.timestampRegistro = v; }
    public void setTopic(String v)               { this.topic = v; }
}