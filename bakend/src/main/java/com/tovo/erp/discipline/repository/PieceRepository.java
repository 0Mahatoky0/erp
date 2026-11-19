package com.tovo.erp.discipline.repository;

import com.tovo.erp.discipline.model.PieceDisciplinaire;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PieceRepository extends JpaRepository<PieceDisciplinaire, Long> {
    List<PieceDisciplinaire> findByIdIncidentOrderByDateAjoutAsc(Long idIncident);
    List<PieceDisciplinaire> findByIdProcedureOrderByDateAjoutAsc(Long idProcedure);
    List<PieceDisciplinaire> findByIdIncidentOrIdProcedureOrderByDateAjoutAsc(Long idIncident, Long idProcedure);
}
