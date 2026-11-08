package com.tovo.erp.contrat.repository;

import com.tovo.erp.contrat.model.Affectation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AffectationRepository extends JpaRepository<Affectation, Long> {

    List<Affectation> findByIdEmployeOrderByDateDebutDesc(Long idEmploye);

    List<Affectation> findByIdContratOrderByDateDebutDesc(Long idContrat);
}
