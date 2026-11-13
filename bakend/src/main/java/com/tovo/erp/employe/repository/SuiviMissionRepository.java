package com.tovo.erp.employe.repository;

import com.tovo.erp.employe.model.SuiviMission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SuiviMissionRepository extends JpaRepository<SuiviMission, Long> {

    List<SuiviMission> findByIdMissionOrderByDateSuiviDesc(Long idMission);
}
