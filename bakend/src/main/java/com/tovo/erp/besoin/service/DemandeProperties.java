package com.tovo.erp.besoin.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Regles parametrables des demandes de recrutement (RM-014, RG-B1-05, RG-B1-09).
 *
 * @param delaiMinimum     delai minimum entre la creation de la demande et la date souhaitee
 * @param effectifMaximum  nombre maximum de postes demandes sur une meme ligne
 * @param moisParAn        nombre de mois retenus pour annualiser un salaire mensuel
 * @param facteurCharges   charges patronales appliquees au cout annuel (0.18 = 18 %)
 * @param expirationJours  delai apres soumission au-dela duquel une demande finalisee
 *                         sans offre publiee expire (RG-B1-14)
 * @param slaValidationJours delai cible de traitement d'une validation (RG-B1-13)
 * @param escaladeJours    delai au-dela duquel une validation en retard est escaladee
 */
@ConfigurationProperties(prefix = "erp.demande")
public record DemandeProperties(Duration delaiMinimum,
                                Integer effectifMaximum,
                                Integer moisParAn,
                                BigDecimal facteurCharges,
                                Integer expirationJours,
                                Integer slaValidationJours,
                                Integer escaladeJours) {

    public DemandeProperties {
        if (delaiMinimum == null) {
            delaiMinimum = Duration.ZERO;
        }
        if (effectifMaximum == null || effectifMaximum <= 0) {
            effectifMaximum = 50;
        }
        if (moisParAn == null || moisParAn <= 0) {
            moisParAn = 12;
        }
        if (facteurCharges == null || facteurCharges.signum() < 0) {
            facteurCharges = new BigDecimal("0.18");
        }
        if (expirationJours == null || expirationJours <= 0) {
            expirationJours = 60;
        }
        if (slaValidationJours == null || slaValidationJours <= 0) {
            slaValidationJours = 3;
        }
        if (escaladeJours == null || escaladeJours <= 0) {
            escaladeJours = 5;
        }
    }
}
