package com.tovo.erp.recrutement.controller;

import com.tovo.erp.shared.Roles;
import com.tovo.erp.recrutement.dto.DecisionCandidatureDto;
import com.tovo.erp.recrutement.dto.RecrutementRequests;
import com.tovo.erp.recrutement.model.DecisionCandidature;
import com.tovo.erp.recrutement.repository.CandidatureRepository;
import com.tovo.erp.recrutement.service.EvaluationDecisionService;
import com.tovo.erp.shared.UtilisateurCourant;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recrutement/decisions")
public class DecisionCandidatureController {

    private final EvaluationDecisionService evaluations;
    private final CandidatureRepository candidatureRepository;
    private final UtilisateurCourant utilisateurCourant;

    public DecisionCandidatureController(EvaluationDecisionService evaluations,
                                         CandidatureRepository candidatureRepository,
                                         UtilisateurCourant utilisateurCourant) {
        this.evaluations = evaluations;
        this.candidatureRepository = candidatureRepository;
        this.utilisateurCourant = utilisateurCourant;
    }

    @PostMapping
    public ResponseEntity<DecisionCandidatureDto> decider(@Valid @RequestBody RecrutementRequests.DecisionRequest r) {
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION);
        DecisionCandidature d = new DecisionCandidature();
        d.setCandidature(candidatureRepository.getReferenceById(r.idCandidature()));
        d.setIdUtilisateur(utilisateurCourant.exiger().getId());
        d.setDecision(r.decision());
        d.setMotif(r.motif());
        return ResponseEntity.status(HttpStatus.CREATED).body(DecisionCandidatureDto.de(evaluations.decider(d)));
    }
}
