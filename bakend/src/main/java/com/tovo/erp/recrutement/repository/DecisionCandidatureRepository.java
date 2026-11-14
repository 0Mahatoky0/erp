package com.tovo.erp.recrutement.repository;

import com.tovo.erp.recrutement.model.DecisionCandidature;
import com.tovo.erp.recrutement.model.DecisionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DecisionCandidatureRepository extends JpaRepository<DecisionCandidature, Long> {
    List<DecisionCandidature> findByCandidatureIdCandidatureOrderByDateDecisionDesc(Long idCandidature);

    /** Dernier rang attribue dans la liste d'attente (FIFO, RG-B2). */
    @Query("""
            select coalesce(max(d.rangListeAttente), 0) from DecisionCandidature d
            where d.decision = :decision
            """)
    Integer dernierRangListeAttente(@Param("decision") DecisionType decision);
}
