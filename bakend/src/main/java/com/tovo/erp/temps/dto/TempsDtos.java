package com.tovo.erp.temps.dto;

import com.tovo.erp.temps.model.AnomaliePointage;
import com.tovo.erp.temps.model.HeureSupplementaire;
import com.tovo.erp.temps.model.JourTravaille;
import com.tovo.erp.temps.model.PlanningTravail;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public final class TempsDtos {

    private TempsDtos() {
    }

    public record PlanningRequest(
            @NotNull Long idEmploye,
            @NotBlank String joursTravailles,
            @NotNull LocalTime heureDebut,
            @NotNull LocalTime heureFin,
            @NotNull LocalDate dateDebut,
            LocalDate dateFin,
            Integer pauseMinutes,
            Integer toleranceMinutes) {
    }

    public record PointageRequest(
            @NotNull Long idEmploye,
            LocalDate date,
            LocalTime heure) {
    }

    public record PlanningDto(
            Long id,
            Long idEmploye,
            String joursTravailles,
            LocalTime heureDebut,
            LocalTime heureFin,
            LocalDate dateDebut,
            LocalDate dateFin,
            int pauseMinutes,
            int toleranceMinutes) {
        public static PlanningDto de(PlanningTravail planning) {
            return new PlanningDto(planning.getId(), planning.getIdEmploye(), planning.getJoursTravailles(),
                    planning.getHeureDebut(), planning.getHeureFin(), planning.getDateDebut(), planning.getDateFin(),
                    planning.getPauseMinutes(), planning.getToleranceMinutes());
        }
    }

    public record PointageDto(
            Long id,
            Long idEmploye,
            LocalDate date,
            LocalTime heureEntree,
            LocalTime heureSortie,
            long minutesTravaillees,
            long minutesRetard,
            long minutesSupplementaires,
            String source) {
    }

    public record PointageMoisDto(
            Long idEmploye,
            String mois,
            long joursPointes,
            long joursAvecSortie,
            long minutesTravaillees,
            long minutesRetard,
            long minutesSupplementaires,
            List<PointageDto> pointages) {
    }

    public record AnomalieDto(
            Long id,
            String type,
            String gravite,
            String statut,
            String resolution) {
        public static AnomalieDto de(AnomaliePointage anomalie) {
            return new AnomalieDto(anomalie.getId(), anomalie.getType(), anomalie.getGravite(),
                    anomalie.getStatut().name(), anomalie.getResolution());
        }
    }

    public record AnomalieTraitementRequest(
            @NotNull String statut,
            String resolution) {
    }

    public record JourTravailleDto(
            Long id,
            Long idEmploye,
            LocalDate date,
            Long idPlanning,
            LocalTime heureEntree,
            LocalTime heureSortie,
            int minutesPresence,
            int minutesPause,
            int minutesTravaillees,
            int minutesTheoriques,
            int minutesRetard,
            int minutesDepartAnticipe,
            int minutesHs,
            int minutesNuit,
            int minutesDimanche,
            int minutesFerie,
            String statut,
            List<AnomalieDto> anomalies) {
        public static JourTravailleDto de(JourTravaille jour, List<AnomaliePointage> anomalies) {
            return new JourTravailleDto(jour.getId(), jour.getIdEmploye(), jour.getDateJour(), jour.getIdPlanning(),
                    jour.getHeureEntree(), jour.getHeureSortie(), jour.getMinutesPresence(), jour.getMinutesPause(),
                    jour.getMinutesTravaillees(), jour.getMinutesTheoriques(), jour.getMinutesRetard(),
                    jour.getMinutesDepartAnticipe(), jour.getMinutesHs(), jour.getMinutesNuit(),
                    jour.getMinutesDimanche(), jour.getMinutesFerie(), jour.getStatut().name(),
                    anomalies.stream().map(AnomalieDto::de).toList());
        }
    }

    public record ClotureRequest(@jakarta.validation.constraints.Min(2000)
                                  @jakarta.validation.constraints.Max(2100) int annee,
                                  @jakarta.validation.constraints.Min(1)
                                  @jakarta.validation.constraints.Max(12) int mois) {
    }

    public record PeriodeCloseDto(Long id, int annee, int mois, LocalDate dateDebut,
                                  LocalDate dateFin, Long idUtilisateur,
                                  LocalDateTime dateCloture) {
        public static PeriodeCloseDto de(com.tovo.erp.temps.model.PeriodeTempsClose p) {
            return new PeriodeCloseDto(p.getId(), p.getAnnee(), p.getMois(), p.getDateDebut(),
                    p.getDateFin(), p.getIdUtilisateur(), p.getDateCloture());
        }
    }

    public record HeureSupplementaireDto(
            Long id,
            Long idEmploye,
            LocalDate semaineDebut,
            int minutesTotal,
            int minutesHs30,
            int minutesHs50,
            int minutesNuit,
            int minutesDimanche,
            int minutesFerie,
            String statut,
            boolean alertePlafond,
            LocalDateTime dateCalcul) {
        public static HeureSupplementaireDto de(HeureSupplementaire heureSup) {
            return new HeureSupplementaireDto(heureSup.getId(), heureSup.getIdEmploye(), heureSup.getSemaineDebut(),
                    heureSup.getMinutesTotal(), heureSup.getMinutesHs30(), heureSup.getMinutesHs50(),
                    heureSup.getMinutesNuit(), heureSup.getMinutesDimanche(), heureSup.getMinutesFerie(),
                    heureSup.getStatut().name(), heureSup.isAlertePlafond(), heureSup.getDateCalcul());
        }
    }
}
