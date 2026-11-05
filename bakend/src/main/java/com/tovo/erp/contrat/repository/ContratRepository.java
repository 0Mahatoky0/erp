package com.tovo.erp.contrat.repository;

import com.tovo.erp.contrat.model.Contrat;
import com.tovo.erp.contrat.model.ContratEtat;
import com.tovo.erp.contrat.model.StatutExecutionContrat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContratRepository extends JpaRepository<Contrat, Long> {

    List<Contrat> findByIdEmployeOrderByDateDebutDesc(Long idEmploye);

    Optional<Contrat> findFirstByIdProposition(Long idProposition);

    List<Contrat> findAllByOrderByDateCreationDesc();

    List<Contrat> findByEtatOrderByDateDebutAsc(ContratEtat etat);

    List<Contrat> findByStatutExecutionOrderByDateDebutAsc(StatutExecutionContrat statutExecution);

    Optional<Contrat> findFirstByIdEmployeAndStatutExecutionOrderByDateDebutDesc(
            Long idEmploye, StatutExecutionContrat statutExecution);
}
