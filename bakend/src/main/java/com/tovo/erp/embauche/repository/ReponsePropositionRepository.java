package com.tovo.erp.embauche.repository;

import com.tovo.erp.embauche.model.ReponseProposition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReponsePropositionRepository extends JpaRepository<ReponseProposition, Long> {
    List<ReponseProposition> findByPropositionIdPropositionEmbaucheOrderByDateReponseDesc(Long idProposition);
}
