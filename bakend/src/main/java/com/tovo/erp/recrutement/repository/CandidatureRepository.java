package com.tovo.erp.recrutement.repository;

import com.tovo.erp.recrutement.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CandidatureRepository extends JpaRepository<Candidature, Long> {
    boolean existsByOffreIdOffreAndCandidatIdCandidat(Long idOffre, Long idCandidat);
    List<Candidature> findByOffreIdOffre(Long idOffre);
    List<Candidature> findByOffreIdOffreAndStatut(Long idOffre, CandidatureStatut statut);
    List<Candidature> findByCandidatIdCandidat(Long idCandidat);
}
