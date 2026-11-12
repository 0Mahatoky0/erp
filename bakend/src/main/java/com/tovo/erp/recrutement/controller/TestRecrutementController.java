package com.tovo.erp.recrutement.controller;

import com.tovo.erp.recrutement.dto.PassageTestDto;
import com.tovo.erp.recrutement.dto.RecrutementRequests;
import com.tovo.erp.recrutement.dto.TestRecrutementDto;
import com.tovo.erp.recrutement.service.EntretienTestService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recrutement")
public class TestRecrutementController {

    private final EntretienTestService entretienTests;
    private final UtilisateurCourant utilisateurCourant;

    public TestRecrutementController(EntretienTestService entretienTests,
                                     UtilisateurCourant utilisateurCourant) {
        this.entretienTests = entretienTests;
        this.utilisateurCourant = utilisateurCourant;
    }

    @PostMapping("/tests")
    public ResponseEntity<TestRecrutementDto> creerTest(
            @Valid @RequestBody RecrutementRequests.TestRequest request) {
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION);
        return ResponseEntity.status(HttpStatus.CREATED).body(entretienTests.creerTest(request));
    }

    @GetMapping("/offres/{idOffre}/tests")
    public List<TestRecrutementDto> testsParOffre(@PathVariable Long idOffre) {
        return entretienTests.testsParOffre(idOffre);
    }

    @PostMapping("/passages-tests")
    public ResponseEntity<PassageTestDto> planifierPassage(
            @Valid @RequestBody RecrutementRequests.PassageTestRequest request) {
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION);
        return ResponseEntity.status(HttpStatus.CREATED).body(entretienTests.planifierPassage(request));
    }

    @GetMapping("/candidatures/{id}/passages-tests")
    public List<PassageTestDto> passagesParCandidature(@PathVariable Long id) {
        return entretienTests.passagesParCandidature(id);
    }
}
