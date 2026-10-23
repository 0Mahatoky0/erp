package com.tovo.erp.domain.enums;

/**
 * Statut global d'une demande de recrutement.
 *
 * <p>Ce statut n'est jamais saisi par l'utilisateur : il est recalcule par le backend
 * a partir du statut de ses lignes (RM-041 a RM-047).
 */
public enum StatutDemande {

    /** La demande n'a pas encore ete soumise. */
    BROUILLON,

    /** Au moins une ligne est en cours de validation. */
    EN_VALIDATION,

    /** Au moins une ligne attend une correction du demandeur. */
    A_CORRIGER,

    /** Toutes les lignes sont acceptees. */
    FINALISEE,

    /** Toutes les lignes sont terminees, au moins une acceptee, au moins une autre non. */
    FINALISEE_PARTIELLE,

    /** Toutes les lignes sont terminees et aucune n'est acceptee. */
    REFUSEE,

    /** Toutes les lignes ont ete annulees. */
    ANNULEE
}
