package com.tovo.erp.conges.repository;

import com.tovo.erp.conges.model.DemandeConge;
import com.tovo.erp.conges.model.StatutDemandeConge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DemandeCongeRepository extends JpaRepository<DemandeConge, Long> {
    List<DemandeConge> findAllByOrderByDateCreationDesc();
    List<DemandeConge> findByStatutOrderByDateCreationDesc(StatutDemandeConge statut);
    List<DemandeConge> findByIdEmployeOrderByDateCreationDesc(Long idEmploye);

    List<DemandeConge> findByStatutOrderByDateDebutAsc(StatutDemandeConge statut);

    @Query("""
        select count(d) > 0 from DemandeConge d
        where d.idEmploye = :employe and d.statut in :statuts
          and d.dateDebut <= :fin and d.dateFin >= :debut
        """)
    boolean existeChevauchement(@Param("employe") Long employe,
                                @Param("debut") LocalDate debut,
                                @Param("fin") LocalDate fin,
                                @Param("statuts") List<StatutDemandeConge> statuts);
}
