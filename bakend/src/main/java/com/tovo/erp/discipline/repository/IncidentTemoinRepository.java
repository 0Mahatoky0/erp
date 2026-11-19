package com.tovo.erp.discipline.repository;

import com.tovo.erp.discipline.model.IncidentTemoin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentTemoinRepository extends JpaRepository<IncidentTemoin, Long> {
    List<IncidentTemoin> findByIdIncident(Long idIncident);
}
