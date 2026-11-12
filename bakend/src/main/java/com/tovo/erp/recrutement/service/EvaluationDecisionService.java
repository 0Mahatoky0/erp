package com.tovo.erp.recrutement.service;

import com.tovo.erp.recrutement.model.*;
import com.tovo.erp.recrutement.repository.*;
import com.tovo.erp.recrutement.dto.DecisionCandidatureDto;
import com.tovo.erp.recrutement.dto.EvaluationCandidatDto;
import com.tovo.erp.recrutement.dto.RecrutementRequests;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;

@Service
public class EvaluationDecisionService {
    private final EvaluationCandidatRepository evaluations;
    private final DecisionCandidatureRepository decisions;
    private final CandidatureRepository candidatures;
    private final EntretienRepository entretiens;
    private final PassageTestRepository passages;
    private final CandidatureService candidatureService;

    public EvaluationDecisionService(EvaluationCandidatRepository evaluations, DecisionCandidatureRepository decisions,
                                     CandidatureRepository candidatures, EntretienRepository entretiens,
                                     PassageTestRepository passages, CandidatureService candidatureService) {
        this.evaluations = evaluations; this.decisions = decisions;
        this.candidatures = candidatures; this.entretiens = entretiens;
        this.passages = passages; this.candidatureService = candidatureService;
    }

    @Transactional
    public DecisionCandidatureDto decider(RecrutementRequests.DecisionRequest request, Long idUtilisateur) {
        DecisionCandidature decision = new DecisionCandidature();
        decision.setCandidature(candidatures.getReferenceById(request.idCandidature()));
        decision.setIdUtilisateur(idUtilisateur);
        decision.setDecision(request.decision());
        decision.setMotif(request.motif());
        return DecisionCandidatureDto.de(decider(decision));
    }

    @Transactional
    public EvaluationCandidatDto evaluer(RecrutementRequests.EvaluationRequest request, Long idEvaluateuteur) {
        EvaluationCandidat evaluation = new EvaluationCandidat();
        evaluation.setCandidature(candidatures.getReferenceById(request.idCandidature()));
        evaluation.setIdUtilisateurEvaluateur(idEvaluateuteur);
        evaluation.setTypeEvaluation(request.typeEvaluation());
        evaluation.setNote(request.note());
        evaluation.setNoteMax(request.noteMax());
        evaluation.setAppreciation(request.appreciation());
        evaluation.setCommentaire(request.commentaire());
        if (request.idEntretien() != null) {
            evaluation.setEntretien(entretiens.getReferenceById(request.idEntretien()));
        }
        if (request.idPassage() != null) {
            evaluation.setPassage(passages.getReferenceById(request.idPassage()));
        }
        return EvaluationCandidatDto.de(evaluer(evaluation));
    }

    @Transactional
    public List<EvaluationCandidatDto> evaluationsDe(Long idCandidature) {
        return evaluations.findByCandidatureIdCandidature(idCandidature).stream()
                .map(EvaluationCandidatDto::de)
                .toList();
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
