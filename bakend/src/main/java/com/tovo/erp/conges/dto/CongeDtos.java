package com.tovo.erp.conges.dto;

import com.tovo.erp.conges.model.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class CongeDtos {
    private CongeDtos() {}

    public record TypeDto(Long id, String code, String libelle, BigDecimal joursAcquisMois,
                          boolean justificatifObligatoire) {
        public static TypeDto de(TypeConge t) {
            return new TypeDto(t.getId(), t.getCode(), t.getLibelle(), t.getJoursAcquisMois(), t.isJustificatifObligatoire());
        }
    }

    public record SoldeDto(Long id, Long idEmploye, Long idTypeConge, String typeConge,
                           int annee, BigDecimal joursAcquis, BigDecimal joursPris, BigDecimal disponible) {}

    public record DemandeRequest(@NotNull Long idEmploye, @NotNull Long idTypeConge,
                                 @NotNull LocalDate dateDebut, @NotNull LocalDate dateFin,
                                 @NotBlank @Size(max = 2000) String motif) {}

    public record DecisionRequest(Long idValidateur, @Size(max = 2000) String motif) {}

    public record DemandeDto(Long id, Long idEmploye, Long idTypeConge, String typeConge,
                             LocalDate dateDebut, LocalDate dateFin, BigDecimal nombreJours,
                             String motif, StatutDemandeConge statut, Long idValidateur,
                             String motifDecision) {
        public static DemandeDto de(DemandeConge d, String type) {
            return new DemandeDto(d.getId(), d.getIdEmploye(), d.getIdTypeConge(), type,
                    d.getDateDebut(), d.getDateFin(), d.getNombreJours(), d.getMotif(),
                    d.getStatut(), d.getIdValidateur(), d.getMotifDecision());
        }
    }

    public record AbsenceRequest(@NotNull Long idEmploye, @NotNull LocalDate dateDebut,
                                 @NotNull LocalDate dateFin, @NotBlank @Size(max = 100) String motif,
                                 String justificatif) {}

    public record AbsenceDto(Long id, Long idEmploye, LocalDate dateDebut, LocalDate dateFin,
                             String motif, String justificatif, StatutAbsence statut, Long idValidateur) {
        public static AbsenceDto de(Absence a) {
            return new AbsenceDto(a.getId(), a.getIdEmploye(), a.getDateDebut(), a.getDateFin(),
                    a.getMotif(), a.getJustificatif(), a.getStatut(), a.getIdValidateur());
        }
    }

    public record CongeResume(List<TypeDto> types, List<SoldeDto> soldes,
                              List<DemandeDto> demandes, List<AbsenceDto> absences) {}
}
