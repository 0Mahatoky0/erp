package com.tovo.erp.recrutement.repository;

import com.tovo.erp.recrutement.domain.DecisionCandidature;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DecisionCandidatureRepository extends JpaRepository<DecisionCandidature, Long> {
    List<DecisionCandidature> findByCandidatureIdCandidatureOrderByDateDecisionDesc(Long idCandidature);
}
