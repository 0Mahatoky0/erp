package com.tovo.erp.conges.model;

public enum StatutSuppleance {
    PROPOSEE, ACCEPTEE, REFUSEE, EN_COURS, TERMINEE;

    public boolean estActive() {
        return this == PROPOSEE || this == ACCEPTEE || this == EN_COURS;
    }
}
