package com.tovo.erp.evaluation.repository;

import com.tovo.erp.evaluation.model.BesoinFormation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BesoinFormationRepository extends JpaRepository<BesoinFormation, Long> {
    List<BesoinFormation> findByIdEmployeOrderByDateCreationDesc(Long idEmploye);
    List<BesoinFormation> findByStatutOrderByDateCreationAsc(String statut);
}
