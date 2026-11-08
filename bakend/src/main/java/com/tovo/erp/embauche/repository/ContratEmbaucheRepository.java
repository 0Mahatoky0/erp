package com.tovo.erp.embauche.repository;

import com.tovo.erp.embauche.model.ContratEmbauche;
import com.tovo.erp.embauche.model.ContratStatut;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContratEmbaucheRepository extends JpaRepository<ContratEmbauche, Long> {
    boolean existsByPropositionIdPropositionEmbauche(Long idProposition);
    boolean existsByIdCandidatAndStatutNot(Long idCandidat, ContratStatut statut);
    Optional<ContratEmbauche> findByPropositionIdPropositionEmbauche(Long idProposition);
    List<ContratEmbauche> findAllByOrderByDateCreationDesc();
    List<ContratEmbauche> findByIdCandidatOrderByDateCreationDesc(Long idCandidat);
}
