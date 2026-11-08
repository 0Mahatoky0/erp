package com.tovo.erp.embauche.controller;

import com.tovo.erp.iam.model.RoleNom;
import com.tovo.erp.embauche.dto.*;
import com.tovo.erp.embauche.service.ContratEmbaucheService;
import com.tovo.erp.shared.UtilisateurCourant;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Les URLs sont identiques à celles d'avant la séparation
 * (/api/embauche/propositions/...), seul le contrôleur change.
 */
@RestController
@RequestMapping("/api/embauche/propositions")
public class ContratEmbaucheController {
    private final ContratEmbaucheService service;
    private final UtilisateurCourant utilisateurCourant;

    public ContratEmbaucheController(ContratEmbaucheService service,
                                     UtilisateurCourant utilisateurCourant) {
        this.service = service;
        this.utilisateurCourant = utilisateurCourant;
    }

    @PostMapping("/{id}/contrat")
    public ResponseEntity<ContratEmbaucheDto> creer(@PathVariable Long id) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.creer(id, utilisateurCourant.exiger()));
    }

    @GetMapping("/contrats")
    public List<ContratEmbaucheDto> lister() {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION, RoleNom.CANDIDAT);
        return service.lister(utilisateurCourant.exiger());
    }

    @GetMapping("/contrats/{id}")
    public ContratEmbaucheDto charger(@PathVariable Long id) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION, RoleNom.CANDIDAT);
        return service.charger(id, utilisateurCourant.exiger());
    }

    @PostMapping("/contrats/{id}/reponse")
    public ContratEmbaucheDto repondre(
            @PathVariable Long id,
            @Valid @RequestBody EmbaucheRequests.ReponseContratRequest request) {
        utilisateurCourant.exigerRole(RoleNom.CANDIDAT);
        return service.repondre(id, request, utilisateurCourant.exiger());
    }

    @PostMapping("/contrats/{id}/validation")
    public ContratEmbaucheDto valider(@PathVariable Long id) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION);
        return service.valider(id, utilisateurCourant.exiger());
    }
}