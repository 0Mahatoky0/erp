package com.tovo.erp.evaluation.repository;

import com.tovo.erp.evaluation.model.InscriptionFormation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InscriptionFormationRepository extends JpaRepository<InscriptionFormation, Long> {
    List<InscriptionFormation> findByIdSessionAndStatutOrderByDateDemandeAsc(Long idSession, String statut);
    List<InscriptionFormation> findByIdEmployeOrderByDateDemandeDesc(Long idEmploye);
    List<InscriptionFormation> findByIdSession(Long idSession);
}
