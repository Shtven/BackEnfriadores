package com.codespace.simulator.repositories;

import com.codespace.simulator.entities.LecturaTemperatura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.OffsetDateTime;
import java.util.List;

public interface LecturaTemperaturaRepository
        extends JpaRepository<LecturaTemperatura, Long> {

    @Query("SELECT l FROM LecturaTemperatura l " +
            "WHERE l.cuartoId = :cuartoId " +
            "  AND l.timestamp >= :desde " +
            "  AND l.timestamp <= :hasta " +
            "ORDER BY l.timestamp ASC")
    List<LecturaTemperatura> findByRango(
            @Param("cuartoId") Integer cuartoId,
            @Param("desde")    OffsetDateTime desde,
            @Param("hasta")    OffsetDateTime hasta);
}