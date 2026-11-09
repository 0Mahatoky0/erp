package com.tovo.erp.recrutement.controller;

import com.tovo.erp.shared.Roles;
import com.tovo.erp.recrutement.dto.PassageTestDto;
import com.tovo.erp.recrutement.dto.TestRecrutementDto;
import com.tovo.erp.recrutement.model.PassageTest;
import com.tovo.erp.recrutement.model.TestRecrutement;
import com.tovo.erp.recrutement.repository.PassageTestRepository;
import com.tovo.erp.recrutement.repository.TestRecrutementRepository;
import com.tovo.erp.recrutement.service.EntretienTestService;
import com.tovo.erp.shared.UtilisateurCourant;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recrutement")
public class TestRecrutementController {

    private final EntretienTestService entretienTests;
    private final TestRecrutementRepository testRepository;
    private final PassageTestRepository passageRepository;
    private final UtilisateurCourant utilisateurCourant;

    public TestRecrutementController(EntretienTestService entretienTests, TestRecrutementRepository testRepository,
                                     PassageTestRepository passageRepository,
                                     UtilisateurCourant utilisateurCourant) {
        this.entretienTests = entretienTests;
        this.testRepository = testRepository;
        this.passageRepository = passageRepository;
        this.utilisateurCourant = utilisateurCourant;
    }

    @PostMapping("/tests")
    public ResponseEntity<TestRecrutementDto> creerTest(@Valid @RequestBody TestRecrutement test) {
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION);
        return ResponseEntity.status(HttpStatus.CREATED).body(TestRecrutementDto.de(entretienTests.creerTest(test)));
    }

    @GetMapping("/offres/{idOffre}/tests")
    public List<TestRecrutementDto> testsParOffre(@PathVariable Long idOffre) {
        return testRepository.findByOffreIdOffre(idOffre).stream().map(TestRecrutementDto::de).toList();
    }

    @PostMapping("/passages-tests")
    public ResponseEntity<PassageTestDto> planifierPassage(@Valid @RequestBody PassageTest passage) {
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PassageTestDto.de(entretienTests.planifierPassage(passage)));
    }

    @GetMapping("/candidatures/{id}/passages-tests")
    public List<PassageTestDto> passagesParCandidature(@PathVariable Long id) {
        return passageRepository.findByCandidatureIdCandidature(id).stream().map(PassageTestDto::de).toList();
    }
}
