package com.tovo.erp.recrutement.dto;

import com.tovo.erp.recrutement.model.*;
import java.time.LocalDateTime;

public record DecisionCandidatureDto(Long idDecision, Long idCandidature, Long idUtilisateur,
                                      DecisionType decision, String motif, LocalDateTime dateDecision) {
    public static DecisionCandidatureDto de(DecisionCandidature d) {
        return new DecisionCandidatureDto(d.getIdDecision(), d.getCandidature().getIdCandidature(),
                d.getIdUtilisateur(), d.getDecision(), d.getMotif(), d.getDateDecision());
    }
}
