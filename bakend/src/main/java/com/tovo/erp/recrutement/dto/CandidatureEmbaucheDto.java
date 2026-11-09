package com.tovo.erp.recrutement.dto;

import com.tovo.erp.recrutement.model.Candidature;
import com.tovo.erp.recrutement.model.CandidatureStatut;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Vue d'une candidature retenue, partagee avec le module embauche sans exposer
 * les entites du module recrutement.
 */
public record CandidatureEmbaucheDto(
        Long idCandidature,
        Long idCandidat,
        String nom,
        String prenom,
        String email,
        String telephone,
        String adresse,
        String cin,
        LocalDate dateNaissance,
        Long idOffre,
        String referenceOffre,
        String titreOffre,
        String typeContratOffre,
        String lieuTravailOffre,
        Integer effectifRecherche,
        BigDecimal salaireMinOffre,
        BigDecimal salaireMaxOffre,
        Long idDemandePoste,
        CandidatureStatut statut,
        LocalDateTime dateCandidature) {

    /** Vrai si la candidature est retenue pour recevoir une proposition. */
    public boolean estRetenue() {
        return statut == CandidatureStatut.RETENUE;
    }

    /** Vrai si la candidature permet encore de poursuivre l'embauche. */
    public boolean estEmbauchable() {
        return statut == CandidatureStatut.RETENUE || statut == CandidatureStatut.ACCEPTEE;
    }

    public static CandidatureEmbaucheDto de(Candidature candidature) {
        var candidat = candidature.getCandidat();
        var offre = candidature.getOffre();
        return new CandidatureEmbaucheDto(
                candidature.getIdCandidature(),
                candidat.getIdCandidat(),
                candidat.getNom(),
                candidat.getPrenom(),
                candidat.getEmail(),
                candidat.getTelephone(),
                candidat.getAdresse(),
                candidat.getCin(),
                candidat.getDateNaissance(),
                offre.getIdOffre(),
                offre.getReference(),
                offre.getTitre(),
                offre.getTypeContrat(),
                offre.getLieuTravail(),
                offre.getEffectifRecherche(),
                offre.getSalaireMin(),
                offre.getSalaireMax(),
                offre.getIdDemandePoste(),
                candidature.getStatut(),
                candidature.getDateCandidature());
    }
}
