package com.tovo.erp.besoin.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Expire quotidiennement les demandes finalisees restees sans offre publiee
 * au-dela du delai parametre (RG-B1-14).
 */
@Component
public class DemandeExpirationJob {

    private static final Logger LOG = LoggerFactory.getLogger(DemandeExpirationJob.class);

    private final DemandeService demandes;

    public DemandeExpirationJob(DemandeService demandes) {
        this.demandes = demandes;
    }

    @Scheduled(initialDelayString = "${erp.demande.expiration-delai-initial-ms:180000}",
            fixedDelayString = "${erp.demande.expiration-verification-ms:86400000}")
    public void expirer() {
        int expirees = demandes.expirerDemandes();
        if (expirees > 0) {
            LOG.info("Demandes de recrutement expirees : {}", expirees);
        }
    }
}
