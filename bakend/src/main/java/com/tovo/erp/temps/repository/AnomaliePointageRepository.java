package com.tovo.erp.temps.repository;

import com.tovo.erp.temps.model.AnomaliePointage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AnomaliePointageRepository extends JpaRepository<AnomaliePointage, Long> {

    List<AnomaliePointage> findByIdEmployeAndDateAnomalieOrderByIdAsc(Long idEmploye, LocalDate dateAnomalie);

    boolean existsByIdEmployeAndDateAnomalieAndStatut(Long idEmploye, LocalDate dateAnomalie,
                                                      AnomaliePointage.Statut statut);

    boolean existsByDateAnomalieBetweenAndStatut(LocalDate debut, LocalDate fin,
                                                  AnomaliePointage.Statut statut);

    void deleteByIdEmployeAndDateAnomalie(Long idEmploye, LocalDate dateAnomalie);
}
