package com.tovo.erp.recrutement.dto;

import java.math.BigDecimal;

/** Indicateurs agreges d'une offre pour le pilotage du recrutement. */
public record OffreIndicateursDto(
        Long idOffre,
        long nombreCandidatures,
        long nombrePreselectionnees,
        BigDecimal tauxPreselection,
        long nombreEnEvaluation,
        long nombreRetenues,
        BigDecimal tauxRetenue,
        long nombreListeAttente,
        long nombreRefusees) {
}
