package com.tovo.erp.carriere.service;

import com.tovo.erp.shared.audit.JournalAuditService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MouvementCarriereJob {

    private final MouvementCarriereService service;
    private final JournalAuditService audit;

    public MouvementCarriereJob(MouvementCarriereService service, JournalAuditService audit) {
        this.service = service;
        this.audit = audit;
    }

    @Scheduled(cron = "0 10 0 * * *")
    public void appliquerLesMouvementsPlanifies() {
        service.appliquerMouvementsPlanifies();
        service.traiterRetoursPlanifies();
    }

    @Scheduled(cron = "0 30 2 * * *")
    public void controlerLIntegriteTemporelle() {
        var rapport = service.rapportIntegrite();
        if (!rapport.anomalies().isEmpty()) {
            audit.enregistrer("RAPPORT_INTEGRITE_CARRIERE", "carriere", null, null,
                    rapport.anomalies().size() + " anomalie(s)");
        }
    }
}
