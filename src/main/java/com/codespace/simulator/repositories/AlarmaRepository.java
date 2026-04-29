package com.codespace.simulator.repositories;

import com.codespace.simulator.entities.Alarma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AlarmaRepository extends JpaRepository<Alarma, Long> {

    Optional<Alarma> findByCuartoIdAndTimestampFinIsNull(Integer cuartoId);

    Optional<Alarma> findByCuartoIdAndTipoAndTimestampFinIsNull(Integer cuartoId, String tipo);
}