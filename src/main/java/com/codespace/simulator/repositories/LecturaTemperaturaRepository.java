package com.codespace.simulator.repositories;

import com.codespace.simulator.entities.LecturaTemperatura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;

@Repository
public interface LecturaTemperaturaRepository extends JpaRepository<LecturaTemperatura, Long> {

    List<LecturaTemperatura> findByCuartoId(Integer cuartoId);

    List<LecturaTemperatura> findByCuartoIdAndTimestampEventoBetween(
            Integer cuartoId, Instant desde, Instant hasta);

    List<LecturaTemperatura> findBySensorId(String sensorId);
}