package com.tovo.erp.recrutement.repository;

import com.tovo.erp.recrutement.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EvaluationCandidatRepository extends JpaRepository<EvaluationCandidat, Long> {
    List<EvaluationCandidat> findByCandidatureIdCandidature(Long idCandidature);
    List<EvaluationCandidat> findByTypeEvaluation(TypeEvaluation typeEvaluation);
}
