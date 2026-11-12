package com.tovo.erp.conges.dto;

import com.tovo.erp.conges.model.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
                           int annee, BigDecimal joursAcquis, BigDecimal joursPris, BigDecimal joursReserves,
                           BigDecimal disponible, boolean alerte, BigDecimal plafond) {}

    public record DemandeRequest(@NotNull Long idEmploye, @NotNull Long idTypeConge,
                                 @NotNull LocalDate dateDebut, @NotNull LocalDate dateFin,
                                 DemiJournee demiJourneeDebut, DemiJournee demiJourneeFin,
                                 @NotBlank @Size(max = 2000) String motif,
                                 Long idRemplacant, Long justificatifDocumentId) {}

    public record DecisionRequest(Long idValidateur, @Size(max = 2000) String motif) {}

    public record AnnulationRequest(@Size(max = 2000) String motif) {}

    public record EcourtementRequest(@NotNull LocalDate dateFinReelle, @Size(max = 2000) String motif) {}

    public record AjustementRequest(@NotNull Long idEmploye, @NotNull Long idTypeConge,
                                    @NotNull Integer annee, @NotNull BigDecimal jours,
                                    @NotBlank @Size(max = 2000) String motif) {}

    public record DemandeDto(Long id, Long idEmploye, Long idTypeConge, String typeConge,
                             LocalDate dateDebut, LocalDate dateFin, BigDecimal nombreJours,
                             String motif, StatutDemandeConge statut, Long idValidateur,
                             String motifDecision, LocalDateTime dateDepot,
                             DemiJournee demiJourneeDebut, DemiJournee demiJourneeFin,
                             Long idValidateurManager, LocalDateTime dateValidationManager,
                             Long idValidateurRh, LocalDateTime dateValidationRh,
                             Long idRemplacant, Long justificatifDocumentId) {
        public static DemandeDto de(DemandeConge d, String type) {
            return new DemandeDto(d.getId(), d.getIdEmploye(), d.getIdTypeConge(), type,
                    d.getDateDebut(), d.getDateFin(), d.getNombreJours(), d.getMotif(),
                    d.getStatut(), d.getIdValidateur(), d.getMotifDecision(), d.getDateDepot(),
                    d.getDemiJourneeDebut(), d.getDemiJourneeFin(),
                    d.getIdValidateurManager(), d.getDateValidationManager(),
                    d.getIdValidateurRh(), d.getDateValidationRh(),
                    d.getIdRemplacant(), d.getJustificatifDocumentId());
        }
    }

    public record MouvementDto(Long id, Long idEmploye, Long idTypeConge, Long idDroitConge,
                               Long idDemandeConge, TypeMouvementConge typeMouvement,
                               BigDecimal jours, LocalDate dateEffet, String commentaire,
                               LocalDateTime dateCreation) {
        public static MouvementDto de(MouvementConge m) {
            return new MouvementDto(m.getId(), m.getIdEmploye(), m.getIdTypeConge(),
                    m.getIdDroitConge(), m.getIdDemandeConge(), m.getTypeMouvement(),
                    m.getJours(), m.getDateEffet(), m.getCommentaire(), m.getDateCreation());
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

    public record EmployeCourantDto(Long idEmploye) {}

    public record VariablePaieDto(Long idEmploye, LocalDate dateDebut, LocalDate dateFin,
                                  BigDecimal jours, String typeConge, String motif) {}

    public record PlanningDto(Long idEmploye, LocalDate dateDebut, LocalDate dateFin,
                              BigDecimal jours, String typeConge, String motif) {}

    public record PeriodeBloqueeRequest(@NotBlank @Size(max = 120) String libelle,
                                        @NotNull LocalDate dateDebut, @NotNull LocalDate dateFin,
                                        boolean derogationDirection, Boolean actif) {}

    public record PeriodeBloqueeDto(Long id, String libelle, LocalDate dateDebut, LocalDate dateFin,
                                    boolean derogationDirection, boolean actif) {
        public static PeriodeBloqueeDto de(PeriodeBloquee p) {
            return new PeriodeBloqueeDto(p.getId(), p.getLibelle(), p.getDateDebut(), p.getDateFin(),
                    p.isDerogationDirection(), p.isActif());
        }
    }

    public record MatriceRemplacementRequest(Long idPosteRemplace, Long idEmployeRemplace,
                                             Long idPosteRemplacant, Long idEmployeRemplacant,
                                             NiveauRemplacement niveau, Integer dureeMinJours,
                                             Integer dureeMaxJours, String competencesRequises,
                                             String habilitations, Boolean actif) {}

    public record MatriceRemplacementDto(Long id, Long idPosteRemplace, Long idEmployeRemplace,
                                         Long idPosteRemplacant, Long idEmployeRemplacant,
                                         NiveauRemplacement niveau, Integer dureeMinJours,
                                         Integer dureeMaxJours, String competencesRequises,
                                         String habilitations, boolean actif) {
        public static MatriceRemplacementDto de(MatriceRemplacement m) {
            return new MatriceRemplacementDto(m.getId(), m.getIdPosteRemplace(), m.getIdEmployeRemplace(),
                    m.getIdPosteRemplacant(), m.getIdEmployeRemplacant(), m.getNiveau(),
                    m.getDureeMinJours(), m.getDureeMaxJours(), m.getCompetencesRequises(),
                    m.getHabilitations(), m.isActif());
        }
    }

    public record SuppleanceRequest(Long idRemplacant, @Size(max = 500) String motif) {}

    public record ReponseSuppleanceRequest(@NotBlank String statut, @Size(max = 500) String motif) {}

    public record SuppleanceDto(Long id, Long idEmployeAbsent, Long idEmployeRemplacant,
                                Long idDemandeConge, LocalDate dateDebut, LocalDate dateFin,
                                StatutSuppleance statut, String motif,
                                LocalDateTime dateProposition, LocalDateTime dateReponse) {
        public static SuppleanceDto de(Suppleance s) {
            return new SuppleanceDto(s.getId(), s.getIdEmployeAbsent(), s.getIdEmployeRemplacant(),
                    s.getIdDemandeConge(), s.getDateDebut(), s.getDateFin(), s.getStatut(),
                    s.getMotif(), s.getDateProposition(), s.getDateReponse());
        }
    }
}
