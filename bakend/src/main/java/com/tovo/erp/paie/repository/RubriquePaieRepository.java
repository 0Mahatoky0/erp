package com.tovo.erp.paie.repository;

import com.tovo.erp.paie.model.RubriquePaie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RubriquePaieRepository extends JpaRepository<RubriquePaie, Long> {
    List<RubriquePaie> findByDateEffetLessThanEqualOrderByOrdreAsc(LocalDate date);

    Optional<RubriquePaie> findByCodeAndDateEffetLessThanEqualOrderByDateEffetDesc(String code, LocalDate date);
}
