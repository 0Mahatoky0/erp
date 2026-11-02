package com.tovo.erp.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Parametres de securite de l'application (RM-001, RM-002).
 *
 * @param secret            cle secrete utilisee pour signer les jetons (256 bits minimum)
 * @param dureeValidite     duree de validite d'un jeton de connexion
 * @param emetteur          identifiant de l'emetteur des jetons
 */
@ConfigurationProperties(prefix = "erp.securite")
public record JwtProperties(String secret, Duration dureeValidite, String emetteur) {

    public JwtProperties {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                    "erp.securite.secret doit contenir au moins 32 caracteres.");
        }
        if (dureeValidite == null) {
            dureeValidite = Duration.ofHours(8);
        }
        if (emetteur == null || emetteur.isBlank()) {
            emetteur = "erp-tovo";
        }
    }
}
