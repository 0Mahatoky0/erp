package com.tovo.erp.embauche.repository;

import com.tovo.erp.embauche.model.HistoriqueProposition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoriquePropositionRepository extends JpaRepository<HistoriqueProposition, Long> {

    List<HistoriqueProposition> findByPropositionIdOrderByDateActionAsc(Long idProposition);
}
