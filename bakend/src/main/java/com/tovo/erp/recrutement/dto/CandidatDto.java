package com.tovo.erp.recrutement.dto;

import com.tovo.erp.recrutement.domain.Candidat;

import java.time.LocalDateTime;

/** Donnees publiques d'un candidat, sans reference vers ses candidatures. */
public record CandidatDto(
        Long idCandidat,
        String nom,
        String prenom,
        String email,
        String telephone,
        String adresse,
        LocalDateTime dateCreation,
        LocalDateTime dateModification) {

    public static CandidatDto de(Candidat candidat) {
        return new CandidatDto(
                candidat.getIdCandidat(),
                candidat.getNom(),
                candidat.getPrenom(),
                candidat.getEmail(),
                candidat.getTelephone(),
                candidat.getAdresse(),
                candidat.getDateCreation(),
                candidat.getDateModification());
    }
}
