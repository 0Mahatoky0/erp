package com.tovo.erp.web.dto;

import com.tovo.erp.domain.entity.Budget;

import java.math.BigDecimal;

/** Objets de transfert des budgets de service (RM-048 a RM-051). */
public final class BudgetDto {

    private BudgetDto() {
    }

    /** Budget annuel d'un service avec son etat de consommation. */
    public record Liste(Long id, Long idService, String service, String departement, Integer exercice,
                        BigDecimal montantAlloue, BigDecimal montantDepense, BigDecimal montantEngage,
                        BigDecimal montantDisponible) {

        public static Liste de(Budget budget) {
            return new Liste(
                    budget.getId(),
                    budget.getService().getId(),
                    budget.getService().getNom(),
                    budget.getService().getDepartement().getNom(),
                    budget.getExercice(),
                    budget.getMontantAlloue(),
                    budget.getMontantDepense(),
                    budget.getMontantEngage(),
                    budget.getMontantDisponible());
        }
    }

    /** Creation ou mise a jour du budget alloue a un service pour un exercice. */
    public record MiseAJour(Long idService, Integer exercice, java.math.BigDecimal montantAlloue) {
    }
}
