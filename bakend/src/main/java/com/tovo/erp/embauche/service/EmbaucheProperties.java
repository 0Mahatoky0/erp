package com.tovo.erp.embauche.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.math.BigDecimal;

/** Parametres metier du module embauche, surchargeables par configuration. */
@ConfigurationProperties(prefix = "erp.embauche")
public record EmbaucheProperties(
        @DefaultValue("7") int validitePropositionJours,
        @DefaultValue("3") int delaiMinimumJours,
        @DefaultValue("3") int relanceJours,
        @DefaultValue("500000") BigDecimal salaireMinimumLegal,
        @DefaultValue("3") int maxTentativesPdf,
        @DefaultValue("3600000") long verificationExpirationMs) {
}
