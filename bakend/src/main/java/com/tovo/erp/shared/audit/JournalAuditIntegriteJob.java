package com.tovo.erp.shared.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Verifie quotidiennement l'integrite de la chaine du journal d'audit (RG-T-02).
 */
@Component
public class JournalAuditIntegriteJob {

    private static final Logger LOG = LoggerFactory.getLogger(JournalAuditIntegriteJob.class);

    private final JournalAuditService audit;

    public JournalAuditIntegriteJob(JournalAuditService audit) {
        this.audit = audit;
    }

    @Scheduled(initialDelayString = "${erp.audit.integrite-delai-initial-ms:120000}",
            fixedDelayString = "${erp.audit.integrite-verification-ms:86400000}")
    public void verifier() {
        int anomalies = audit.verifierChaine();
        if (anomalies > 0) {
            LOG.error("Journal d'audit : {} anomalie(s) de chaine detectee(s) !", anomalies);
        }
    }
}
