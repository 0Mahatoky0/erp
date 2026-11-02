package com.tovo.erp.demande.domain;

/** Duree du remplacement (RM-024). */
public enum TypeRemplacement {

    /** Remplacement limite dans le temps : une date de fin est renseignee. */
    TEMPORAIRE,

    /** Remplacement durable, typiquement suite a un depart definitif. */
    DEFINITIF
}
