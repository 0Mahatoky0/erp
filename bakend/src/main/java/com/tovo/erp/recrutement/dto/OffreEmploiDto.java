package com.tovo.erp.recrutement.dto;

import com.tovo.erp.recrutement.model.OffreEmploi;
import com.tovo.erp.recrutement.model.OffreStatut;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Representation d'une offre destinee aux clients HTTP. */
public record OffreEmploiDto(
        Long idOffre,
        Long idDemandePoste,
        Long idUtilisateurCreateur,
        String reference,
        String titre,
        String description,
        String profilRecherche,
        String lieuTravail,
        String typeContrat,
        Integer effectifRecherche,
        BigDecimal salaireMin,
        BigDecimal salaireMax,
        String modeSelection,
        OffreStatut statut,
        LocalDateTime datePublication,
        LocalDateTime dateLimiteCandidature,
        LocalDateTime dateCloture,
        LocalDateTime dateCreation,
        LocalDateTime dateModification) {

    public static OffreEmploiDto de(OffreEmploi offre) {
        return new OffreEmploiDto(
                offre.getIdOffre(),
                offre.getIdDemandePoste(),
                offre.getIdUtilisateurCreateur(),
                offre.getReference(),
                offre.getTitre(),
                offre.getDescription(),
                offre.getProfilRecherche(),
                offre.getLieuTravail(),
                offre.getTypeContrat(),
                offre.getEffectifRecherche(),
                offre.getSalaireMin(),
                offre.getSalaireMax(),
                offre.getModeSelection(),
                offre.getStatut(),
                offre.getDatePublication(),
                offre.getDateLimiteCandidature(),
                offre.getDateCloture(),
                offre.getDateCreation(),
                offre.getDateModification());
    }

    public static OffreResumeDto resume(OffreEmploi offre) {
        return new OffreResumeDto(
                offre.getIdOffre(),
                offre.getReference(),
                offre.getTitre(),
                offre.getStatut(),
                offre.getLieuTravail(),
                offre.getTypeContrat());
    }

    public record OffreResumeDto(
            Long idOffre,
            String reference,
            String titre,
            OffreStatut statut,
            String lieuTravail,
            String typeContrat) {
    }
}
