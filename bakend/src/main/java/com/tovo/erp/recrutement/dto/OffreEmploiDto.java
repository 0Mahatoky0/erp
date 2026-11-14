package com.tovo.erp.recrutement.dto;

import com.tovo.erp.recrutement.model.OffreEmploi;
import com.tovo.erp.recrutement.model.OffreStatut;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

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
        String criteresEliminatoires,
        String grilleEvaluation,
        Integer poidsDossier,
        Integer poidsEntretien,
        Integer poidsTest,
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
                offre.getCriteresEliminatoires(),
                offre.getGrilleEvaluation(),
                offre.getPoidsDossier(),
                offre.getPoidsEntretien(),
                offre.getPoidsTest(),
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

    /** Corps de creation/modification d'une offre, sans exposition de l'entite. */
    public record Request(
            @NotNull Long idDemandePoste,
            @NotBlank @Size(max = 50) String reference,
            @NotBlank @Size(max = 150) String titre,
            @NotBlank String description,
            @NotBlank String profilRecherche,
            @Size(max = 255) String lieuTravail,
            @Size(max = 50) String typeContrat,
            @NotNull @Min(1) Integer effectifRecherche,
            @PositiveOrZero BigDecimal salaireMin,
            @PositiveOrZero BigDecimal salaireMax,
            @NotBlank String modeSelection,
            String criteresEliminatoires,
            String grilleEvaluation,
            @Positive Integer poidsDossier,
            @Positive Integer poidsEntretien,
            @Positive Integer poidsTest,
            LocalDateTime dateLimiteCandidature) {
    }
}
