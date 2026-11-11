package com.tovo.erp.paie.repository;

import com.tovo.erp.paie.model.PeriodePaie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

public interface PeriodePaieRepository extends JpaRepository<PeriodePaie, Long> {
    Optional<PeriodePaie> findByMois(LocalDate mois);
    List<PeriodePaie> findAllByOrderByMoisDesc();
}
