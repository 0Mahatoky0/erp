package com.tovo.erp.temps.service;

import com.tovo.erp.temps.model.AnomaliePointage;
import com.tovo.erp.temps.repository.AnomaliePointageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Relance et escalade RH des anomalies non traitees apres 3 jours ouvres
 * (RG-B6-15). L'escalade est materialisee sur l'anomalie pour l'ecran RH.
 */
@Service
public class AnomalieSlaJob {

    private static final Logger LOG = LoggerFactory.getLogger(AnomalieSlaJob.class);
    private static final int DELAI_JOURS_OUVRES = 3;

    private final AnomaliePointageRepository anomalies;

    public AnomalieSlaJob(AnomaliePointageRepository anomalies) {
        this.anomalies = anomalies;
    }

    @Scheduled(cron = "0 0 7 * * *")
    @Transactional
    public void escaladerAnomaliesEnRetard() {
        LocalDate aujourdhui = LocalDate.now();
        int escaladees = 0;
        for (AnomaliePointage anomalie : anomalies.findByStatutAndEscaladeeFalseAndDateAnomalieBefore(
                AnomaliePointage.Statut.OUVERTE, aujourdhui)) {
            if (joursOuvresEntre(anomalie.getDateAnomalie(), aujourdhui) < DELAI_JOURS_OUVRES) {
                continue;
            }
            anomalie.setEscaladee(true);
            anomalie.setDateEscalade(LocalDateTime.now());
            anomalie.setGravite("HAUTE");
            anomalies.save(anomalie);
            escaladees++;
        }
        if (escaladees > 0) {
            LOG.warn("{} anomalie(s) de pointage escaladee(s) apres {} jours ouvres.",
                    escaladees, DELAI_JOURS_OUVRES);
        }
    }

    private static long joursOuvresEntre(LocalDate debut, LocalDate fin) {
        long jours = 0;
        for (LocalDate date = debut.plusDays(1); !date.isAfter(fin); date = date.plusDays(1)) {
            if (date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY) {
                jours++;
            }
        }
        return jours;
    }
}
