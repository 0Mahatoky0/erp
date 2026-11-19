package com.tovo.erp.discipline.repository;

import com.tovo.erp.discipline.model.ProcedureDisciplinaire;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProcedureRepository extends JpaRepository<ProcedureDisciplinaire, Long> {
    List<ProcedureDisciplinaire> findAllByOrderByDateOuvertureDesc();
    List<ProcedureDisciplinaire> findByIdEmployeOrderByDateOuvertureDesc(Long idEmploye);
    List<ProcedureDisciplinaire> findByIdIncident(Long idIncident);
    Optional<ProcedureDisciplinaire> findByIdIncidentAndIdEmploye(Long idIncident, Long idEmploye);
    boolean existsByIdIncidentAndIdEmploye(Long idIncident, Long idEmploye);
}
