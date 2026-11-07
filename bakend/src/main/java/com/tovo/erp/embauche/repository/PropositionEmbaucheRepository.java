package com.tovo.erp.embauche.repository;

import com.tovo.erp.embauche.model.PropositionEmbauche;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PropositionEmbaucheRepository extends JpaRepository<PropositionEmbauche, Long> {
    boolean existsByIdCandidature(Long idCandidature);

    List<PropositionEmbauche> findAllByOrderByDatePropositionDesc();

    List<PropositionEmbauche> findByIdCandidatOrderByDatePropositionDesc(Long idCandidat);

    @Override
    Optional<PropositionEmbauche> findById(Long id);
}
