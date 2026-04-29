package com.codespace.simulator.controllers;

import com.codespace.simulator.services.AlarmaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/alarmas")
public class AlarmaController {

    private final AlarmaService alarmaService;

    public AlarmaController(AlarmaService alarmaService) {
        this.alarmaService = alarmaService;
    }

    /**
     * POST /api/alarmas/{cuartoId}/silenciar
     * Body: { "operador_id": 1 }   ← Integer, FK a tabla operadores
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
                    "mensaje",      "Alarma crítica silenciada correctamente",
                    "cuarto_id",    cuartoId,
                    "operador_id",  operadorId
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }
}