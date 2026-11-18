package com.tovo.erp.evaluation.controller;

import com.tovo.erp.evaluation.dto.EvaluationDtos;
import com.tovo.erp.evaluation.service.ReferentielService;
import com.tovo.erp.shared.UtilisateurCourant;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("evaluationReferentielController")
@RequestMapping("/api/evaluation/referentiel")
@PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
public class ReferentielController {
    private final ReferentielService service;
    private final UtilisateurCourant utilisateurCourant;

    public ReferentielController(ReferentielService service, UtilisateurCourant utilisateurCourant) {
        this.service = service;
        this.utilisateurCourant = utilisateurCourant;
    }

    @GetMapping("/competences")
    public List<EvaluationDtos.CompetenceDto> competences() {
        return service.competences();
    }

    @PostMapping("/competences")
    public EvaluationDtos.CompetenceDto creerCompetence(@Valid @RequestBody EvaluationDtos.CompetenceRequest request) {
        return service.creerCompetence(request);
    }

    @GetMapping("/postes/{idPoste}/competences")
    public List<EvaluationDtos.PosteCompetenceDto> posteCompetences(@PathVariable Long idPoste) {
        return service.posteCompetences(idPoste);
    }

    @PostMapping("/postes/competences")
    public EvaluationDtos.PosteCompetenceDto enregistrerPosteCompetence(
            @Valid @RequestBody EvaluationDtos.PosteCompetenceRequest request) {
        return service.enregistrerPosteCompetence(request);
    }

    @GetMapping("/employes/{idEmploye}/competences")
    public List<EvaluationDtos.EmployeCompetenceDto> employeCompetences(@PathVariable Long idEmploye) {
        return service.employeCompetences(idEmploye);
    }

    @PostMapping("/employes/{idEmploye}/competences")
    public EvaluationDtos.EmployeCompetenceDto enregistrerNiveau(
            @PathVariable Long idEmploye, @Valid @RequestBody EvaluationDtos.EmployeCompetenceRequest request) {
        if (!idEmploye.equals(request.idEmploye())) {
            throw new IllegalArgumentException("L'employé du chemin et de la requête doivent correspondre.");
        }
        return service.enregistrerNiveau(request, utilisateurCourant.exiger().getId());
    }
}
