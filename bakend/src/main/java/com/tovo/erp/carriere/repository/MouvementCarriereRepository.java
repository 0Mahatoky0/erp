package com.tovo.erp.carriere.repository;

import com.tovo.erp.carriere.model.MouvementCarriere;
import com.tovo.erp.carriere.model.StatutMouvementCarriere;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface MouvementCarriereRepository extends JpaRepository<MouvementCarriere, Long> {

    List<MouvementCarriere> findByIdEmployeOrderByDateCreationDesc(Long idEmploye);

    List<MouvementCarriere> findByStatut(StatutMouvementCarriere statut);

    List<MouvementCarriere> findByStatutAndDateEffetLessThanEqual(
            StatutMouvementCarriere statut, LocalDate date);
}
