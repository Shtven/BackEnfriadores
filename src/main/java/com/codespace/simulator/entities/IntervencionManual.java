package com.codespace.simulator.entities;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "intervenciones_manuales")
public class IntervencionManual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK a operadores — para acciones automáticas usa el id del operador 'sistema'
    @Column(name = "operador_id", nullable = false)
    private Integer operadorId;

    @Column(name = "cuarto_id", nullable = false)
    private Integer cuartoId;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "tipo_accion", nullable = false, length = 50)
    private String tipoAccion;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "alarma_id")
    private Integer alarmaId;

    @PrePersist
    protected void onCreate() {
        if (this.timestamp == null) this.timestamp = Instant.now();
    }

    public IntervencionManual() {}

    public IntervencionManual(Integer operadorId, Integer cuartoId,
                              String tipoAccion, String descripcion) {
        this.operadorId  = operadorId;
        this.cuartoId    = cuartoId;
        this.tipoAccion  = tipoAccion;
        this.descripcion = descripcion;
        this.timestamp   = Instant.now();
    }

    // ── Getters y Setters ─────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getOperadorId() { return operadorId; }
    public void setOperadorId(Integer operadorId) { this.operadorId = operadorId; }

    public Integer getCuartoId() { return cuartoId; }
    public void setCuartoId(Integer cuartoId) { this.cuartoId = cuartoId; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getTipoAccion() { return tipoAccion; }
    public void setTipoAccion(String tipoAccion) { this.tipoAccion = tipoAccion; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Integer getAlarmaId() { return alarmaId; }
    public void setAlarmaId(Integer alarmaId) { this.alarmaId = alarmaId; }
}