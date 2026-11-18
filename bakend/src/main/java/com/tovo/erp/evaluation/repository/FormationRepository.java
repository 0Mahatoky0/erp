package com.tovo.erp.evaluation.repository;

import com.tovo.erp.evaluation.model.Formation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FormationRepository extends JpaRepository<Formation, Long> {
    List<Formation> findAllByOrderByTitreAsc();
    Optional<Formation> findByCode(String code);
}
