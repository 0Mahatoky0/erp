package com.tovo.erp.temps.repository;

import com.tovo.erp.temps.model.JourFerie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface JourFerieRepository extends JpaRepository<JourFerie, Long> {
    Optional<JourFerie> findByDate(LocalDate date);
    List<JourFerie> findByDateBetweenOrderByDateAsc(LocalDate debut, LocalDate fin);
    List<JourFerie> findByRecurrentTrue();
}
