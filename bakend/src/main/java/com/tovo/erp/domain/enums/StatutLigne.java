package com.tovo.erp.domain.enums;

/**
 * Statut d'une ligne de demande (demande_recrutement_poste).
 *
 * <p>Chaque ligne possede son propre circuit de validation (RM-032) : une ligne peut
 * etre acceptee pendant que sa voisine est refusee ou en attente de correction.
 */
public enum StatutLigne {

    /** La ligne n'est pas encore soumise. */
    BROUILLON,

    /** En attente de validation RH. */
    EN_ATTENTE_RH,

    /** En attente de validation financiere. */
    EN_ATTENTE_FINANCE,

    /** En attente de validation de la direction. */
    EN_ATTENTE_DIRECTION,

    /** Le demandeur doit corriger la ligne puis la resoumettre. */
    A_CORRIGER,

    /** La ligne a passe toutes les validations requises. */
    ACCEPTEE,

    /** La ligne a ete refusee. */
    REFUSEE,

    /** La ligne a ete annulee. */
    ANNULEE;

    /** Un statut terminal est definitif : la ligne ne participe plus au circuit. */
    public boolean estTerminal() {
        return this == ACCEPTEE || this == REFUSEE || this == ANNULEE;
    }

    /** Vrai si la ligne est encore en attente d'une decision. */
    public boolean estEnValidation() {
        return this == EN_ATTENTE_RH || this == EN_ATTENTE_FINANCE || this == EN_ATTENTE_DIRECTION;
    }
}
