package com.tovo.erp.besoin.service;

import com.tovo.erp.besoin.model.DemandeRecrutementPoste;
import com.tovo.erp.besoin.repository.DemandeRecrutementPosteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Surveille le delai de traitement des validations (RG-B1-13) : relance a partir
 * du SLA cible, escalade apres le delai d'escalade.
 *
 * <p>Les notifications sortantes seront branchees en phase 8 ; ce controle trace
 * des maintenant les relances et escalades dans les journaux applicatifs.
 */
@Component
public class DemandeSlaJob {

    private static final Logger LOG = LoggerFactory.getLogger(DemandeSlaJob.class);

    private final DemandeRecrutementPosteRepository lignes;
    private final DemandeProperties proprietes;

    public DemandeSlaJob(DemandeRecrutementPosteRepository lignes, DemandeProperties proprietes) {
        this.lignes = lignes;
        this.proprietes = proprietes;
    }

    @Scheduled(initialDelayString = "${erp.demande.sla-delai-initial-ms:240000}",
            fixedDelayString = "${erp.demande.sla-verification-ms:3600000}")
    public void verifier() {
        LocalDateTime maintenant = LocalDateTime.now();
        LocalDateTime limite = maintenant.minusDays(proprietes.slaValidationJours());
        List<DemandeRecrutementPoste> enRetard = lignes.findEnValidationAvant(limite);
        for (DemandeRecrutementPoste ligne : enRetard) {
            long jours = ChronoUnit.DAYS.between(ligne.getDateModification(), maintenant);
            if (jours >= proprietes.escaladeJours()) {
                LOG.warn("ESCALADE validation : ligne {} en attente depuis {} jour(s).",
                        ligne.getId(), jours);
            } else {
                LOG.info("RELANCE validation : ligne {} en attente depuis {} jour(s).",
                        ligne.getId(), jours);
            }
        }
    }
}
