package com.codespace.simulator.repositories;

import com.codespace.simulator.entities.IntervencionManual;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface IntervencionManualRepository extends JpaRepository<IntervencionManual, Long> {
    List<IntervencionManual> findByTimestampBetweenOrderByTimestampDesc(
            Instant desde, Instant hasta);

    List<IntervencionManual> findByCuartoIdAndTimestampBetweenOrderByTimestampDesc(
            Integer cuartoId, Instant desde, Instant hasta);

    List<IntervencionManual> findByOperadorIdAndTimestampBetweenOrderByTimestampDesc(
            Integer operadorId, Instant desde, Instant hasta);

    List<IntervencionManual> findByOperadorIdAndCuartoIdAndTimestampBetweenOrderByTimestampDesc(
            Integer operadorId, Integer cuartoId, Instant desde, Instant hasta);
}