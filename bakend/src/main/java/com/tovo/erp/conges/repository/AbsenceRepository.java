package com.tovo.erp.conges.repository;

import com.tovo.erp.conges.model.Absence;
import com.tovo.erp.conges.model.StatutAbsence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface AbsenceRepository extends JpaRepository<Absence, Long> {
    List<Absence> findAllByOrderByDateDebutDesc();
    List<Absence> findByStatutOrderByDateDebutDesc(StatutAbsence statut);
    List<Absence> findByIdEmployeOrderByDateDebutDesc(Long idEmploye);

    List<Absence> findByIdEmployeInAndStatutInAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
            Collection<Long> idEmployes, Collection<StatutAbsence> statuts,
            LocalDate fin, LocalDate debut);

    @Query("""
        select count(a) > 0 from Absence a
        where a.idEmploye = :employe and a.statut in :statuts
          and a.dateDebut <= :fin and a.dateFin >= :debut
        """)
    boolean existeChevauchement(@Param("employe") Long employe,
                                @Param("debut") LocalDate debut,
                                @Param("fin") LocalDate fin,
                                @Param("statuts") List<StatutAbsence> statuts);
}
