package com.tovo.erp.embauche.controller;

import com.tovo.erp.iam.model.RoleNom;
import com.tovo.erp.embauche.dto.*;
import com.tovo.erp.embauche.service.PropositionEmbaucheService;
import com.tovo.erp.shared.UtilisateurCourant;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/embauche/propositions")
public class PropositionEmbaucheController {
    private final PropositionEmbaucheService service;
    private final UtilisateurCourant utilisateurCourant;

    public PropositionEmbaucheController(PropositionEmbaucheService service,
                                         UtilisateurCourant utilisateurCourant) {
        this.service = service;
        this.utilisateurCourant = utilisateurCourant;
    }

    @PostMapping
    public ResponseEntity<PropositionEmbaucheDto> creer(
            @Valid @RequestBody EmbaucheRequests.PropositionRequest request) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.creer(request));
    }

    @GetMapping
    public List<PropositionEmbaucheDto> lister() {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION, RoleNom.CANDIDAT);
        return service.lister(utilisateurCourant.exiger());
    }

    @GetMapping("/{id}")
    public PropositionEmbaucheDto charger(@PathVariable Long id) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION, RoleNom.CANDIDAT);
        return service.charger(id, utilisateurCourant.exiger());
    }

    @PostMapping("/{id}/reponse")
    public ResponseEntity<ReponsePropositionDto> repondre(
            @PathVariable Long id,
            @Valid @RequestBody EmbaucheRequests.ReponsePropositionRequest request) {
        utilisateurCourant.exigerRole(RoleNom.CANDIDAT);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.repondre(id, request, utilisateurCourant.exiger()));
    }

    @GetMapping("/{id}/reponses")
    public List<ReponsePropositionDto> reponses(@PathVariable Long id) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION, RoleNom.CANDIDAT);
        return service.reponses(id, utilisateurCourant.exiger());
    }
}
