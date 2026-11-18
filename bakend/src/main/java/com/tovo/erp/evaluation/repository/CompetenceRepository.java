package com.tovo.erp.evaluation.repository;

import com.tovo.erp.evaluation.model.Competence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompetenceRepository extends JpaRepository<Competence, Long> {
    List<Competence> findAllByActifTrueOrderByLibelleAsc();
    Optional<Competence> findByCode(String code);
}
