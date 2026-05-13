package com.codespace.simulator.entities;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "refrigeracion_estado")
public class RefrigeracionEstado {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cuarto_id",           nullable = false)
    private Integer cuartoId;

    @Column(name = "potencia_porcentaje", nullable = false)
    private Integer potenciaPorcentaje;

    @Column(name = "motivo",              nullable = false)
    private String motivo;   // NORMAL | PUERTA_ABIERTA | FORZADO_MANUAL

    @Column(name = "timestamp",           nullable = false)
    private Instant timestamp;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) timestamp = Instant.now();
    }

    public RefrigeracionEstado() {}

    public RefrigeracionEstado(Integer cuartoId, Integer potencia, String motivo) {
        this.cuartoId           = cuartoId;
        this.potenciaPorcentaje = potencia;
        this.motivo             = motivo;
        this.timestamp          = Instant.now();
    }

    public Long    getId()                { return id; }
    public Integer getCuartoId()          { return cuartoId; }
    public Integer getPotenciaPorcentaje(){ return potenciaPorcentaje; }
    public String  getMotivo()            { return motivo; }
    public Instant getTimestamp()         { return timestamp; }
}