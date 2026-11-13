package com.tovo.erp.employe.repository;

import com.tovo.erp.employe.model.DemandeModification;
import com.tovo.erp.employe.model.StatutDemandeModification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DemandeModificationRepository extends JpaRepository<DemandeModification, Long> {

    List<DemandeModification> findByIdEmployeOrderByDateCreationDesc(Long idEmploye);

    List<DemandeModification> findByStatutOrderByDateCreationAsc(StatutDemandeModification statut);
}
