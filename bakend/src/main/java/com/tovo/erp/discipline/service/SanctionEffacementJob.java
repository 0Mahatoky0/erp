package com.tovo.erp.discipline.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class SanctionEffacementJob {

    private final SanctionService sanctions;

    public SanctionEffacementJob(SanctionService sanctions) {
        this.sanctions = sanctions;
    }

    @Scheduled(cron = "0 15 0 * * *")
    public void effacer() {
        sanctions.effacerEchus(LocalDate.now());
    }
}
