package com.tovo.erp.recrutement.dto;

import com.tovo.erp.recrutement.domain.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EvaluationCandidatDto(Long idEvaluation, Long idCandidature, Long idUtilisateurEvaluateur,
                                     TypeEvaluation typeEvaluation, Long idEntretien, Long idPassage,
                                     BigDecimal note, BigDecimal noteMax, String appreciation,
                                     String commentaire, LocalDateTime dateEvaluation) {
    public static EvaluationCandidatDto de(EvaluationCandidat e) {
        return new EvaluationCandidatDto(e.getIdEvaluation(), e.getCandidature().getIdCandidature(),
                e.getIdUtilisateurEvaluateur(), e.getTypeEvaluation(),
                e.getEntretien() == null ? null : e.getEntretien().getIdEntretien(),
                e.getPassage() == null ? null : e.getPassage().getIdPassage(),
                e.getNote(), e.getNoteMax(), e.getAppreciation(), e.getCommentaire(), e.getDateEvaluation());
    }
}
