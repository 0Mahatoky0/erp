package com.tovo.erp.discipline.repository;

import com.tovo.erp.discipline.model.SanctionDisciplinaire;
import com.tovo.erp.discipline.model.StatutSanction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface SanctionRepository extends JpaRepository<SanctionDisciplinaire, Long> {
    List<SanctionDisciplinaire> findAllByOrderByDateNotificationDesc();
    List<SanctionDisciplinaire> findByIdEmployeOrderByDateNotificationDesc(Long idEmploye);
    List<SanctionDisciplinaire> findByIdProcedure(Long idProcedure);
    boolean existsByIdProcedureAndStatutIn(Long idProcedure, Collection<StatutSanction> statuts);
    List<SanctionDisciplinaire> findByIdEmployeAndStatutInAndDateEffacementAfter(
            Long idEmploye, Collection<StatutSanction> statuts, java.time.LocalDate date);
    List<SanctionDisciplinaire> findByStatutInAndDateEffacementBefore(
            Collection<StatutSanction> statuts, java.time.LocalDate date);
}
