package com.tovo.erp.discipline.repository;

import com.tovo.erp.discipline.model.AccesDisciplinaire;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccesRepository extends JpaRepository<AccesDisciplinaire, Long> {
    List<AccesDisciplinaire> findByUtilisateurIdAndActifTrue(Long utilisateurId);
    List<AccesDisciplinaire> findByIdProcedureAndActifTrue(Long idProcedure);
    List<AccesDisciplinaire> findByIdIncidentAndActifTrue(Long idIncident);
}
