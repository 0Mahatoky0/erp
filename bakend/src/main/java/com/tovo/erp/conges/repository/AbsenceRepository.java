package com.tovo.erp.conges.repository;

import com.tovo.erp.conges.model.Absence;
import com.tovo.erp.conges.model.StatutAbsence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AbsenceRepository extends JpaRepository<Absence, Long> {
    List<Absence> findAllByOrderByDateDebutDesc();
    List<Absence> findByStatutOrderByDateDebutDesc(StatutAbsence statut);
}
