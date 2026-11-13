package com.tovo.erp.employe.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Tache planifiee qui recalcule les alertes du dossier administratif (bloc 5).
 * Le calcul est idempotent : il peut etre rejoue sans creer de doublons.
 */
@Component
public class AlerteDossierJob {

    private static final Logger LOG = LoggerFactory.getLogger(AlerteDossierJob.class);

    private final AlerteService alertes;

    public AlerteDossierJob(AlerteService alertes) {
        this.alertes = alertes;
    }

    @Scheduled(initialDelayString = "${erp.employe.alertes-delai-initial-ms:60000}",
            fixedDelayString = "${erp.employe.alertes-verification-ms:86400000}")
    public void recalculer() {
        int nombre = alertes.recalculerToutes();
        if (nombre > 0) {
            LOG.info("Dossier administratif : {} alerte(s) recalculee(s).", nombre);
        }
    }
}
