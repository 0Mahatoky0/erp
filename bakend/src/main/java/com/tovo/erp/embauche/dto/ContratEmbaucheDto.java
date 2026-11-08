package com.tovo.erp.embauche.dto;

import com.tovo.erp.embauche.model.ContratEmbauche;
import com.tovo.erp.embauche.model.ContratStatut;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ContratEmbaucheDto(
        Long idContrat, Long idPropositionEmbauche, Long idCandidat, Long idEmploye,
        String typeContrat, BigDecimal salaire, String lieuTravail,
        LocalDateTime dateDebut, LocalDateTime dateFin, ContratStatut statut,
        LocalDateTime dateCreation, LocalDateTime dateReponse, LocalDateTime dateValidation) {

    public static ContratEmbaucheDto de(ContratEmbauche contrat) {
        return new ContratEmbaucheDto(
                contrat.getIdContrat(),
                contrat.getProposition().getIdPropositionEmbauche(),
                contrat.getIdCandidat(),
                contrat.getIdEmploye(),
                contrat.getTypeContrat(),
                contrat.getSalaire(),
                contrat.getLieuTravail(),
                contrat.getDateDebut(),
                contrat.getDateFin(),
                contrat.getStatut(),
                contrat.getDateCreation(),
                contrat.getDateReponse(),
                contrat.getDateValidation());
    }
}
