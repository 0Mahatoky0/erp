package com.tovo.erp.recrutement.controller;

import com.tovo.erp.recrutement.dto.EvaluationCandidatDto;
import com.tovo.erp.recrutement.dto.RecrutementRequests;
import com.tovo.erp.recrutement.dto.SyntheseEvaluationDto;
import com.tovo.erp.recrutement.service.EvaluationDecisionService;
import com.tovo.erp.recrutement.service.SyntheseEvaluationService;
import com.tovo.erp.shared.Roles;
import com.tovo.erp.shared.UtilisateurCourant;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recrutement")
public class EvaluationCandidatController {

    private final EvaluationDecisionService evaluations;
    private final SyntheseEvaluationService syntheses;
    private final UtilisateurCourant utilisateurCourant;

    public EvaluationCandidatController(EvaluationDecisionService evaluations,
                                        SyntheseEvaluationService syntheses,
                                        UtilisateurCourant utilisateurCourant) {
        this.evaluations = evaluations;
        this.syntheses = syntheses;
        this.utilisateurCourant = utilisateurCourant;
    }

    @PostMapping("/evaluations")
    public ResponseEntity<EvaluationCandidatDto> evaluer(
            @Valid @RequestBody RecrutementRequests.EvaluationRequest request) {
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(evaluations.evaluer(request, utilisateurCourant.exiger().getId()));
    }

    @GetMapping("/candidatures/{id}/evaluations")
    public List<EvaluationCandidatDto> parCandidature(@PathVariable Long id) {
        return evaluations.evaluationsDe(id);
    }

    /** Notes par categorie (dossier, entretien, test) de toutes les candidatures d'une offre. */
    @GetMapping("/offres/{idOffre}/evaluations/synthese")
    public List<SyntheseEvaluationDto> syntheseDeLOffre(@PathVariable Long idOffre,
                                                        @RequestParam(required = false) String statut) {
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION, Roles.MANAGER);
        return syntheses.parOffre(idOffre, statut);
    }

    /** Notes par categorie d'une candidature, pour sa fiche d'evaluation. */
    @GetMapping("/candidatures/{id}/evaluations/synthese")
    public SyntheseEvaluationDto syntheseDeLaCandidature(@PathVariable Long id) {
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION);
        return syntheses.pourCandidature(id);
    }
}
