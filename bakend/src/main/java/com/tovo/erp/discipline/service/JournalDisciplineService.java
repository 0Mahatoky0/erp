package com.tovo.erp.discipline.service;

import com.tovo.erp.discipline.dto.DisciplineDtos;
import com.tovo.erp.discipline.model.JournalDiscipline;
import com.tovo.erp.discipline.repository.JournalDisciplineRepository;
import com.tovo.erp.shared.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class JournalDisciplineService {

    private final JournalDisciplineRepository journal;

    public JournalDisciplineService(JournalDisciplineRepository journal) {
        this.journal = journal;
    }

    @Transactional
    public void enregistrer(Long utilisateurId, String action, String ressource, Long idRessource,
                            String motif, String details) {
        JournalDiscipline entree = new JournalDiscipline();
        entree.setDateAction(LocalDateTime.now());
        entree.setUtilisateurId(utilisateurId);
        entree.setAction(action);
        entree.setRessource(ressource);
        entree.setIdRessource(idRessource);
        entree.setMotif(motif);
        entree.setDetails(details);
        journal.save(entree);
    }

    @Transactional(readOnly = true)
    public List<DisciplineDtos.JournalDto> lire() {
        return journal.findAllByOrderByDateActionDesc().stream().map(DisciplineDtos.JournalDto::de).toList();
    }

    @Transactional(readOnly = true)
    public List<DisciplineDtos.JournalDto> parDossier(String ressource, Long idRessource, String motif) {
        exigerMotif(motif);
        return journal.findByRessourceAndIdRessourceOrderByDateActionDesc(ressource, idRessource).stream()
                .map(DisciplineDtos.JournalDto::de).toList();
    }

    public void exigerMotif(String motif) {
        if (motif == null || motif.isBlank()) {
            throw new BusinessException("MOTIF_CONSULTATION_OBLIGATOIRE",
                    "Le motif de consultation est obligatoire.");
        }
    }
}
