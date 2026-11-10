package com.tovo.erp.temps.repository;

import com.tovo.erp.temps.model.PlanningTravail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PlanningTravailRepository extends JpaRepository<PlanningTravail, Long> {

    List<PlanningTravail> findByIdEmployeOrderByDateDebutDesc(Long idEmploye);

    Optional<PlanningTravail> findFirstByIdEmployeAndDateDebutLessThanEqualAndDateFinIsNullOrderByDateDebutDesc(
            Long idEmploye, LocalDate date);

    Optional<PlanningTravail> findFirstByIdEmployeAndDateDebutLessThanEqualAndDateFinGreaterThanEqualOrderByDateDebutDesc(
            Long idEmploye, LocalDate debut, LocalDate fin);
}
