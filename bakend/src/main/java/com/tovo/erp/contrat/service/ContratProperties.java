package com.tovo.erp.contrat.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Parametres metier du bloc 4. Les durees juridiques ne sont jamais codees en
 * dur : elles sont surchargeables par configuration et a valider par un juriste
 * (OUR_IA.md, A3.11).
 */
@ConfigurationProperties(prefix = "erp.contrat")
public record ContratProperties(
        @DefaultValue("24") int dureeMaxCddMois,
        @DefaultValue("1") int renouvellementsCddMax,
        @DefaultValue("30") int onboardingJours,
        @DefaultValue("3") int nonPresentationJours,
        @DefaultValue("3") int signatureAlerteJours,
        @DefaultValue("30") int cddAlerteJours1,
        @DefaultValue("15") int cddAlerteJours2,
        @DefaultValue("7") int cddAlerteJours3,
        @DefaultValue("15") int essaiAlerteJours1,
        @DefaultValue("7") int essaiAlerteJours2) {
}
