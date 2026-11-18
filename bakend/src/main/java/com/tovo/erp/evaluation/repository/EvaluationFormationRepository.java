package com.tovo.erp.evaluation.repository;

import com.tovo.erp.evaluation.model.EvaluationFormation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvaluationFormationRepository extends JpaRepository<EvaluationFormation, Long> {
    List<EvaluationFormation> findByIdInscriptionOrderByDateEvaluationDesc(Long idInscription);
}
