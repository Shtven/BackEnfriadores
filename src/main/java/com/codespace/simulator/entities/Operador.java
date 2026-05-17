package com.codespace.simulator.entities;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "operadores")
public class Operador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "rol", nullable = false)
    private String rol;             // 'operador' | 'supervisor'

    @Column(name = "activo")
    private boolean activo;

    @Column(name = "creado_en")
    private OffsetDateTime creadoEn;

    // ── Nuevas columnas (T-13-01) ─────────────────────────────────
    @Column(name = "usuario", unique = true)
    private String usuario;

    @Column(name = "password_hash")
    private String passwordHash;

    // ── Aprobacion HACCP (registro dinamico) ──────────────────────
    // FK logica a operador.id de quien aprobo (no mapeada como relacion JPA
    // para evitar lazy fetch innecesario en respuestas; se valida en el service).
    @Column(name = "aprobado_por")
    private Integer aprobadoPor;

    @Column(name = "timestamp_aprobacion")
    private OffsetDateTime timestampAprobacion;

    // Getters y Setters
    public Integer getId()           { return id; }
    public String getNombre()        { return nombre; }
    public String getRol()           { return rol; }
    public boolean isActivo()        { return activo; }
    public OffsetDateTime getCreadoEn() { return creadoEn; }
    public String getUsuario()       { return usuario; }
    public String getPasswordHash()  { return passwordHash; }
    public Integer getAprobadoPor()  { return aprobadoPor; }
    public OffsetDateTime getTimestampAprobacion() { return timestampAprobacion; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setRol(String rol) { this.rol = rol; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public void setCreadoEn(OffsetDateTime creadoEn) { this.creadoEn = creadoEn; }
    public void setUsuario(String u) { this.usuario = u; }
    public void setPasswordHash(String h) { this.passwordHash = h; }
    public void setAprobadoPor(Integer aprobadoPor) { this.aprobadoPor = aprobadoPor; }
    public void setTimestampAprobacion(OffsetDateTime timestampAprobacion) { this.timestampAprobacion = timestampAprobacion; }
}
