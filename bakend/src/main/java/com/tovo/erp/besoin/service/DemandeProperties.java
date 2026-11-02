package com.tovo.erp.besoin.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Regles de planification des demandes (RM-014).
 *
 * @param delaiMinimum  delai minimum entre la creation de la demande et la date souhaitee
 */
@ConfigurationProperties(prefix = "erp.demande")
public record DemandeProperties(Duration delaiMinimum) {

    public DemandeProperties {
        if (delaiMinimum == null) {
            delaiMinimum = Duration.ZERO;
        }
    }
}
