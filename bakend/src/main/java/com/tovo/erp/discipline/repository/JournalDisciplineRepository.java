package com.tovo.erp.discipline.repository;

import com.tovo.erp.discipline.model.JournalDiscipline;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JournalDisciplineRepository extends JpaRepository<JournalDiscipline, Long> {
    List<JournalDiscipline> findAllByOrderByDateActionDesc();
    List<JournalDiscipline> findByRessourceAndIdRessourceOrderByDateActionDesc(String ressource, Long idRessource);
    List<JournalDiscipline> findByUtilisateurIdOrderByDateActionDesc(Long utilisateurId);
}
