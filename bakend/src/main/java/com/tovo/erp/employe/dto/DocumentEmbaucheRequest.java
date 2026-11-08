package com.tovo.erp.employe.dto;

import com.tovo.erp.employe.model.TypeDocumentEmploye;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Depot d'une piece numerique dans le dossier salarie (bloc 5). */
public record DocumentEmbaucheRequest(
        @NotNull TypeDocumentEmploye typeDocument,
        @NotBlank String titre,
        String nomFichier,
        String typeMime,
        @NotBlank String contenuBase64,
        LocalDate dateEmission,
        LocalDate dateValidite) {
}
