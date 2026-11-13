package com.tovo.erp.conges.repository;

import com.tovo.erp.conges.model.StatutSuppleance;
import com.tovo.erp.conges.model.Suppleance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SuppleanceRepository extends JpaRepository<Suppleance, Long> {
    List<Suppleance> findByIdDemandeCongeOrderByDateCreationDesc(Long idDemandeConge);
    List<Suppleance> findByIdEmployeRemplacantAndStatutIn(Long idEmployeRemplacant,
                                                         Collection<StatutSuppleance> statuts);

    @Query("""
        select count(s) from Suppleance s
        where s.idEmployeRemplacant = :employe
          and s.statut in :statuts
          and s.dateDebut <= :fin and s.dateFin >= :debut
        """)
    long countChevauchement(@Param("employe") Long employe,
                            @Param("debut") LocalDate debut,
                            @Param("fin") LocalDate fin,
                            @Param("statuts") Collection<StatutSuppleance> statuts);

    @Query("""
        select max(s.dateFin) from Suppleance s
        where s.idEmployeRemplacant = :employe
          and s.statut in :statuts
        """)
    Optional<LocalDate> derniereDateFin(@Param("employe") Long employe,
                                        @Param("statuts") Collection<StatutSuppleance> statuts);

    List<Suppleance> findByIdEmployeAbsentOrderByDateDebutDesc(Long idEmployeAbsent);

    List<Suppleance> findAllByOrderByDateDebutDesc();
}
