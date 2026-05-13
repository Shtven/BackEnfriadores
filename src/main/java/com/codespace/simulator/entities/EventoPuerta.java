package com.codespace.simulator.entities;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "eventos_puerta")
public class EventoPuerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cuarto_id",          nullable = false) private Integer cuartoId;
    @Column(name = "timestamp",          nullable = false) private OffsetDateTime timestamp;
    @Column(name = "accion",             nullable = false) private String accion;
    @Column(name = "origen",             nullable = false) private String origen;
    @Column(name = "temperatura")                          private Double temperatura;
    @Column(name = "presencia")                            private Boolean presencia;
    @Column(name = "alarma_id")                            private Integer alarmaId;
    @Column(name = "sensor_id",          nullable = false) private Integer sensorId;
    @Column(name = "timestamp_evento")                     private OffsetDateTime timestampEvento;
    @Column(name = "timestamp_registro", nullable = false) private OffsetDateTime timestampRegistro;
    @Column(name = "tipo")                                 private String tipo;
    @Column(name = "topic")                                private String topic;

    // Getters y Setters
    public Long getId()                              { return id; }
    public Integer getCuartoId()                     { return cuartoId; }
    public void setCuartoId(Integer v)               { this.cuartoId = v; }
    public OffsetDateTime getTimestamp()             { return timestamp; }
    public void setTimestamp(OffsetDateTime v)        { this.timestamp = v; }
    public String getAccion()                        { return accion; }
    public void setAccion(String v)                  { this.accion = v; }
    public String getOrigen()                        { return origen; }
    public void setOrigen(String v)                  { this.origen = v; }
    public Double getTemperatura()                   { return temperatura; }
    public void setTemperatura(Double v)             { this.temperatura = v; }
    public Boolean getPresencia()                    { return presencia; }
    public void setPresencia(Boolean v)              { this.presencia = v; }
    public Integer getAlarmaId()                     { return alarmaId; }
    public void setAlarmaId(Integer v)               { this.alarmaId = v; }
    public Integer getSensorId()                      { return sensorId; }
    public void setSensorId(Integer v)                { this.sensorId = v; }
    public OffsetDateTime getTimestampEvento()       { return timestampEvento; }
    public void setTimestampEvento(OffsetDateTime v) { this.timestampEvento = v; }
    public OffsetDateTime getTimestampRegistro()     { return timestampRegistro; }
    public void setTimestampRegistro(OffsetDateTime v){ this.timestampRegistro = v; }
    public String getTipo()                          { return tipo; }
    public void setTipo(String v)                    { this.tipo = v; }
    public String getTopic()                         { return topic; }
    public void setTopic(String v)                   { this.topic = v; }
}