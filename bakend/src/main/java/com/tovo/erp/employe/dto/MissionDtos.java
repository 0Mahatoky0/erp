package com.tovo.erp.employe.dto;

import com.tovo.erp.employe.model.Mission;
import com.tovo.erp.employe.model.PrioriteMission;
import com.tovo.erp.employe.model.StatutMission;
import com.tovo.erp.employe.model.SuiviMission;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Objets de transfert des missions et de leur suivi (bloc 5). */
public final class MissionDtos {

    private MissionDtos() {
    }

    public record CreationMissionRequest(
            @NotBlank String titre,
            String description,
            String lieu,
            @NotNull LocalDate dateDebut,
            LocalDate dateFinPrevue,
            PrioriteMission priorite,
            Long idResponsable) {
    }

    public record ModificationMissionRequest(
            @NotBlank String titre,
            String description,
            String lieu,
            @NotNull LocalDate dateDebut,
            LocalDate dateFinPrevue,
            LocalDate dateFinReelle,
            PrioriteMission priorite,
            StatutMission statut,
            Long idResponsable,
            @Min(0) @Max(100) Integer avancement,
            Boolean manaoAsa,
            Boolean miassaTsara,
            String commentaireSuivi) {
    }

    public record SuiviRequest(
            @Min(0) @Max(100) Integer avancement,
            Boolean manaoAsa,
            Boolean miassaTsara,
            String commentaire) {
    }

    public record SuiviDto(
            Long id,
            Long idMission,
            LocalDateTime dateSuivi,
            Integer avancement,
            Boolean manaoAsa,
            Boolean miassaTsara,
            String commentaire,
            Long idAuteur) {

        public static SuiviDto de(SuiviMission suivi) {
            return new SuiviDto(suivi.getId(), suivi.getIdMission(), suivi.getDateSuivi(),
                    suivi.getAvancement(), suivi.getManaoAsa(), suivi.getMiassaTsara(),
                    suivi.getCommentaire(), suivi.getIdAuteur());
        }
    }

    public record MissionDto(
            Long id,
            Long idEmploye,
            String reference,
            String titre,
            String description,
            String lieu,
            LocalDate dateDebut,
            LocalDate dateFinPrevue,
            LocalDate dateFinReelle,
            PrioriteMission priorite,
            StatutMission statut,
            Long idResponsable,
            Integer avancement,
            Boolean manaoAsa,
            Boolean miassaTsara,
            String commentaireSuivi,
            LocalDateTime dateCreation,
            LocalDateTime dateModification) {

        public static MissionDto de(Mission mission) {
            return new MissionDto(mission.getId(), mission.getIdEmploye(),
                    mission.getReference(), mission.getTitre(), mission.getDescription(),
                    mission.getLieu(), mission.getDateDebut(), mission.getDateFinPrevue(),
                    mission.getDateFinReelle(), mission.getPriorite(), mission.getStatut(),
                    mission.getIdResponsable(), mission.getAvancement(),
                    mission.getManaoAsa(), mission.getMiassaTsara(),
                    mission.getCommentaireSuivi(), mission.getDateCreation(),
                    mission.getDateModification());
        }
    }
}
