package com.codespace.simulator.repositories;

import com.codespace.simulator.entities.Operador;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OperadorRepository extends JpaRepository<Operador, Integer> {

    Optional<Operador> findByUsuario(String usuario);

    boolean existsByUsuario(String usuario);

    /** Lista cuentas pendientes de aprobacion (registro dinamico HACCP). */
    List<Operador> findByRolAndActivoFalseOrderByCreadoEnAsc(String rol);
}
