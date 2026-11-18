package com.tovo.erp.paie.repository;

import com.tovo.erp.paie.model.BaremeIrsa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface BaremeIrsaRepository extends JpaRepository<BaremeIrsa, Long> {
    List<BaremeIrsa> findByDateEffetLessThanEqualOrderByBorneInfAsc(LocalDate date);
}
