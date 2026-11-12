package com.tovo.erp.temps.repository;

import com.tovo.erp.temps.model.JourTravaille;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface JourTravailleRepository extends JpaRepository<JourTravaille, Long> {

    Optional<JourTravaille> findByIdEmployeAndDateJour(Long idEmploye, LocalDate dateJour);

    List<JourTravaille> findByIdEmployeAndDateJourBetweenOrderByDateJourAsc(
            Long idEmploye, LocalDate debut, LocalDate fin);
}
