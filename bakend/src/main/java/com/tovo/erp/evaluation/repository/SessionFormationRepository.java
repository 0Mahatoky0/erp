package com.tovo.erp.evaluation.repository;

import com.tovo.erp.evaluation.model.SessionFormation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SessionFormationRepository extends JpaRepository<SessionFormation, Long> {
    List<SessionFormation> findAllByOrderByDateDebutDesc();
    List<SessionFormation> findByStatutOrderByDateDebutAsc(String statut);
}
