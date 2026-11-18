package com.tovo.erp.evaluation.repository;

import com.tovo.erp.evaluation.model.CampagneEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CampagneEvaluationRepository extends JpaRepository<CampagneEvaluation, Long> {
    List<CampagneEvaluation> findAllByOrderByDateOuvertureDesc();
    List<CampagneEvaluation> findByStatutOrderByDateOuvertureDesc(String statut);
}
