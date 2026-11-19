package com.tovo.erp.discipline.dto;

import com.tovo.erp.discipline.model.AbsenceDisciplinaire;
import com.tovo.erp.discipline.model.AccesDisciplinaire;
import com.tovo.erp.discipline.model.CategorieIncident;
import com.tovo.erp.discipline.model.EtapeProcedure;
import com.tovo.erp.discipline.model.GraviteIncident;
import com.tovo.erp.discipline.model.Incident;
import com.tovo.erp.discipline.model.JournalDiscipline;
import com.tovo.erp.discipline.model.NiveauAccesDisciplinaire;
import com.tovo.erp.discipline.model.PieceDisciplinaire;
import com.tovo.erp.discipline.model.ProcedureDisciplinaire;
import com.tovo.erp.discipline.model.SanctionDisciplinaire;
import com.tovo.erp.discipline.model.StatutIncident;
import com.tovo.erp.discipline.model.StatutProcedure;
import com.tovo.erp.discipline.model.StatutSanction;
import com.tovo.erp.discipline.model.TypeEtapeProcedure;
import com.tovo.erp.discipline.model.TypePieceDisciplinaire;
import com.tovo.erp.discipline.model.TypeSanction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class DisciplineDtos {

    private DisciplineDtos() {
    }

    public record TemoinDto(Long idEmploye, boolean protege) {
    }

    public record IncidentDto(
            Long id, String reference, Long declarantId, LocalDateTime dateDeclaration,
            LocalDate dateFaits, LocalDate dateConnaissance, String lieu, String description,
            CategorieIncident categorie, GraviteIncident graviteInitiale, GraviteIncident graviteRetenue,
            StatutIncident statut, String motifClassement, List<Long> employesConcernes,
            List<TemoinDto> temoins) {
    }

    public record IncidentRequest(
            @NotNull LocalDate dateFaits,
            @NotNull LocalDate dateConnaissance,
            String lieu,
            @NotBlank String description,
            @NotNull CategorieIncident categorie,
            @NotNull GraviteIncident graviteInitiale,
            @NotNull List<Long> employesConcernes,
            List<TemoinDto> temoins) {
    }

    public record QualificationRequest(
            @NotNull GraviteIncident graviteRetenue,
            boolean ouvrirProcedure,
            String motif,
            List<Long> employesConcernes,
            Long idInstructeur,
            Long idDecideur,
            boolean miseAPied,
            LocalDate miseAPiedDebut,
            LocalDate miseAPiedFin,
            boolean derogationPrescription) {
    }

    public record ProcedureDto(
            Long id, String reference, Long idIncident, Long idEmploye,
            Long instructeurId, Long decideurId, LocalDateTime dateOuverture,
            LocalDate dateLimitePrescription, boolean miseAPied, LocalDate miseAPiedDebut,
            LocalDate miseAPiedFin, Boolean miseAPiedRemuneree, StatutProcedure statut,
            String decision, String motivation, LocalDateTime dateDecision, LocalDateTime dateCloture,
            List<EtapeDto> etapes, List<SanctionDto> sanctions) {

        public static ProcedureDto de(ProcedureDisciplinaire procedure, List<EtapeDto> etapes,
                                      List<SanctionDto> sanctions) {
            return new ProcedureDto(procedure.getId(), procedure.getReference(), procedure.getIdIncident(),
                    procedure.getIdEmploye(), procedure.getInstructeurId(), procedure.getDecideurId(),
                    procedure.getDateOuverture(), procedure.getDateLimitePrescription(),
                    procedure.isMiseAPied(), procedure.getMiseAPiedDebut(), procedure.getMiseAPiedFin(),
                    procedure.getMiseAPiedRemuneree(), procedure.getStatut(), procedure.getDecision(),
                    procedure.getMotivation(), procedure.getDateDecision(), procedure.getDateCloture(),
                    etapes, sanctions);
        }
    }

    public record ProcedureRequest(
            @NotNull Long idInstructeur, @NotNull Long idDecideur, boolean miseAPied,
            LocalDate miseAPiedDebut, LocalDate miseAPiedFin) {
    }

    public record EtapeDto(
            Long id, Long idProcedure, TypeEtapeProcedure type, LocalDateTime dateEtape,
            Long acteurId, String modeRemise, String preuve, Integer delaiReponseJours,
            LocalDateTime dateLimite, Long idDocument, String commentaire) {

        public static EtapeDto de(EtapeProcedure etape) {
            return new EtapeDto(etape.getId(), etape.getIdProcedure(), etape.getType(),
                    etape.getDateEtape(), etape.getActeurId(), etape.getModeRemise(), etape.getPreuve(),
                    etape.getDelaiReponseJours(), etape.getDateLimite(), etape.getIdDocument(),
                    etape.getCommentaire());
        }
    }

    public record EtapeRequest(
            @NotNull TypeEtapeProcedure type,
            String modeRemise,
            String preuve,
            Integer delaiReponseJours,
            Long idDocument,
            String commentaire) {
    }

    public record DecisionRequest(
            @NotBlank String decision, @NotBlank String motivation,
            List<Long> piecesReferencees, boolean entretienRealise) {
    }

    public record NotificationRequest(
            @NotNull TypeSanction type, @NotNull LocalDate dateEffet, Integer dureeJours,
            @NotBlank String motif, @NotBlank String baseReglement,
            @NotBlank String modeRemise, String preuve) {
    }

    public record RecoursRequest(@NotBlank String motif) {
    }

    public record SanctionDto(
            Long id, Long idProcedure, Long idEmploye, TypeSanction type,
            LocalDateTime dateNotification, LocalDate dateEffet, Integer dureeJours,
            String motif, String baseReglement, StatutSanction statut,
            LocalDate dateEffacement, BigDecimal impactPaieJours) {

        public static SanctionDto de(SanctionDisciplinaire sanction) {
            return new SanctionDto(sanction.getId(), sanction.getIdProcedure(), sanction.getIdEmploye(),
                    sanction.getType(), sanction.getDateNotification(), sanction.getDateEffet(),
                    sanction.getDureeJours(), sanction.getMotif(), sanction.getBaseReglement(),
                    sanction.getStatut(), sanction.getDateEffacement(), sanction.getImpactPaieJours());
        }
    }

    public record PieceDto(
            Long id, Long idIncident, Long idProcedure, TypePieceDisciplinaire type,
            String fichier, String hashSha256, Integer version, Long auteurId,
            LocalDateTime dateAjout, String confidentialite) {

        public static PieceDto de(PieceDisciplinaire piece) {
            return new PieceDto(piece.getId(), piece.getIdIncident(), piece.getIdProcedure(),
                    piece.getType(), piece.getFichier(), piece.getHashSha256(), piece.getVersion(),
                    piece.getAuteurId(), piece.getDateAjout(), piece.getConfidentialite());
        }
    }

    public record PieceRequest(
            @NotNull TypePieceDisciplinaire type, @NotBlank String fichier,
            @NotBlank String hashSha256, Long idIncident, Long idProcedure) {
    }

    public record AccesDto(
            Long id, Long idIncident, Long idProcedure, Long utilisateurId,
            NiveauAccesDisciplinaire niveau, String motif, Long accordeParId,
            LocalDateTime dateDebut, LocalDateTime dateFin, boolean actif) {

        public static AccesDto de(AccesDisciplinaire acces) {
            return new AccesDto(acces.getId(), acces.getIdIncident(), acces.getIdProcedure(),
                    acces.getUtilisateurId(), acces.getNiveau(), acces.getMotif(),
                    acces.getAccordeParId(), acces.getDateDebut(), acces.getDateFin(), acces.isActif());
        }
    }

    public record AccesRequest(
            Long idIncident, Long idProcedure, @NotNull Long utilisateurId,
            @NotNull NiveauAccesDisciplinaire niveau, @NotBlank String motif,
            LocalDateTime dateFin) {
    }

    public record JournalDto(
            Long id, LocalDateTime dateAction, Long utilisateurId, String action,
            String ressource, Long idRessource, String motif, String adresseIp, String details) {

        public static JournalDto de(JournalDiscipline journal) {
            return new JournalDto(journal.getId(), journal.getDateAction(), journal.getUtilisateurId(),
                    journal.getAction(), journal.getRessource(), journal.getIdRessource(), journal.getMotif(),
                    journal.getAdresseIp(), journal.getDetails());
        }
    }

    public record AbsenceDto(
            Long id, Long idSanction, Long idEmploye, LocalDate dateDebut, LocalDate dateFin,
            BigDecimal jours, boolean remuneree, String motif, boolean traiteePaie) {

        public static AbsenceDto de(AbsenceDisciplinaire absence) {
            return new AbsenceDto(absence.getId(), absence.getIdSanction(), absence.getIdEmploye(),
                    absence.getDateDebut(), absence.getDateFin(), absence.getJours(),
                    absence.isRemuneree(), absence.getMotif(), absence.isTraiteePaie());
        }
    }

    public record VariablePaieDto(
            Long idAbsence, Long idEmploye, String code, BigDecimal jours,
            BigDecimal retenue, boolean remuneree, String motif) {
    }

    public record IndicateursDto(
            BigDecimal tauxIncidents, BigDecimal delaiMoyenTraitementJours,
            Map<String, Long> incidentsParCategorie, Map<String, Long> sanctionsParType) {
    }
}
