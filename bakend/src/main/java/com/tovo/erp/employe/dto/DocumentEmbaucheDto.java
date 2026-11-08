package com.tovo.erp.employe.dto;

import com.tovo.erp.employe.model.ConfidentialiteDocument;
import com.tovo.erp.employe.model.DocumentEmploye;
import com.tovo.erp.employe.model.StatutDocument;
import com.tovo.erp.employe.model.TypeDocumentEmploye;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Metadonnees d'une piece du dossier salarie (le contenu se telecharge a part). */
public record DocumentEmbaucheDto(
        Long id,
        Long idEmploye,
        Long idContrat,
        TypeDocumentEmploye typeDocument,
        String titre,
        String nomFichier,
        String typeMime,
        String hashSha256,
        Integer version,
        StatutDocument statut,
        ConfidentialiteDocument confidentialite,
        LocalDate dateEmission,
        LocalDate dateValidite,
        LocalDateTime dateCreation) {

    public static DocumentEmbaucheDto de(DocumentEmploye document) {
        return new DocumentEmbaucheDto(
                document.getId(),
                document.getEmploye() == null ? null : document.getEmploye().getId(),
                document.getIdContrat(),
                document.getTypeDocument(),
                document.getTitre(),
                document.getNomFichier(),
                document.getTypeMime(),
                document.getHashSha256(),
                document.getVersion(),
                document.getStatut(),
                document.getConfidentialite(),
                document.getDateEmission(),
                document.getDateValidite(),
                document.getDateCreation());
    }
}
