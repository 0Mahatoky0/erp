package com.tovo.erp.evaluation.controller;

import com.tovo.erp.evaluation.dto.EvaluationDtos;
import com.tovo.erp.evaluation.service.FormationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/evaluation/formation")
@PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER', 'EMPLOYE', 'FINANCE')")
public class FormationController {
    private final FormationService service;

    public FormationController(FormationService service) {
        this.service = service;
    }

    @GetMapping("/catalogue")
    public List<EvaluationDtos.FormationDto> formations() {
        return service.formations();
    }

    @PostMapping("/catalogue")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public EvaluationDtos.FormationDto creerFormation(@Valid @RequestBody EvaluationDtos.FormationRequest request) {
        return service.creerFormation(request);
    }

    @GetMapping("/sessions")
    public List<EvaluationDtos.SessionDto> sessions() {
        return service.sessions();
    }

    @PostMapping("/sessions")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public EvaluationDtos.SessionDto creerSession(@Valid @RequestBody EvaluationDtos.SessionRequest request) {
        return service.creerSession(request);
    }

    @PostMapping("/sessions/{idSession}/statut")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public EvaluationDtos.SessionDto changerStatutSession(@PathVariable Long idSession, @RequestParam String statut) {
        return service.changerStatutSession(idSession, statut);
    }

    @GetMapping("/plans")
    public List<EvaluationDtos.PlanFormationDto> plans(@RequestParam(required = false) Integer annee) {
        return service.plans(annee == null ? java.time.Year.now().getValue() : annee);
    }

    @PostMapping("/plans")
    @PreAuthorize("hasAnyRole('RH', 'FINANCE', 'DIRECTION')")
    public EvaluationDtos.PlanFormationDto enregistrerPlan(@Valid @RequestBody EvaluationDtos.PlanFormationRequest request) {
        return service.enregistrerPlan(request);
    }

    @GetMapping("/sessions/{idSession}/inscriptions")
    public List<EvaluationDtos.InscriptionDto> inscriptionsSession(@PathVariable Long idSession) {
        return service.inscriptionsSession(idSession);
    }

    @GetMapping("/employes/{idEmploye}/inscriptions")
    public List<EvaluationDtos.InscriptionDto> inscriptionsEmploye(@PathVariable Long idEmploye) {
        return service.inscriptionsEmploye(idEmploye);
    }

    @GetMapping("/mes-inscriptions")
    public List<EvaluationDtos.InscriptionDto> mesInscriptions() {
        return service.mesInscriptions();
    }

    @PostMapping("/inscriptions")
    public EvaluationDtos.InscriptionDto demanderInscription(
            @Valid @RequestBody EvaluationDtos.InscriptionRequest request) {
        return service.demanderInscription(request);
    }

    @PostMapping("/sessions/{idSession}/inscriptions")
    public EvaluationDtos.InscriptionDto demanderInscriptionSession(
            @PathVariable Long idSession,
            @RequestParam(required = false) Long idEmploye,
            @RequestParam(required = false) Long idBesoin) {
        if (idEmploye == null) {
            return service.demanderInscriptionPourMoi(idSession, idBesoin);
        }
        return service.demanderInscription(
                new EvaluationDtos.InscriptionRequest(idSession, idEmploye, idBesoin));
    }

    @PostMapping("/inscriptions/moi")
    public EvaluationDtos.InscriptionDto demanderInscriptionPourMoi(@RequestParam Long idSession,
                                                                    @RequestParam(required = false) Long idBesoin) {
        return service.demanderInscriptionPourMoi(idSession, idBesoin);
    }

    @PostMapping("/inscriptions/{idInscription}/decision-manager")
    public EvaluationDtos.InscriptionDto deciderInscriptionManager(@PathVariable Long idInscription,
                                                                   @Valid @RequestBody EvaluationDtos.DecisionInscriptionRequest request) {
        return service.deciderInscriptionManager(idInscription, request);
    }

    @PostMapping("/inscriptions/{idInscription}/confirmer")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public EvaluationDtos.InscriptionDto confirmerInscription(@PathVariable Long idInscription) {
        return service.confirmerInscription(idInscription);
    }

    @PostMapping("/inscriptions/{idInscription}/annuler")
    public EvaluationDtos.InscriptionDto annulerInscription(@PathVariable Long idInscription) {
        return service.annulerInscription(idInscription);
    }

    @PostMapping("/inscriptions/{idInscription}/presence")
    public EvaluationDtos.InscriptionDto enregistrerPresence(@PathVariable Long idInscription,
                                                             @Valid @RequestBody EvaluationDtos.PresenceRequest request) {
        return service.enregistrerPresence(idInscription, request);
    }

    @PostMapping("/inscriptions/{idInscription}/evaluation")
    public EvaluationDtos.EvaluationFormationDto evaluerFormation(@PathVariable Long idInscription,
                                                                  @Valid @RequestBody EvaluationDtos.EvaluationFormationRequest request) {
        return service.evaluerFormation(idInscription, request);
    }

    @GetMapping("/inscriptions/{idInscription}/evaluations")
    public List<EvaluationDtos.EvaluationFormationDto> evaluationsInscription(@PathVariable Long idInscription) {
        return service.evaluationsInscription(idInscription);
    }

    @PostMapping("/inscriptions/{idInscription}/mesure-90j")
    public EvaluationDtos.EvaluationFormationDto mesurer90Jours(@PathVariable Long idInscription,
                                                                @Valid @RequestBody EvaluationDtos.EmployeCompetenceRequest request) {
        return service.mesurer90Jours(idInscription, request);
    }

    @PostMapping("/inscriptions/{idInscription}/convention")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'FINANCE')")
    public void enregistrerConvention(@PathVariable Long idInscription,
                                      @Valid @RequestBody EvaluationDtos.ConventionFormationRequest request) {
        service.enregistrerConvention(idInscription, request);
    }

    @GetMapping("/inscriptions/{idInscription}/remboursement")
    public EvaluationDtos.RemboursementDto remboursement(@PathVariable Long idInscription,
                                                         @RequestParam int moisRestants) {
        return service.remboursement(idInscription, moisRestants);
    }

    @GetMapping("/indicateurs")
    public EvaluationDtos.IndicateursDto indicateurs() {
        return service.indicateurs();
    }
}
