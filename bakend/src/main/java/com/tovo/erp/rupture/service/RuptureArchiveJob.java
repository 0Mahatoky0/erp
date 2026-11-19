package com.tovo.erp.rupture.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RuptureArchiveJob {

    private final DossierRuptureService dossiers;

    public RuptureArchiveJob(DossierRuptureService dossiers) {
        this.dossiers = dossiers;
    }

    @Scheduled(cron = "${erp.rupture.archivage-cron:0 15 2 * * *}")
    public void archiver() {
        dossiers.archiverAutomatiques();
    }

    @Scheduled(cron = "${erp.rupture.desactivation-cron:0 59 23 * * *}")
    public void desactiverAccesSortants() {
        dossiers.desactiverAccesSortants();
    }
}
