package com.tovo.erp.conges.model;

public enum StatutDemandeConge {
    BROUILLON, SOUMISE, VALIDEE_MANAGER, VALIDEE_RH,
    APPROUVEE, EN_COURS, TERMINEE, REFUSEE, ANNULEE, ECOURTEE;

    public boolean estActive() {
        return this == SOUMISE || this == VALIDEE_MANAGER || this == VALIDEE_RH
                || this == APPROUVEE || this == EN_COURS;
    }

    public boolean estReservante() {
        return this == SOUMISE || this == VALIDEE_MANAGER || this == VALIDEE_RH;
    }

    public boolean estConsommee() {
        return this == APPROUVEE || this == EN_COURS || this == TERMINEE || this == ECOURTEE;
    }
}
