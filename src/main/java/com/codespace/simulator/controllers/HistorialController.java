package com.codespace.simulator.controllers;

import com.codespace.simulator.entities.LecturaTemperatura;
import com.codespace.simulator.repositories.AlarmaRepository;
import com.codespace.simulator.services.HistorialService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cuartos")
public class HistorialController {

    private final HistorialService historialService;
    private final AlarmaRepository alarmaRepository;

    public HistorialController(HistorialService historialService, AlarmaRepository alarmaRepository) {
        this.historialService = historialService;
        this.alarmaRepository = alarmaRepository;
    }

    /**
     * T-11-01: GET /api/cuartos/{id}/historial
     * Params: shortcut=24h|7d|30d  OR  desde=&hasta=  (ISO-8601)
     *         formato=json|csv  (default json)
     * Requiere JWT válido (operador o supervisor) — garantizado por SecurityFilterChain.
     */
    @GetMapping("/{id}/historial")
    public ResponseEntity<?> historial(
            @PathVariable Integer id,
            @RequestParam(required = false) String shortcut,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta,
            @RequestParam(defaultValue = "json") String formato) {

        List<LecturaTemperatura> lecturas =
                historialService.consultar(id, shortcut, desde, hasta);

        // T-11-03: respuesta CSV
        if ("csv".equalsIgnoreCase(formato)) {
            String csv      = historialService.generarCsv(lecturas);
            String filename = "historial_cuarto" + id + "_"
                    + LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.BASIC_ISO_DATE)
                    + ".csv";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(csv);
        }

        // Respuesta JSON (default)
        return ResponseEntity.ok(historialService.toJson(lecturas));
    }

    @GetMapping("/{id}/alarma-activa")
    public ResponseEntity<?> alarmaActiva(@PathVariable Integer id) {
        return alarmaRepository
                .findByCuartoIdAndTimestampFinIsNull(id)
                .map(alarma -> ResponseEntity.ok(Map.of(
                        "cuarto_id",        alarma.getCuartoId(),
                        "estado",           alarma.getTipo(),
                        "tipo",             alarma.getTipo(),
                        "temperatura_pico", alarma.getTemperaturaPico(),
                        "timestamp_inicio", alarma.getTimestampInicio().toString()
                )))
                .orElse(ResponseEntity.ok(Map.of(
                        "cuarto_id", id,
                        "estado",    "normal"
                )));
    }
}