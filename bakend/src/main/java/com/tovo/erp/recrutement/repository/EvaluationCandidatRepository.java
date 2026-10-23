package com.tovo.erp.recrutement.repository;

import com.tovo.erp.recrutement.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;

public interface EvaluationCandidatRepository extends JpaRepository<EvaluationCandidat, Long> {
    @EntityGraph(attributePaths = {"candidature", "entretien", "passage"})
    List<EvaluationCandidat> findByCandidatureIdCandidature(Long idCandidature);
    List<EvaluationCandidat> findByTypeEvaluation(TypeEvaluation typeEvaluation);
}
