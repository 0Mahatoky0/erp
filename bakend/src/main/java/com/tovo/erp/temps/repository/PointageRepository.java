package com.tovo.erp.temps.repository;

import com.tovo.erp.temps.model.Pointage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PointageRepository extends JpaRepository<Pointage, Long> {

    Optional<Pointage> findByIdEmployeAndDatePointage(Long idEmploye, LocalDate datePointage);

    List<Pointage> findByIdEmployeAndDatePointageBetweenOrderByDatePointageDesc(
            Long idEmploye, LocalDate debut, LocalDate fin);
}
