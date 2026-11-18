package com.tovo.erp.evaluation.repository;

import com.tovo.erp.evaluation.model.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
    Optional<Evaluation> findTopByIdEmployeAndIdCampagneOrderByVersionDesc(Long idEmploye, Long idCampagne);
    List<Evaluation> findByIdCampagneOrderByIdEmployeAsc(Long idCampagne);
    List<Evaluation> findByIdEmployeOrderByDateCreationDesc(Long idEmploye);
}
