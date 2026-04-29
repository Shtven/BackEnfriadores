package com.codespace.simulator.repositories;

import com.codespace.simulator.entities.IntervencionManual;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IntervencionManualRepository extends JpaRepository<IntervencionManual, Long> {}