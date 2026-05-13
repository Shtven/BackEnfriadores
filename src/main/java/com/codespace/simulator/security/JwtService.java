package com.codespace.simulator.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long      expirationMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-hours}") int hours) {

        this.secretKey    = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = (long) hours * 3600 * 1000;
    }

    /** Genera JWT con claims operador_id, rol, exp=+8h (T-13-03) */
    public String generarToken(Integer operadorId, String rol) {
        return Jwts.builder()
                .subject(String.valueOf(operadorId))
                .claim("operador_id", operadorId)
                .claim("rol", rol)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    /** Valida firma y expiración, lanza excepción si no es válido (T-13-04) */
    public Claims validarYExtraerClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean esValido(String token) {
        try {
            validarYExtraerClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Integer getOperadorId(String token) {
        return validarYExtraerClaims(token).get("operador_id", Integer.class);
    }

    public String getRol(String token) {
        return validarYExtraerClaims(token).get("rol", String.class);
    }
}