package com.tovo.erp.embauche.repository;

import com.tovo.erp.embauche.model.VersionProposition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VersionPropositionRepository extends JpaRepository<VersionProposition, Long> {

    List<VersionProposition> findByPropositionIdOrderByNumeroVersionDesc(Long idProposition);
}
