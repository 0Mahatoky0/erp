package com.tovo.erp.temps.repository;

import com.tovo.erp.temps.model.HoraireTravail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HoraireTravailRepository extends JpaRepository<HoraireTravail, Long> {
    List<HoraireTravail> findByIdRegimeOrderByJourSemaineAscHeureDebutAsc(Long idRegime);
    void deleteByIdRegime(Long idRegime);
}
