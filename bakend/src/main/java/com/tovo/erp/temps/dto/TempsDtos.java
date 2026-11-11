package com.tovo.erp.temps.dto;

import com.tovo.erp.temps.model.PlanningTravail;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
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
            LocalDate dateFin) {
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
            LocalDate dateFin) {
        public static PlanningDto de(PlanningTravail planning) {
            return new PlanningDto(planning.getId(), planning.getIdEmploye(), planning.getJoursTravailles(),
                    planning.getHeureDebut(), planning.getHeureFin(), planning.getDateDebut(), planning.getDateFin());
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
            List<PointageDto> pointages,
            List<com.tovo.erp.conges.dto.CongeDtos.PlanningDto> conges) {
    }
}
