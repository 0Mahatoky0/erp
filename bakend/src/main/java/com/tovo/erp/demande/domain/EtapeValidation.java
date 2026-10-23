package com.tovo.erp.domain.enums;

/** Etape du circuit de validation d'une ligne (RM-034). */
public enum EtapeValidation {

    /** Verification de la coherence du besoin et des informations du poste. */
    RH,

    /** Verification de la disponibilite budgetaire. */
    FINANCE,

    /** Decision finale de la direction. */
    DIRECTION
}
