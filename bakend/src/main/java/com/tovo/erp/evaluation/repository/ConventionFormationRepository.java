package com.tovo.erp.evaluation.repository;

import com.tovo.erp.evaluation.model.ConventionFormation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConventionFormationRepository extends JpaRepository<ConventionFormation, Long> {
    Optional<ConventionFormation> findByIdInscription(Long idInscription);
}
