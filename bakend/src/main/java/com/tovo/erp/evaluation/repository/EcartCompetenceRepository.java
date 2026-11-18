package com.tovo.erp.evaluation.repository;

import com.tovo.erp.evaluation.model.EcartCompetence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EcartCompetenceRepository extends JpaRepository<EcartCompetence, Long> {
    List<EcartCompetence> findByIdEmployeOrderByPrioriteScoreDesc(Long idEmploye);
    List<EcartCompetence> findByIdEvaluation(Long idEvaluation);
}
