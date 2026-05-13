package com.codespace.simulator.repositories;

import com.codespace.simulator.entities.RefrigeracionEstado;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RefrigeracionEstadoRepository
        extends JpaRepository<RefrigeracionEstado, Long> {

    Optional<RefrigeracionEstado> findTopByCuartoIdOrderByTimestampDesc(Integer cuartoId);
}