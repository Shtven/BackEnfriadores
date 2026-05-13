package com.codespace.simulator.controllers;

import com.codespace.simulator.dto.request.LoginRequest;
import com.codespace.simulator.dto.response.LoginResponse;
import com.codespace.simulator.entities.Operador;
import com.codespace.simulator.repositories.OperadorRepository;
import com.codespace.simulator.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final OperadorRepository operadorRepository;
    private final PasswordEncoder    passwordEncoder;
    private final JwtService         jwtService;

    public AuthController(OperadorRepository operadorRepository,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService) {
        this.operadorRepository = operadorRepository;
        this.passwordEncoder    = passwordEncoder;
        this.jwtService         = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {

        Operador operador = operadorRepository
                .findByUsuario(req.getUsuario())
                .orElse(null);

        if (operador == null
                || !operador.isActivo()
                || !passwordEncoder.matches(req.getPassword(), operador.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("{\"error\": \"Credenciales inválidas\"}");
        }

        String jwt = jwtService.generarToken(operador.getId(), operador.getRol());

        return ResponseEntity.ok(new LoginResponse(
                operador.getId(),
                operador.getNombre(),
                operador.getRol(),
                jwt
        ));
    }
}