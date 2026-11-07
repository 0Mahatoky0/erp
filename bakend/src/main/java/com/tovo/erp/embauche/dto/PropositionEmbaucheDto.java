package com.tovo.erp.embauche.dto;

import com.tovo.erp.embauche.model.PropositionEmbauche;
import com.tovo.erp.embauche.model.PropositionStatut;
import com.tovo.erp.recrutement.dto.CandidatDto;
import com.tovo.erp.recrutement.dto.CandidatureDto;
import com.tovo.erp.recrutement.dto.OffreEmploiDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PropositionEmbaucheDto(
        Long idPropositionEmbauche,
        Long idCandidature,
        CandidatDto candidat,
        OffreEmploiDto.OffreResumeDto offre,
        String typeContrat,
        BigDecimal salairePropose,
        String lieuTravail,
        LocalDateTime dateDebut,
        LocalDateTime dateFin,
        PropositionStatut statut,
        LocalDateTime dateProposition) {

    public static PropositionEmbaucheDto de(PropositionEmbauche proposition, CandidatureDto candidature) {
        return new PropositionEmbaucheDto(
                proposition.getIdPropositionEmbauche(),
                proposition.getIdCandidature(),
                candidature.candidat(),
                candidature.offre(),
                proposition.getTypeContrat(),
                proposition.getSalairePropose(),
                proposition.getLieuTravail(),
                proposition.getDateDebut(),
                proposition.getDateFin(),
                proposition.getStatut(),
                proposition.getDateProposition());
    }
}
