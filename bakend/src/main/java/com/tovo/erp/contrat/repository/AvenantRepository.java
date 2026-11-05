package com.tovo.erp.contrat.repository;

import com.tovo.erp.contrat.model.Avenant;
import com.tovo.erp.contrat.model.AvenantEtat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AvenantRepository extends JpaRepository<Avenant, Long> {

    List<Avenant> findByIdContratOrderByIdAsc(Long idContrat);

    long countByIdContratAndEtatNot(Long idContrat, AvenantEtat etat);
}
