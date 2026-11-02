package com.tovo.erp.besoin.dto;

import com.tovo.erp.besoin.model.HistoriqueStatut;
import com.tovo.erp.besoin.model.Validation;
import com.tovo.erp.besoin.model.EtapeValidation;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Objets de transfert des decisions, verifications budgetaires et changements de statut. */
public final class ValidationDto {

    private ValidationDto() {
    }

    /**
     * Decision prise par un validateur a une etape du circuit (RM-033).
     *
     * @param montantVerifie montant acknowledges par la finance ; s'il est renseigne, il doit
     *                        correspondre exactement au montant soumis pour la ligne (RM-054)
     */
    public record DecisionRequest(@NotNull com.tovo.erp.besoin.model.Decision decision,
                                  String commentaire,
                                  BigDecimal montantVerifie) {
    }

    /** Entree d'historique d'une decision. */
    public record Historique(Long id, EtapeValidation etape,
                              com.tovo.erp.besoin.model.Decision decision, String commentaire,
                              LocalDateTime dateAction, String utilisateur, Long idUtilisateur, int episode) {

        public static Historique de(Validation validation) {
            return new Historique(
                    validation.getId(),
                    validation.getEtape(),
                    validation.getDecision(),
                    validation.getCommentaire(),
                    validation.getDateAction(),
                    validation.getUtilisateur().getNom(),
                    validation.getUtilisateur().getId(),
                    validation.getEpisode());
        }
    }

    /** Entree d'historique d'une verification budgetaire (RM-053). */
    public record VerificationBudgetaire(Long id, Long idBudgetService, Integer exercice,
                                         BigDecimal budgetDemande, BigDecimal budgetDisponible,
                                         BigDecimal montantEstime,
                                         com.tovo.erp.besoin.model.Decision decision, String commentaire,
                                         LocalDateTime dateVerification, String utilisateur, int episode) {

        public static VerificationBudgetaire de(com.tovo.erp.besoin.model.VerificationBudget verification) {
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
                    verification.getUtilisateur().getNom(),
                    verification.getEpisode());
        }
    }

    /** Changement de statut trace dans le journal (RM-065). */
    public record ChangementStatut(Long id, Long idDemandePoste, String ancienStatut, String nouveauStatut,
                                   String utilisateur, String commentaire, LocalDateTime dateAction) {

        public static ChangementStatut de(HistoriqueStatut historique) {
            return new ChangementStatut(
                    historique.getId(),
                    historique.getDemandePoste() == null ? null : historique.getDemandePoste().getId(),
                    historique.getAncienStatut(),
                    historique.getNouveauStatut(),
                    historique.getUtilisateur() == null ? null : historique.getUtilisateur().getNom(),
                    historique.getCommentaire(),
                    historique.getDateAction());
        }
    }
}
