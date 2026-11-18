package com.tovo.erp.evaluation.controller;

import com.tovo.erp.evaluation.dto.EvaluationDtos;
import com.tovo.erp.evaluation.service.CampagneService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/evaluation")
@PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER', 'EMPLOYE')")
public class CampagneController {
    private final CampagneService service;

    public CampagneController(CampagneService service) {
        this.service = service;
    }

    @GetMapping("/campagnes")
    public List<EvaluationDtos.CampagneDto> campagnes() {
        return service.campagnes();
    }

    @PostMapping("/campagnes")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public EvaluationDtos.CampagneDto creerCampagne(@Valid @RequestBody EvaluationDtos.CampagneRequest request) {
        return service.creerCampagne(request);
    }

    @PostMapping("/campagnes/{idCampagne}/statut")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public EvaluationDtos.CampagneDto changerStatut(@PathVariable Long idCampagne, @RequestParam String statut) {
        return service.changerStatut(idCampagne, statut);
    }

    @GetMapping("/campagnes/{idCampagne}/objectifs")
    public List<EvaluationDtos.ObjectifDto> objectifs(@PathVariable Long idCampagne, @RequestParam Long idEmploye) {
        return service.objectifs(idEmploye, idCampagne);
    }

    @PostMapping("/objectifs")
    public EvaluationDtos.ObjectifDto creerObjectif(@Valid @RequestBody EvaluationDtos.ObjectifRequest request) {
        return service.creerObjectif(request);
    }

    @PostMapping("/objectifs/{idObjectif}/avenant")
    public EvaluationDtos.ObjectifAvenantResult avenant(@PathVariable Long idObjectif,
                                                        @Valid @RequestBody EvaluationDtos.ObjectifAvenantRequest request) {
        return service.avenant(idObjectif, request);
    }

    @PostMapping("/objectifs/validation")
    public EvaluationDtos.ObjectifValidationDto validerObjectifs(@RequestParam Long idEmploye,
                                                                 @RequestParam Long idCampagne) {
        return service.validerObjectifs(idEmploye, idCampagne);
    }
}
