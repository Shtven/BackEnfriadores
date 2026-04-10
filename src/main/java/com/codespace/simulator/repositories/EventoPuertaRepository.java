package com.codespace.simulator.repositories;

import com.codespace.simulator.entities.EventoPuerta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EventoPuertaRepository extends JpaRepository<EventoPuerta, Long> {

    List<EventoPuerta> findByCuartoId(Integer cuartoId);

    List<EventoPuerta> findByCuartoIdAndTipo(Integer cuartoId, String tipo);
}