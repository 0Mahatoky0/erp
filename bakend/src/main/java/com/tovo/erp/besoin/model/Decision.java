package com.tovo.erp.besoin.model;

/** Decision prise par un validateur a une etape du circuit (RM-035 a RM-037). */
public enum Decision {

    /** L'etape est franchie, la ligne passe a l'etape suivante. */
    APPROUVE,

    /** L'etape refuse definitivement la ligne. */
    REFUSE,

    /** L'etape demande une correction au demandeur. */
    A_CORRIGER
}
