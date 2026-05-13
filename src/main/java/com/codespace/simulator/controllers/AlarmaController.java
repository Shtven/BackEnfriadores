package com.codespace.simulator.controllers;

import com.codespace.simulator.repositories.AlarmaRepository;
import com.codespace.simulator.services.AlarmaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/alarmas")
public class AlarmaController {

    private final AlarmaService      alarmaService;
    private final AlarmaRepository   alarmaRepository;

    public AlarmaController(AlarmaService alarmaService,
                            AlarmaRepository alarmaRepository) {
        this.alarmaService    = alarmaService;
        this.alarmaRepository = alarmaRepository;
    }

    /**
     * POST /api/alarmas/{cuartoId}/silenciar
     * Body: { "operador_id": 1 }
     */
    @PostMapping("/{cuartoId}/silenciar")
    public ResponseEntity<?> silenciar(@PathVariable Integer cuartoId,
                                       @RequestBody Map<String, Integer> body) {
        Integer operadorId = body.get("operador_id");
        if (operadorId == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "operador_id es requerido y debe ser un entero"));
        }

        try {
            alarmaService.silenciarCritica(cuartoId, operadorId);
            return ResponseEntity.ok(Map.of(
                    "mensaje",     "Alarma crítica silenciada correctamente",
                    "cuarto_id",   cuartoId,
                    "operador_id", operadorId
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/cuartos/{id}/alarma-activa
     * Retorna la alarma activa del cuarto, o {estado: "normal"} si no hay ninguna.
     */
    @GetMapping("/cuartos/{id}/alarma-activa")
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