package com.tovo.erp.recrutement.repository;

import com.tovo.erp.recrutement.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EntretienRepository extends JpaRepository<Entretien, Long> {
    List<Entretien> findByCandidatureIdCandidature(Long idCandidature);
    List<Entretien> findByStatut(EntretienStatut statut);
}
