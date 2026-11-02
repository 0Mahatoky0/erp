package com.tovo.erp.besoin.dto;

import java.math.BigDecimal;

/** Objets de transfert du tableau de bord. */
public final class DashboardDto {

    private DashboardDto() {
    }

    /** Indicateurs affiches sur l'accueil. */
    public record Resume(int totalDemandes,
                         long demandesBrouillon,
                         long demandesEnValidation,
                         long demandesACorriger,
                         long demandesFinalisees,
                         long demandesRefusees,
                         long demandesAnnulees,
                         long lignesAValider,
                         long lignesAcceptees,
                         long lignesACorriger,
                         String etapeValidation,
                         int exercice,
                         BigDecimal budgetAlloue,
                         BigDecimal budgetEngage,
                         BigDecimal budgetDisponible) {
    }
}
