package com.tovo.erp.shared;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Fait metier emis lorsqu'un mouvement de carriere est applique. */
public record MouvementAppliqueEvent(
        Long idMouvement,
        Long idEmploye,
        String type,
        Long idPosteOrigine,
        Long idPosteDestination,
        Long idServiceOrigine,
        Long idServiceDestination,
        Long idManagerOrigine,
        Long idManagerDestination,
        String lieuOrigine,
        String lieuDestination,
        LocalDate dateEffet,
        BigDecimal salaireAncien,
        BigDecimal salaireNouveau,
        Long idUtilisateurApplication) {
}
