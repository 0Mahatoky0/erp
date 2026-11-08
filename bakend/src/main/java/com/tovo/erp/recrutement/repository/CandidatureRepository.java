package com.tovo.erp.recrutement.repository;

import com.tovo.erp.recrutement.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.Collection;
import java.util.List;

public interface CandidatureRepository extends JpaRepository<Candidature, Long> {
    boolean existsByOffreIdOffreAndCandidatIdCandidat(Long idOffre, Long idCandidat);
    boolean existsByOffreIdOffre(Long idOffre);
    long countByOffreIdOffre(Long idOffre);
    long countByOffreIdOffreAndStatut(Long idOffre, CandidatureStatut statut);
    @EntityGraph(attributePaths = {"offre", "candidat"})
    List<Candidature> findByOffreIdOffre(Long idOffre);
    @EntityGraph(attributePaths = {"offre", "candidat"})
    List<Candidature> findByOffreIdOffreAndStatut(Long idOffre, CandidatureStatut statut);
    @EntityGraph(attributePaths = {"offre", "candidat"})
    List<Candidature> findByCandidatIdCandidat(Long idCandidat);
    @EntityGraph(attributePaths = {"offre", "candidat"})
    List<Candidature> findByCandidatIdCandidatOrderByDateCandidatureDesc(Long idCandidat);
    @EntityGraph(attributePaths = {"offre", "candidat"})
    List<Candidature> findAllByStatutOrderByDateCandidatureDesc(CandidatureStatut statut);
    @EntityGraph(attributePaths = {"offre", "candidat"})
    List<Candidature> findAllByOrderByDateCandidatureDesc();
    @EntityGraph(attributePaths = {"offre", "candidat"})
    List<Candidature> findAllByIdCandidatureIn(Collection<Long> ids);
}
