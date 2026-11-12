package com.tovo.erp.recrutement.controller;

import com.tovo.erp.recrutement.dto.DecisionCandidatureDto;
import com.tovo.erp.recrutement.dto.RecrutementRequests;
import com.tovo.erp.recrutement.service.EvaluationDecisionService;
import com.tovo.erp.shared.Roles;
import com.tovo.erp.shared.UtilisateurCourant;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recrutement/decisions")
public class DecisionCandidatureController {

    private final EvaluationDecisionService evaluations;
    private final UtilisateurCourant utilisateurCourant;

    public DecisionCandidatureController(EvaluationDecisionService evaluations,
                                         UtilisateurCourant utilisateurCourant) {
        this.evaluations = evaluations;
        this.utilisateurCourant = utilisateurCourant;
    }

    @PostMapping
    public ResponseEntity<DecisionCandidatureDto> decider(
            @Valid @RequestBody RecrutementRequests.DecisionRequest request) {
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(evaluations.decider(request, utilisateurCourant.exiger().getId()));
    }
}
