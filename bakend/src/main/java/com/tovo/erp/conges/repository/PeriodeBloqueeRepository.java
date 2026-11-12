package com.tovo.erp.conges.repository;

import com.tovo.erp.conges.model.PeriodeBloquee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PeriodeBloqueeRepository extends JpaRepository<PeriodeBloquee, Long> {

    @Query("""
        select p from PeriodeBloquee p
        where p.actif = true and p.dateDebut <= :fin and p.dateFin >= :debut
        order by p.dateDebut
        """)
    List<PeriodeBloquee> chevauchant(@Param("debut") LocalDate debut, @Param("fin") LocalDate fin);
}
