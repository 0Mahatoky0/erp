package com.tovo.erp.evaluation.repository;

import com.tovo.erp.evaluation.model.EmployeCompetence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeCompetenceRepository extends JpaRepository<EmployeCompetence, Long> {
    List<EmployeCompetence> findByIdEmployeOrderByDateEvaluationDesc(Long idEmploye);
    Optional<EmployeCompetence> findTopByIdEmployeAndIdCompetenceOrderByDateEvaluationDesc(
            Long idEmploye, Long idCompetence);
}
