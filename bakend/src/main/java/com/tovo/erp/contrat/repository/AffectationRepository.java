package com.tovo.erp.contrat.repository;

import com.tovo.erp.contrat.model.Affectation;
import com.tovo.erp.contrat.model.AffectationStatut;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface AffectationRepository extends JpaRepository<Affectation, Long> {

    List<Affectation> findByIdEmployeOrderByDateDebutDesc(Long idEmploye);

    List<Affectation> findByIdContratOrderByDateDebutDesc(Long idContrat);

    @Query("""
            select a from Affectation a
            where a.idEmploye = :idEmploye
              and a.statut <> :statutAnnulee
              and a.dateDebut <= :fin
              and (a.dateFin is null or a.dateFin >= :debut)
            order by a.dateDebut desc
            """)
    List<Affectation> chevauchements(@Param("idEmploye") Long idEmploye,
                                     @Param("debut") LocalDate debut,
                                     @Param("fin") LocalDate fin,
                                     @Param("statutAnnulee") AffectationStatut statutAnnulee);

    @Query("""
            select count(a) from Affectation a
            where a.idPoste = :idPoste
              and a.statut in :statuts
              and a.dateDebut <= :fin
              and (a.dateFin is null or a.dateFin >= :debut)
            """)
    long compterAffectationsSurPeriode(@Param("idPoste") Long idPoste,
                                       @Param("debut") LocalDate debut,
                                       @Param("fin") LocalDate fin,
                                       @Param("statuts") Collection<AffectationStatut> statuts);
}
