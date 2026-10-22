package com.tovo.erp.recrutement.repository;

import com.tovo.erp.recrutement.domain.OffreEmploi;
import com.tovo.erp.recrutement.domain.OffreStatut;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OffreEmploiRepository extends JpaRepository<OffreEmploi, Long> {
    Optional<OffreEmploi> findByReference(String reference);
    List<OffreEmploi> findByStatut(OffreStatut statut);
    List<OffreEmploi> findByIdDemandePoste(Long idDemandePoste);
}
