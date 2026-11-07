package com.tovo.erp.embauche.dto;

import com.tovo.erp.embauche.model.DecisionProposition;
import com.tovo.erp.embauche.model.ReponseProposition;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReponsePropositionDto(
        Long idReponse,
        Long idPropositionEmbauche,
        DecisionProposition decision,
        BigDecimal salaireSouhaite,
        String lieuSouhaite,
        String typeContratSouhaite,
        LocalDateTime dateDebutSouhaitee,
        LocalDateTime dateFinSouhaitee,
        String commentaire,
        LocalDateTime dateReponse) {

    public static ReponsePropositionDto de(ReponseProposition reponse) {
        return new ReponsePropositionDto(
                reponse.getIdReponse(),
                reponse.getProposition().getIdPropositionEmbauche(),
                reponse.getDecision(),
                reponse.getSalaireSouhaite(),
                reponse.getLieuSouhaite(),
                reponse.getTypeContratSouhaite(),
                reponse.getDateDebutSouhaitee(),
                reponse.getDateFinSouhaitee(),
                reponse.getCommentaire(),
                reponse.getDateReponse());
    }
}
