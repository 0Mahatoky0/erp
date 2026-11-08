package com.tovo.erp.recrutement.dto;

import com.tovo.erp.recrutement.model.Candidature;
import com.tovo.erp.recrutement.model.CandidatureStatut;

import java.time.LocalDateTime;

/** Representation sans cycle de la candidature et de ses relations utiles a l'UI. */
public record CandidatureDto(
        Long idCandidature,
        CandidatDto candidat,
        OffreEmploiDto.OffreResumeDto offre,
        String messageCandidat,
        String source,
        CandidatureStatut statut,
        LocalDateTime dateCandidature,
        LocalDateTime dateModification) {

    public static CandidatureDto de(Candidature candidature) {
        var offre = candidature.getOffre();
        return new CandidatureDto(
                candidature.getIdCandidature(),
                CandidatDto.de(candidature.getCandidat()),
                offre == null ? null : OffreEmploiDto.resume(offre),
                candidature.getMessageCandidat(),
                candidature.getSource(),
                candidature.getStatut(),
                candidature.getDateCandidature(),
                candidature.getDateModification());
    }
}
