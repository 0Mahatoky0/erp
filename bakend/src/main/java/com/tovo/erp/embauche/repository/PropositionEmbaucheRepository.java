package com.tovo.erp.embauche.repository;

import com.tovo.erp.embauche.model.PropositionEmbauche;
import com.tovo.erp.embauche.model.PropositionStatut;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PropositionEmbaucheRepository extends JpaRepository<PropositionEmbauche, Long> {

    Optional<PropositionEmbauche> findByTokenAcceptation(String token);

    Optional<PropositionEmbauche> findFirstByIdCandidatureAndActiveTrue(Long idCandidature);

    boolean existsByIdCandidatureAndActiveTrue(Long idCandidature);

    List<PropositionEmbauche> findAllByOrderByDateCreationDesc();

    List<PropositionEmbauche> findByStatutOrderByDateCreationDesc(PropositionStatut statut);

    List<PropositionEmbauche> findByActiveTrueAndStatutAndDateExpirationBefore(
            PropositionStatut statut, LocalDateTime date);
}
