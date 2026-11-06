package com.tovo.erp.contrat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Planificateur des echeances du bloc 4 : activation, fin de contrat,
 * non-presentation et alertes. La logique reste dans le service metier ;
 * ce composant ne fait que declencher un passage idempotent.
 */
@Component
public class ContratEcheanceJob {

    private static final Logger LOG = LoggerFactory.getLogger(ContratEcheanceJob.class);

    private final ContratService contrats;

    public ContratEcheanceJob(ContratService contrats) {
        this.contrats = contrats;
    }

    @Scheduled(fixedDelayString = "${erp.contrat.verification-echeances-ms:3600000}")
    public void traiterEcheances() {
        ContratService.ResultatEcheances resultat = contrats.traiterEcheances();
        if (resultat.actives() + resultat.termines() + resultat.absences() > 0) {
            LOG.info("Echeances contrats : {} active(s), {} termine(s), {} absence(s).",
                    resultat.actives(), resultat.termines(), resultat.absences());
        }
    }
}
