package com.tovo.erp.recrutement.dto;

import com.tovo.erp.recrutement.domain.*;
import java.time.LocalDateTime;

public record HistoriqueCandidatureDto(Long idHistorique, Long idCandidature, Long idUtilisateur,
                                       AuteurType auteurType, CandidatureStatut ancienStatut,
                                       CandidatureStatut nouveauStatut, String commentaire,
                                       LocalDateTime dateAction) {
    public static HistoriqueCandidatureDto de(HistoriqueCandidature h) {
        return new HistoriqueCandidatureDto(h.getIdHistorique(), h.getCandidature().getIdCandidature(),
                h.getIdUtilisateur(), h.getAuteurType(), h.getAncienStatut(), h.getNouveauStatut(),
                h.getCommentaire(), h.getDateAction());
    }
}
