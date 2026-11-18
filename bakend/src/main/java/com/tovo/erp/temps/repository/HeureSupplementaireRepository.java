package com.tovo.erp.temps.repository;

import com.tovo.erp.temps.model.HeureSupplementaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HeureSupplementaireRepository extends JpaRepository<HeureSupplementaire, Long> {

    Optional<HeureSupplementaire> findByIdEmployeAndSemaineDebut(Long idEmploye, LocalDate semaineDebut);

    List<HeureSupplementaire> findByIdEmployeAndSemaineDebutBetweenOrderBySemaineDebutDesc(
            Long idEmploye, LocalDate debut, LocalDate fin);

    @Query("select h from HeureSupplementaire h where h.semaineDebut between :debut and :fin "
            + "and h.statut = :statut order by h.idEmploye, h.semaineDebut")
    List<HeureSupplementaire> variablesPaie(@Param("debut") LocalDate debut,
                                             @Param("fin") LocalDate fin,
                                             @Param("statut") HeureSupplementaire.Statut statut);
}
