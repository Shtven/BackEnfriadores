package com.codespace.simulator.controllers;

import com.codespace.simulator.entities.IntervencionManual;
import com.codespace.simulator.repositories.IntervencionManualRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/api/intervenciones")
public class IntervencionesController {

    private final IntervencionManualRepository repo;

    public IntervencionesController(IntervencionManualRepository repo) {
        this.repo = repo;
    }

    /**
     * T-12-01: GET /api/intervenciones?cuarto_id=&desde=&hasta=
     * - rol=operador → solo sus propias intervenciones
     * - rol=supervisor → todas
     */
    @GetMapping
    public ResponseEntity<List<IntervencionManual>> listar(
            @RequestParam(required = false) Integer cuartoId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta) {

        Authentication auth      = SecurityContextHolder.getContext().getAuthentication();
        Integer        operadorId = (Integer) auth.getPrincipal();
        String         rol        = auth.getAuthorities().iterator().next()
                .getAuthority()
                .replace("ROLE_", "")
                .toLowerCase();

        if (desde == null) desde = OffsetDateTime.now(ZoneOffset.UTC).minusDays(30);
        if (hasta == null) hasta = OffsetDateTime.now(ZoneOffset.UTC);

        List<IntervencionManual> resultado;

        if ("supervisor".equals(rol)) {
            // Supervisor ve todas
            resultado = cuartoId != null
                    ? repo.findByCuartoIdAndTimestampBetweenOrderByTimestampDesc(
                    cuartoId, desde.toInstant(), hasta.toInstant())
                    : repo.findByTimestampBetweenOrderByTimestampDesc(
                    desde.toInstant(), hasta.toInstant());
        } else {
            // Operador solo ve las suyas
            resultado = cuartoId != null
                    ? repo.findByOperadorIdAndCuartoIdAndTimestampBetweenOrderByTimestampDesc(
                    operadorId, cuartoId, desde.toInstant(), hasta.toInstant())
                    : repo.findByOperadorIdAndTimestampBetweenOrderByTimestampDesc(
                    operadorId, desde.toInstant(), hasta.toInstant());
        }

        return ResponseEntity.ok(resultado);
    }
}