package com.codespace.simulator.config;

import com.codespace.simulator.entities.Operador;
import com.codespace.simulator.repositories.OperadorRepository;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class OperadorSeedConfig {

    private static final Logger log = LoggerFactory.getLogger(OperadorSeedConfig.class);

    @Bean
    ApplicationRunner seedOperadorDefault(OperadorRepository operadorRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (operadorRepository.count() > 0) {
                return;
            }

            Operador operador = new Operador();
            operador.setNombre("Operador Local");
            operador.setRol("operador");
            operador.setActivo(true);
            operador.setCreadoEn(OffsetDateTime.now());
            operador.setUsuario("operador");
            operador.setPasswordHash(passwordEncoder.encode("1234"));

            operadorRepository.save(operador);
            log.warn("[SEED] Operador por defecto creado: usuario=operador rol=operador");
        };
    }
}
