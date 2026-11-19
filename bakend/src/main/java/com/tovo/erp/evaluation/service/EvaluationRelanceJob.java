package com.tovo.erp.evaluation.service;

import com.tovo.erp.evaluation.model.CampagneEvaluation;
import com.tovo.erp.evaluation.model.Evaluation;
import com.tovo.erp.evaluation.repository.CampagneEvaluationRepository;
import com.tovo.erp.evaluation.repository.EvaluationRepository;
import com.tovo.erp.shared.audit.JournalAuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class EvaluationRelanceJob {
    private static final Logger LOG = LoggerFactory.getLogger(EvaluationRelanceJob.class);

    private final CampagneEvaluationRepository campagnes;
    private final EvaluationRepository evaluations;
    private final JournalAuditService audit;

    public EvaluationRelanceJob(CampagneEvaluationRepository campagnes,
                                EvaluationRepository evaluations,
                                JournalAuditService audit) {
        this.campagnes = campagnes;
        this.evaluations = evaluations;
        this.audit = audit;
    }

    @Scheduled(initialDelayString = "${erp.evaluation.relance-delai-initial-ms:60000}",
            fixedDelayString = "${erp.evaluation.relance-verification-ms:86400000}")
    public void relancer() {
        LocalDate aujourdHui = LocalDate.now();
        int relances = 0;
        for (CampagneEvaluation campagne : campagnes.findAll()) {
            if ("CLOTUREE".equals(campagne.getStatut())) {
                continue;
            }
            LocalDate cloture = campagne.getDateCloture();
            if (aujourdHui.equals(cloture.minusDays(60))
                    || aujourdHui.equals(cloture.minusDays(30))
                    || !aujourdHui.isBefore(cloture.minusDays(30))
                            && aujourdHui.isBefore(cloture)
                            && aujourdHui.getDayOfWeek().getValue() == 1) {
                audit.enregistrer("CAMPAGNE_RELANCE", "campagne_evaluation", campagne.getId(),
                        null, aujourdHui.toString());
                relances++;
            }
        }

        for (Evaluation evaluation : evaluations.findAll()) {
            if (evaluation.getDateEntretien() != null
                    && evaluation.getDateEntretien().toLocalDate().plusDays(90).equals(aujourdHui)) {
                audit.enregistrer("ALERTE_MANAGER_J90", "evaluation", evaluation.getId(),
                        null, "Entretien dépassé de 90 jours");
                relances++;
            }
        }
        if (relances > 0) {
            LOG.info("Évaluations : {} relance(s) ou alerte(s) créée(s).", relances);
        }
    }
}
