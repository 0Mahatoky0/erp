package com.tovo.erp.employe.dto;

import com.tovo.erp.employe.model.Alerte;
import com.tovo.erp.employe.model.NiveauAlerte;
import com.tovo.erp.employe.model.TypeAlerte;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Alerte du dossier administratif lue par l'ecran RH (bloc 5). */
public record AlerteDto(
        Long id,
        Long idEmploye,
        TypeAlerte typeAlerte,
        NiveauAlerte niveau,
        String message,
        LocalDate dateEcheance,
        boolean lue,
        LocalDateTime dateCreation) {

    public static AlerteDto de(Alerte alerte) {
        return new AlerteDto(alerte.getId(), alerte.getIdEmploye(), alerte.getTypeAlerte(),
                alerte.getNiveau(), alerte.getMessage(), alerte.getDateEcheance(),
                alerte.isLue(), alerte.getDateCreation());
    }
}
