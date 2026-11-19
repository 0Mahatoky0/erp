package com.tovo.erp.discipline.repository;

import com.tovo.erp.discipline.model.IncidentEmploye;
import com.tovo.erp.discipline.model.IncidentEmployeId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentEmployeRepository extends JpaRepository<IncidentEmploye, IncidentEmployeId> {
    List<IncidentEmploye> findByIdIdIncident(Long idIncident);
    List<IncidentEmploye> findByIdIdEmploye(Long idEmploye);
    boolean existsByIdIdIncidentAndIdIdEmploye(Long idIncident, Long idEmploye);
}
