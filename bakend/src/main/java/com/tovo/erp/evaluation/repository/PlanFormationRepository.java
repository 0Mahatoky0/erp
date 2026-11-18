package com.tovo.erp.evaluation.repository;

import com.tovo.erp.evaluation.model.PlanFormation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanFormationRepository extends JpaRepository<PlanFormation, Long> {
    Optional<PlanFormation> findByAnneeAndIdDepartement(int annee, Long idDepartement);
    List<PlanFormation> findByAnneeOrderByIdDepartementAsc(int annee);
}
