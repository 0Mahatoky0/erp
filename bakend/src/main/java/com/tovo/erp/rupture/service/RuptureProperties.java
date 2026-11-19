package com.tovo.erp.rupture.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "erp.rupture")
public record RuptureProperties(
        @DefaultValue("30") int preavisDemissionJours,
        @DefaultValue("30") int preavisLicenciementJours,
        @DefaultValue("30") int preavisRuptureAmiableJours,
        @DefaultValue("30") int preavisRetraiteJours,
        @DefaultValue("0") int preavisDefautJours,
        @DefaultValue("10") int indemniteJoursParAnnee,
        @DefaultValue("30") int delaiPaiementJours,
        @DefaultValue("5") int conservationAnnees,
        @DefaultValue("6") int accesCoffreMois) {
}
