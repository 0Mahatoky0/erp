package com.tovo.erp.recrutement.service;

import com.tovo.erp.recrutement.model.*;
import com.tovo.erp.recrutement.repository.*;
import com.tovo.erp.recrutement.dto.DecisionCandidatureDto;
import com.tovo.erp.recrutement.dto.EvaluationCandidatDto;
import com.tovo.erp.recrutement.dto.RecrutementRequests;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

@Service
public class EvaluationDecisionService {
    private static final ObjectMapper JSON = new ObjectMapper();
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
        if (request.decision() == DecisionType.LISTE_ATTENTE) {
            decision.setRangListeAttente(
                    decisions.dernierRangListeAttente(DecisionType.LISTE_ATTENTE) + 1);
        }
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
        if (request.criteres() != null && !request.criteres().isEmpty()) {
            evaluation.setCriteres(serialiserCriteres(request.criteres()));
            evaluation.setNoteGlobale(calculerNoteGlobale(request.criteres()));
        }
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
        EvaluationCandidat sauvegardee = evaluations.save(evaluation);
        recalculerClassement(sauvegardee.getCandidature());
        return sauvegardee;
    }

    private String serialiserCriteres(List<RecrutementRequests.CritereEvaluation> criteres) {
        try {
            return JSON.writeValueAsString(criteres);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Impossible d'enregistrer la grille d'evaluation", ex);
        }
    }

    private BigDecimal calculerNoteGlobale(List<RecrutementRequests.CritereEvaluation> criteres) {
        BigDecimal poidsTotal = BigDecimal.ZERO;
        BigDecimal somme = BigDecimal.ZERO;
        for (RecrutementRequests.CritereEvaluation critere : criteres) {
            if (critere.note() == null) {
                continue;
            }
            BigDecimal poids = critere.poids() == null || critere.poids().signum() <= 0
                    ? BigDecimal.ONE : critere.poids();
            poidsTotal = poidsTotal.add(poids);
            somme = somme.add(critere.note().multiply(poids));
        }
        return poidsTotal.signum() == 0 ? null : somme.divide(poidsTotal, 2, RoundingMode.HALF_UP);
    }

    /** Met a jour les scores de la candidature puis son rang parmi les candidatures de l'offre. */
    private void recalculerClassement(Candidature candidature) {
        if (candidature == null || candidature.getIdCandidature() == null) {
            return;
        }
        List<EvaluationCandidat> toutes = evaluations.findByCandidatureIdCandidature(
                candidature.getIdCandidature());
        if (toutes == null) {
            toutes = List.of();
        }
        candidature.setScoreDossier(moyenneSurVingt(toutes, TypeEvaluation.DOSSIER));
        candidature.setScoreGlobal(moyenneSurVingt(toutes, null));
        candidatures.save(candidature);

        Long idOffre = candidature.getOffre() == null ? null : candidature.getOffre().getIdOffre();
        if (idOffre == null) {
            return;
        }
        List<Candidature> classees = candidatures.findByOffreIdOffre(idOffre);
        if (classees == null || classees.isEmpty()) {
            return;
        }
        classees.sort(Comparator
                .comparing(Candidature::getScoreGlobal, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Candidature::getDateCandidature, Comparator.nullsLast(Comparator.naturalOrder())));
        int rang = 1;
        for (Candidature classe : classees) {
            classe.setRang(classe.getScoreGlobal() == null ? null : rang++);
        }
        candidatures.saveAll(classees);
    }

    private BigDecimal moyenneSurVingt(List<EvaluationCandidat> evaluations, TypeEvaluation type) {
        List<BigDecimal> notes = evaluations.stream()
                .filter(evaluation -> type == null || evaluation.getTypeEvaluation() == type)
                .map(EvaluationDecisionService::noteSurVingt)
                .filter(note -> note != null)
                .toList();
        if (notes.isEmpty()) {
            return null;
        }
        BigDecimal somme = notes.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return somme.divide(BigDecimal.valueOf(notes.size()), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal noteSurVingt(EvaluationCandidat evaluation) {
        if (evaluation.getNoteGlobale() != null) {
            return evaluation.getNoteGlobale();
        }
        if (evaluation.getNote() == null || evaluation.getNoteMax() == null
                || evaluation.getNoteMax().signum() <= 0) {
            return null;
        }
        return evaluation.getNote().multiply(BigDecimal.valueOf(20))
                .divide(evaluation.getNoteMax(), 4, RoundingMode.HALF_UP);
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
