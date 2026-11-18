package com.tovo.erp.evaluation.controller;

import com.tovo.erp.evaluation.dto.EvaluationDtos;
import com.tovo.erp.evaluation.service.EvaluationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/evaluation")
@PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER', 'EMPLOYE')")
public class EvaluationController {
    private final EvaluationService service;

    public EvaluationController(EvaluationService service) {
        this.service = service;
    }

    @GetMapping("/mes-evaluations")
    public List<EvaluationDtos.EvaluationDto> mesEvaluations() {
        return service.mesEvaluations();
    }

    @GetMapping("/evaluations")
    public List<EvaluationDtos.EvaluationDto> evaluations(@RequestParam Long idCampagne) {
        return service.evaluations(idCampagne);
    }

    @GetMapping("/evaluations/{idEvaluation}")
    public EvaluationDtos.EvaluationDto evaluation(@PathVariable Long idEvaluation) {
        return service.evaluation(idEvaluation);
    }

    @PostMapping("/evaluations/demarrer")
    public EvaluationDtos.EvaluationDto demarrer(@RequestParam Long idCampagne, @RequestParam Long idEmploye) {
        return service.demarrer(idCampagne, idEmploye);
    }

    @PostMapping("/evaluations/{idEvaluation}/auto")
    public EvaluationDtos.EvaluationDto autoEvaluer(@PathVariable Long idEvaluation,
                                                    @Valid @RequestBody EvaluationDtos.AutoEvaluationRequest request) {
        return service.autoEvaluer(idEvaluation, request);
    }

    @PostMapping("/evaluations/{idEvaluation}/manager")
    public EvaluationDtos.EvaluationDto evaluerManager(@PathVariable Long idEvaluation,
                                                       @Valid @RequestBody EvaluationDtos.EvaluationManagerRequest request) {
        return service.evaluerManager(idEvaluation, request);
    }

    @PostMapping("/evaluations/{idEvaluation}/entretien")
    public EvaluationDtos.EvaluationDto entretien(@PathVariable Long idEvaluation,
                                                  @Valid @RequestBody EvaluationDtos.EntretienRequest request) {
        return service.entretien(idEvaluation, request);
    }

    @PostMapping("/evaluations/{idEvaluation}/n2")
    public EvaluationDtos.EvaluationDto validerN2(@PathVariable Long idEvaluation,
                                                  @Valid @RequestBody EvaluationDtos.ValidationN2Request request) {
        return service.validerN2(idEvaluation, request);
    }

    @PostMapping("/evaluations/{idEvaluation}/signature")
    public EvaluationDtos.EvaluationDto signer(@PathVariable Long idEvaluation,
                                               @Valid @RequestBody EvaluationDtos.SignatureRequest request) {
        return service.signer(idEvaluation, request);
    }

    @PostMapping("/evaluations/{idEvaluation}/contestation")
    public EvaluationDtos.EvaluationDto contester(@PathVariable Long idEvaluation,
                                                  @Valid @RequestBody EvaluationDtos.ContestationRequest request) {
        return service.contester(idEvaluation, request);
    }

    @PostMapping("/evaluations/{idEvaluation}/nouvelle-version")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public EvaluationDtos.EvaluationDto nouvelleVersion(@PathVariable Long idEvaluation,
                                                        @RequestParam String motif) {
        return service.nouvelleVersion(idEvaluation, motif);
    }

    @PostMapping("/evaluations/{idEvaluation}/cloturer")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public EvaluationDtos.EvaluationDto clore(@PathVariable Long idEvaluation) {
        return service.clore(idEvaluation);
    }

    @GetMapping("/employes/{idEmploye}/ecarts")
    public List<EvaluationDtos.EcartDto> ecarts(@PathVariable Long idEmploye) {
        return service.ecartsEmploye(idEmploye);
    }

    @GetMapping("/employes/{idEmploye}/besoins")
    public List<EvaluationDtos.BesoinFormationDto> besoins(@PathVariable Long idEmploye) {
        return service.besoinsEmploye(idEmploye);
    }

    @PostMapping("/besoins-formation")
    public EvaluationDtos.BesoinFormationDto creerBesoin(
            @Valid @RequestBody EvaluationDtos.BesoinFormationRequest request) {
        return service.creerBesoin(request);
    }

    @PostMapping("/besoins-formation/{idBesoin}/decision")
    public EvaluationDtos.BesoinFormationDto deciderBesoin(@PathVariable Long idBesoin,
                                                           @Valid @RequestBody EvaluationDtos.DecisionBesoinRequest request) {
        return service.deciderBesoin(idBesoin, request);
    }

    @PostMapping("/ecarts/{idEcart}/combler")
    public EvaluationDtos.EcartDto comblerEcart(@PathVariable Long idEcart,
                                                @Valid @RequestBody EvaluationDtos.EmployeCompetenceRequest request) {
        return service.comblerEcart(idEcart, request);
    }
}
