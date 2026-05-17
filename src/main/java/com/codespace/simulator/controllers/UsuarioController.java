package com.codespace.simulator.controllers;

import com.codespace.simulator.dto.request.RegistroRequest;
import com.codespace.simulator.dto.response.AprobacionResponse;
import com.codespace.simulator.dto.response.RegistroResponse;
import com.codespace.simulator.dto.response.UsuarioPendienteResponse;
import com.codespace.simulator.services.UsuarioService;
import com.codespace.simulator.services.UsuarioService.Resultado;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Endpoints del flujo de registro dinamico con aprobacion HACCP:
 *   POST   /api/usuarios/registro        (publico)
 *   PATCH  /api/usuarios/{id}/aprobar    (operador activo)
 *   GET    /api/usuarios/pendientes      (operador activo)
 *
 * No incluye cambios sobre /api/login ni sobre la entidad Operador
 * fuera del flujo de aprobacion.
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // ── 1) Registro publico ────────────────────────────────────────────
    @PostMapping("/registro")
    public ResponseEntity<?> registrar(@RequestBody(required = false) RegistroRequest req) {
        Resultado<RegistroResponse> r = usuarioService.registrar(req);
        if (r.error != null) {
            return ResponseEntity.status(r.status).body(Map.of("error", r.error));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(r.body);
    }

    // ── 2) Aprobacion (operador activo) ────────────────────────────────
    @PatchMapping("/{id}/aprobar")
    public ResponseEntity<?> aprobar(@PathVariable("id") Integer id) {
        ClaimsJwt c = leerClaimsJwt();
        if (c == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "JWT requerido"));
        }
        Resultado<AprobacionResponse> r = usuarioService.aprobar(id, c.operadorId, c.rol);
        if (r.error != null) {
            return ResponseEntity.status(r.status).body(Map.of("error", r.error));
        }
        return ResponseEntity.status(r.status).body(r.body);
    }

    // ── 3) Listado de pendientes ───────────────────────────────────────
    @GetMapping("/pendientes")
    public ResponseEntity<?> listarPendientes() {
        ClaimsJwt c = leerClaimsJwt();
        if (c == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "JWT requerido"));
        }
        Resultado<List<UsuarioPendienteResponse>> r = usuarioService.listarPendientes(c.operadorId, c.rol);
        if (r.error != null) {
            return ResponseEntity.status(r.status).body(Map.of("error", r.error));
        }
        return ResponseEntity.status(r.status).body(r.body);
    }

    // ── Helpers ────────────────────────────────────────────────────────
    /** Extrae operador_id y rol que JwtAuthFilter inyecto en el SecurityContext. */
    private ClaimsJwt leerClaimsJwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof Integer operadorId)) {
            return null;
        }
        String rol = auth.getDetails() instanceof String ? (String) auth.getDetails() : null;
        if (rol == null) {
            return null;
        }
        return new ClaimsJwt(operadorId, rol);
    }

    private record ClaimsJwt(Integer operadorId, String rol) {}
}
