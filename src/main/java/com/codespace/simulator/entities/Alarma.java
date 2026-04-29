package com.codespace.simulator.entities;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "alarmas")
public class Alarma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cuarto_id", nullable = false)
    private Integer cuartoId;

    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo; // "preventiva" | "critica"

    @Column(name = "timestamp_inicio", nullable = false)
    private Instant timestampInicio;

    @Column(name = "timestamp_fin")
    private Instant timestampFin;

    @Column(name = "temperatura_pico", nullable = false)
    private Double temperaturaPico;

    // FK a tabla operadores — null mientras está activa
    @Column(name = "silenciada_por")
    private Integer silenciadaPor;

    @Column(name = "timestamp_silencio")
    private Instant timestampSilencio;

    @PrePersist
    protected void onCreate() {
        if (this.timestampInicio == null) this.timestampInicio = Instant.now();
    }

    public Alarma() {}

    public Alarma(Integer cuartoId, String tipo, Double temperaturaPico) {
        this.cuartoId        = cuartoId;
        this.tipo            = tipo;
        this.temperaturaPico = temperaturaPico;
        this.timestampInicio = Instant.now();
    }

    // ── Helpers de estado (sin columna estado en BD) ─────────
    public boolean isActiva()    { return this.timestampFin == null; }
    public boolean isSilenciada(){ return this.silenciadaPor != null; }

    // ── Getters y Setters ─────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getCuartoId() { return cuartoId; }
    public void setCuartoId(Integer cuartoId) { this.cuartoId = cuartoId; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public Instant getTimestampInicio() { return timestampInicio; }
    public void setTimestampInicio(Instant t) { this.timestampInicio = t; }

    public Instant getTimestampFin() { return timestampFin; }
    public void setTimestampFin(Instant t) { this.timestampFin = t; }

    public Double getTemperaturaPico() { return temperaturaPico; }
    public void setTemperaturaPico(Double t) { this.temperaturaPico = t; }

    public Integer getSilenciadaPor() { return silenciadaPor; }
    public void setSilenciadaPor(Integer silenciadaPor) { this.silenciadaPor = silenciadaPor; }

    public Instant getTimestampSilencio() { return timestampSilencio; }
    public void setTimestampSilencio(Instant t) { this.timestampSilencio = t; }
}