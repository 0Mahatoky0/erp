package com.tovo.erp.recrutement.controller;

import com.tovo.erp.shared.Roles;
import com.tovo.erp.recrutement.dto.EvaluationCandidatDto;
import com.tovo.erp.recrutement.dto.RecrutementRequests;
import com.tovo.erp.recrutement.model.EvaluationCandidat;
import com.tovo.erp.recrutement.repository.CandidatureRepository;
import com.tovo.erp.recrutement.repository.EntretienRepository;
import com.tovo.erp.recrutement.repository.EvaluationCandidatRepository;
import com.tovo.erp.recrutement.repository.PassageTestRepository;
import com.tovo.erp.recrutement.service.EvaluationDecisionService;
import com.tovo.erp.shared.UtilisateurCourant;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recrutement")
public class EvaluationCandidatController {

    private final EvaluationDecisionService evaluations;
    private final EvaluationCandidatRepository evaluationRepository;
    private final CandidatureRepository candidatureRepository;
    private final EntretienRepository entretienRepository;
    private final PassageTestRepository passageRepository;
    private final UtilisateurCourant utilisateurCourant;

    public EvaluationCandidatController(EvaluationDecisionService evaluations,
                                        EvaluationCandidatRepository evaluationRepository,
                                        CandidatureRepository candidatureRepository,
                                        EntretienRepository entretienRepository,
                                        PassageTestRepository passageRepository,
                                        UtilisateurCourant utilisateurCourant) {
        this.evaluations = evaluations;
        this.evaluationRepository = evaluationRepository;
        this.candidatureRepository = candidatureRepository;
        this.entretienRepository = entretienRepository;
        this.passageRepository = passageRepository;
        this.utilisateurCourant = utilisateurCourant;
    }

    @PostMapping("/evaluations")
    public ResponseEntity<EvaluationCandidatDto> evaluer(
            @Valid @RequestBody RecrutementRequests.EvaluationRequest request) {
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION);
        EvaluationCandidat evaluation = new EvaluationCandidat();
        evaluation.setCandidature(candidatureRepository.getReferenceById(request.idCandidature()));
        evaluation.setIdUtilisateurEvaluateur(utilisateurCourant.exiger().getId());
        evaluation.setTypeEvaluation(request.typeEvaluation());
        evaluation.setNote(request.note());
        evaluation.setNoteMax(request.noteMax());
        evaluation.setAppreciation(request.appreciation());
        evaluation.setCommentaire(request.commentaire());
        if (request.idEntretien() != null) {
            evaluation.setEntretien(entretienRepository.getReferenceById(request.idEntretien()));
        }
        if (request.idPassage() != null) {
            evaluation.setPassage(passageRepository.getReferenceById(request.idPassage()));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(EvaluationCandidatDto.de(evaluations.evaluer(evaluation)));
    }

    @GetMapping("/candidatures/{id}/evaluations")
    public List<EvaluationCandidatDto> parCandidature(@PathVariable Long id) {
        return evaluationRepository.findByCandidatureIdCandidature(id).stream()
                .map(EvaluationCandidatDto::de).toList();
    }
}
