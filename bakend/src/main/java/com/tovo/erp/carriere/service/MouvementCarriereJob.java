package com.tovo.erp.carriere.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MouvementCarriereJob {

    private final MouvementCarriereService service;

    public MouvementCarriereJob(MouvementCarriereService service) {
        this.service = service;
    }

    @Scheduled(cron = "0 10 0 * * *")
    public void appliquerLesMouvementsPlanifies() {
        service.appliquerMouvementsPlanifies();
        service.traiterRetoursPlanifies();
    }
}
