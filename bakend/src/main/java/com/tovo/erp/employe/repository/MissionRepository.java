package com.tovo.erp.employe.repository;

import com.tovo.erp.employe.model.Mission;
import com.tovo.erp.employe.model.StatutMission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface MissionRepository extends JpaRepository<Mission, Long> {

    List<Mission> findByIdEmployeOrderByDateDebutDesc(Long idEmploye);

    List<Mission> findByStatutNotAndDateFinPrevueBefore(StatutMission statut, LocalDate date);
}
