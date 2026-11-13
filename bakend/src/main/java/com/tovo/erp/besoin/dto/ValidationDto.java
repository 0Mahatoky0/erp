package com.tovo.erp.besoin.dto;

import com.tovo.erp.besoin.model.HistoriqueStatut;
import com.tovo.erp.besoin.model.Validation;
import com.tovo.erp.besoin.model.EtapeValidation;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Objets de transfert des decisions, verifications budgetaires et changements de statut. */
public final class ValidationDto {

    private ValidationDto() {
    }

    /**
     * Decision prise par un validateur a une etape du circuit (RM-033).
     *
     * @param montantVerifie   montant acknowledges par la finance ; s'il est renseigne, il doit
     *                         correspondre exactement au montant soumis pour la ligne (RM-054)
     * @param salaireMinEstime fourchette salariale definie par le RH pour ce profil
     *                         (etape RH, decision APPROUVE uniquement)
     * @param salaireMaxEstime idem, borne haute
     * @param budgetEstime     budget du profil defini par le RH ; obligatoire a l'approbation
     *                         de l'etape RH si la ligne n'en porte pas encore
     */
    public record DecisionRequest(@NotNull com.tovo.erp.besoin.model.Decision decision,
                                  String commentaire,
                                  BigDecimal montantVerifie,
                                  @PositiveOrZero BigDecimal salaireMinEstime,
                                  @PositiveOrZero BigDecimal salaireMaxEstime,
                                  @PositiveOrZero BigDecimal budgetEstime) {
    }

    /** Entree d'historique d'une decision. */
    public record Historique(Long id, EtapeValidation etape,
                              com.tovo.erp.besoin.model.Decision decision, String commentaire,
                              LocalDateTime dateAction, String utilisateur, Long idUtilisateur, int episode) {

        public static Historique de(Validation validation, String utilisateur) {
            return new Historique(
                    validation.getId(),
                    validation.getEtape(),
                    validation.getDecision(),
                    validation.getCommentaire(),
                    validation.getDateAction(),
                    utilisateur,
                    validation.getIdUtilisateur(),
                    validation.getEpisode());
        }
    }

    /** Entree d'historique d'une verification budgetaire (RM-053). */
    public record VerificationBudgetaire(Long id, Long idBudgetService, Integer exercice,
                                         BigDecimal budgetDemande, BigDecimal budgetDisponible,
                                         BigDecimal montantEstime,
                                         com.tovo.erp.besoin.model.Decision decision, String commentaire,
                                         LocalDateTime dateVerification, String utilisateur, int episode) {

        public static VerificationBudgetaire de(com.tovo.erp.besoin.model.VerificationBudget verification,
                                                   String utilisateur) {
            return new VerificationBudgetaire(
                    verification.getId(),
                    verification.getBudgetService().getId(),
                    verification.getBudgetService().getExercice(),
                    verification.getBudgetDemande(),
                    verification.getBudgetDisponible(),
                    verification.getMontantEstime(),
                    verification.getDecision(),
                    verification.getCommentaire(),
                    verification.getDateVerification(),
                    utilisateur,
                    verification.getEpisode());
        }
    }

    /** Changement de statut trace dans le journal (RM-065). */
    public record ChangementStatut(Long id, Long idDemandePoste, String ancienStatut, String nouveauStatut,
                                   String utilisateur, String commentaire, LocalDateTime dateAction) {

        public static ChangementStatut de(HistoriqueStatut historique, String utilisateur) {
            return new ChangementStatut(
                    historique.getId(),
                    historique.getDemandePoste() == null ? null : historique.getDemandePoste().getId(),
                    historique.getAncienStatut(),
                    historique.getNouveauStatut(),
                    utilisateur,
                    historique.getCommentaire(),
                    historique.getDateAction());
        }
    }
}
