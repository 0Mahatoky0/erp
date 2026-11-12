package com.tovo.erp.temps.repository;

import com.tovo.erp.temps.model.PeriodeTempsClose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PeriodeTempsCloseRepository extends JpaRepository<PeriodeTempsClose, Long> {

    Optional<PeriodeTempsClose> findByAnneeAndMois(int annee, int mois);

    @Query("""
        select p from PeriodeTempsClose p
        where p.dateDebut <= :date and p.dateFin >= :date
        """)
    Optional<PeriodeTempsClose> couvrant(@Param("date") LocalDate date);

    List<PeriodeTempsClose> findAllByOrderByAnneeDescMoisDesc();
}
