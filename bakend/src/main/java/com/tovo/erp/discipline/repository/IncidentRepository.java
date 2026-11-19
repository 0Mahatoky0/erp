package com.tovo.erp.discipline.repository;

import com.tovo.erp.discipline.model.Incident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {
    List<Incident> findAllByOrderByDateDeclarationDesc();
    List<Incident> findByDeclarantIdOrderByDateDeclarationDesc(Long declarantId);
}
