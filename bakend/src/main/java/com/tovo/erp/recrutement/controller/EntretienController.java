package com.tovo.erp.recrutement.controller;

import com.tovo.erp.recrutement.dto.EntretienDto;
import com.tovo.erp.recrutement.dto.RecrutementRequests;
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
public class EntretienController {

    private final EntretienTestService entretienTests;
    private final UtilisateurCourant utilisateurCourant;

    public EntretienController(EntretienTestService entretienTests,
                               UtilisateurCourant utilisateurCourant) {
        this.entretienTests = entretienTests;
        this.utilisateurCourant = utilisateurCourant;
    }

    @PostMapping("/entretiens")
    public ResponseEntity<EntretienDto> planifier(
            @Valid @RequestBody RecrutementRequests.EntretienRequest request) {
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(entretienTests.planifierEntretien(request, utilisateurCourant.exiger().getId()));
    }

    @GetMapping("/candidatures/{id}/entretiens")
    public List<EntretienDto> parCandidature(@PathVariable Long id) {
        return entretienTests.entretiensDe(id);
    }
}
