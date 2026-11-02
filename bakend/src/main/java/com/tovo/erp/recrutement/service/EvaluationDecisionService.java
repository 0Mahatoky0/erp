package com.tovo.erp.recrutement.service;

import com.tovo.erp.recrutement.model.*;
import com.tovo.erp.recrutement.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class EvaluationDecisionService {
    private final EvaluationCandidatRepository evaluations;
    private final DecisionCandidatureRepository decisions;
    private final CandidatureRepository candidatures;
    private final CandidatureService candidatureService;

    public EvaluationDecisionService(EvaluationCandidatRepository evaluations, DecisionCandidatureRepository decisions,
                                     CandidatureRepository candidatures, CandidatureService candidatureService) {
        this.evaluations = evaluations; this.decisions = decisions;
        this.candidatures = candidatures; this.candidatureService = candidatureService;
    }

    @Transactional
    public EvaluationCandidat evaluer(EvaluationCandidat evaluation) {
        if (!candidatures.existsById(evaluation.getCandidature().getIdCandidature()))
            throw new IllegalArgumentException("Candidature introuvable");
        BigDecimal note = evaluation.getNote();
        if (note != null && (note.signum() < 0 || note.compareTo(evaluation.getNoteMax()) > 0))
            throw new IllegalArgumentException("Note hors barème");
        return evaluations.save(evaluation);
    }

    @Transactional
    public DecisionCandidature decider(DecisionCandidature decision) {
        if (decision.getMotif() == null || decision.getMotif().isBlank())
            throw new IllegalArgumentException("Le motif de décision est obligatoire");
        DecisionCandidature saved = decisions.save(decision);
        CandidatureStatut statut = switch (decision.getDecision()) {
            case RETENUE -> CandidatureStatut.RETENUE;
            case REFUSEE -> CandidatureStatut.REFUSEE;
            case LISTE_ATTENTE -> CandidatureStatut.LISTE_ATTENTE;
        };
        candidatureService.changerStatut(decision.getCandidature().getIdCandidature(), statut,
                decision.getIdUtilisateur(), decision.getMotif());
        return saved;
    }
}
