package com.tovo.erp.demande.domain;

/** Nature du besoin porte par une ligne de demande (RM-022, RM-023). */
public enum TypeBesoin {

    /** Besoin d'un poste ou d'un effectif supplementaire. */
    CREATION,

    /** Besoin de remplacer un employe existant ou dont le depart est confirme. */
    REMPLACEMENT
}
