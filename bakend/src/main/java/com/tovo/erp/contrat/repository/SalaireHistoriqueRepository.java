package com.tovo.erp.contrat.repository;

import com.tovo.erp.contrat.model.SalaireHistorique;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SalaireHistoriqueRepository extends JpaRepository<SalaireHistorique, Long> {
    List<SalaireHistorique> findByIdEmployeOrderByDateEffetDesc(Long idEmploye);

    Optional<SalaireHistorique> findFirstByIdEmployeAndDateFinIsNullOrderByDateEffetDesc(Long idEmploye);

    Optional<SalaireHistorique> findFirstByIdEmployeOrderByDateEffetDesc(Long idEmploye);

    boolean existsByIdContrat(Long idContrat);
}
