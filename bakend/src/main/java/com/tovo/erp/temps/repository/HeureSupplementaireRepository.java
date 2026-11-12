package com.tovo.erp.temps.repository;

import com.tovo.erp.temps.model.HeureSupplementaire;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HeureSupplementaireRepository extends JpaRepository<HeureSupplementaire, Long> {

    Optional<HeureSupplementaire> findByIdEmployeAndSemaineDebut(Long idEmploye, LocalDate semaineDebut);

    List<HeureSupplementaire> findByIdEmployeAndSemaineDebutBetweenOrderBySemaineDebutDesc(
            Long idEmploye, LocalDate debut, LocalDate fin);
}
