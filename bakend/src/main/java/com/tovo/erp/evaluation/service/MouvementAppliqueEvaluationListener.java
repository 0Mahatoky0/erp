package com.tovo.erp.evaluation.service;

import com.tovo.erp.evaluation.dto.EvaluationDtos;
import com.tovo.erp.shared.MouvementAppliqueEvent;
import com.tovo.erp.shared.audit.JournalAuditService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

@Component
public class MouvementAppliqueEvaluationListener {

    private final ReferentielService referentiel;
    private final EvaluationService evaluations;
    private final JournalAuditService audit;

    public MouvementAppliqueEvaluationListener(ReferentielService referentiel,
                                               EvaluationService evaluations,
                                               JournalAuditService audit) {
        this.referentiel = referentiel;
        this.evaluations = evaluations;
        this.audit = audit;
    }

    @EventListener
    public void surMouvementApplique(MouvementAppliqueEvent evenement) {
        Long idPoste = evenement.idPosteDestination();
        if (idPoste == null) {
            return;
        }
        Map<Long, Integer> acquis = referentiel.niveauxAcquis(evenement.idEmploye());
        var dejaCouverts = evaluations.besoinsEmploye(evenement.idEmploye()).stream()
                .map(EvaluationDtos.BesoinFormationDto::idCompetence)
                .toList();
        for (EvaluationDtos.PosteCompetenceDto exigence : referentiel.posteCompetences(idPoste)) {
            int niveauAcquis = acquis.getOrDefault(exigence.idCompetence(), 0);
            if (exigence.niveauRequis() <= niveauAcquis || dejaCouverts.contains(exigence.idCompetence())) {
                continue;
            }
            LocalDate dateCible = evenement.dateEffet() == null
                    ? LocalDate.now().plusMonths(3) : evenement.dateEffet().plusMonths(3);
            evaluations.creerBesoin(new EvaluationDtos.BesoinFormationRequest(
                    evenement.idEmploye(), exigence.idCompetence(), exigence.niveauRequis(),
                    "MOBILITE", dateCible, null));
            audit.enregistrer("BESOIN_FORMATION_MOBILITE", "employe", evenement.idEmploye(), null,
                    exigence.idCompetence() + ":" + exigence.niveauRequis());
        }
    }
}
