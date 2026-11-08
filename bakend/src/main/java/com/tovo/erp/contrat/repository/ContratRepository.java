package com.tovo.erp.contrat.repository;

import com.tovo.erp.contrat.model.Contrat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContratRepository extends JpaRepository<Contrat, Long> {

    List<Contrat> findByIdEmployeOrderByDateDebutDesc(Long idEmploye);

    Optional<Contrat> findFirstByIdProposition(Long idProposition);
}
