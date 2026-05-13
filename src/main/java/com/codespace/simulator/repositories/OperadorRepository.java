package com.codespace.simulator.repositories;

import com.codespace.simulator.entities.Operador;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OperadorRepository extends JpaRepository<Operador, Integer> {
    Optional<Operador> findByUsuario(String usuario);
}