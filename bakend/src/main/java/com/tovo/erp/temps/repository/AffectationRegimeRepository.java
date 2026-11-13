package com.tovo.erp.temps.repository;

import com.tovo.erp.temps.model.AffectationRegime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AffectationRegimeRepository extends JpaRepository<AffectationRegime, Long> {

    List<AffectationRegime> findByIdEmployeOrderByDateDebutDesc(Long idEmploye);

    boolean existsByIdRegime(Long idRegime);

    @Query("""
        select count(a) from AffectationRegime a
        where a.idEmploye = :idEmploye
          and a.dateDebut <= :fin
          and (a.dateFin is null or a.dateFin >= :debut)
        """)
    long countChevauchement(@Param("idEmploye") Long idEmploye,
                            @Param("debut") LocalDate debut,
                            @Param("fin") LocalDate fin);

    @Query("""
        select a from AffectationRegime a
        where a.idEmploye = :idEmploye
          and a.dateDebut <= :date
          and (a.dateFin is null or a.dateFin >= :date)
        order by a.dateDebut desc
        """)
    List<AffectationRegime> courantes(@Param("idEmploye") Long idEmploye,
                                      @Param("date") LocalDate date);
}
