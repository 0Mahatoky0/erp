package com.tovo.erp.evaluation.repository;

import com.tovo.erp.evaluation.model.Objectif;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ObjectifRepository extends JpaRepository<Objectif, Long> {
    List<Objectif> findByIdEmployeAndIdCampagneOrderByVersionDescIdAsc(Long idEmploye, Long idCampagne);
    List<Objectif> findByIdCampagneOrderByIdEmployeAsc(Long idCampagne);
}
