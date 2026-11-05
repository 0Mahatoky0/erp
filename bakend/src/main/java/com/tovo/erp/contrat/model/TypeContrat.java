package com.tovo.erp.contrat.model;

/** Type de contrat de travail (bloc 4, section 6.2). */
public enum TypeContrat {
    CDI,
    CDD,
    CDD_RENOUVELLEMENT,
    STAGE,
    APPRENTISSAGE,
    TEMPS_PARTIEL,
    CONSULTANT;

    /** Vrai pour les contrats a duree determinee soumis au plafond de 24 mois. */
    public boolean estDureeDeterminee() {
        return this == CDD || this == CDD_RENOUVELLEMENT;
    }
}
