package com.tovo.erp.conges.repository;

import com.tovo.erp.conges.model.DemandeConge;
import com.tovo.erp.conges.model.StatutDemandeConge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface DemandeCongeRepository extends JpaRepository<DemandeConge, Long> {
    List<DemandeConge> findAllByOrderByDateCreationDesc();
    List<DemandeConge> findByStatutOrderByDateCreationDesc(StatutDemandeConge statut);
    List<DemandeConge> findByIdEmployeOrderByDateCreationDesc(Long idEmploye);

    List<DemandeConge> findByStatutOrderByDateDebutAsc(StatutDemandeConge statut);
    List<DemandeConge> findByStatutInOrderByDateDebutAsc(List<StatutDemandeConge> statuts);

    @Query("""
        select count(d) > 0 from DemandeConge d
        where d.idEmploye = :employe and d.statut in :statuts
          and d.dateDebut <= :fin and d.dateFin >= :debut
        """)
    boolean existeChevauchement(@Param("employe") Long employe,
                                @Param("debut") LocalDate debut,
                                @Param("fin") LocalDate fin,
                                @Param("statuts") List<StatutDemandeConge> statuts);

    List<DemandeConge> findByIdEmployeInAndStatutInAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
            Collection<Long> idEmployes, Collection<StatutDemandeConge> statuts,
            LocalDate fin, LocalDate debut);

    @Query(value = """
        select exists (
            select 1
            from affectation a
            where a.id_employe = :employe
              and a.id_manager = :manager
              and a.principale = true
              and a.statut in ('ACTIVE', 'PLANIFIEE')
              and a.date_debut <= :fin
              and (a.date_fin is null or a.date_fin >= :debut)
        )
        """, nativeQuery = true)
    boolean managerAffecteAEmploye(@Param("manager") Long manager,
                                   @Param("employe") Long employe,
                                   @Param("debut") LocalDate debut,
                                   @Param("fin") LocalDate fin);
}
