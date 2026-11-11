package com.tovo.erp.conges.repository;

import com.tovo.erp.conges.model.SoldeConge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SoldeCongeRepository extends JpaRepository<SoldeConge, Long> {
    List<SoldeConge> findByIdEmployeAndAnnee(Long idEmploye, int annee);
    Optional<SoldeConge> findByIdEmployeAndIdTypeCongeAndAnnee(Long idEmploye, Long idTypeConge, int annee);
}
