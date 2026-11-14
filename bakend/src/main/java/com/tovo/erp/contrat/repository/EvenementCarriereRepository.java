package com.tovo.erp.contrat.repository;

import com.tovo.erp.contrat.model.EvenementCarriere;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvenementCarriereRepository extends JpaRepository<EvenementCarriere, Long> {
    List<EvenementCarriere> findByIdEmployeOrderByDateEffetDesc(Long idEmploye);

    boolean existsByIdEmployeAndType(Long idEmploye, com.tovo.erp.contrat.model.TypeEvenementCarriere type);
}
