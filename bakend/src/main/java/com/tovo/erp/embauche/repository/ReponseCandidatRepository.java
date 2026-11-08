package com.tovo.erp.embauche.repository;

import com.tovo.erp.embauche.model.ReponseCandidat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReponseCandidatRepository extends JpaRepository<ReponseCandidat, Long> {

    List<ReponseCandidat> findByPropositionIdOrderByDateReponseDesc(Long idProposition);
}
