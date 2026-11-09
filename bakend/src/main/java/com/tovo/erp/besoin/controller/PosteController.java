package com.tovo.erp.besoin.controller;

import com.tovo.erp.shared.Roles;
import com.tovo.erp.shared.UtilisateurCourant;
import com.tovo.erp.besoin.service.PosteService;
import com.tovo.erp.besoin.dto.PosteDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Catalogue des postes officiels (RM-006 a RM-011).
 * La lecture est ouverte a tous les connectes, l'ecriture est reservee au role RH.
 */
@RestController
@RequestMapping("/api/postes")
public class PosteController {

    private final PosteService posteService;
    private final UtilisateurCourant utilisateurCourant;

    public PosteController(PosteService posteService, UtilisateurCourant utilisateurCourant) {
        this.posteService = posteService;
        this.utilisateurCourant = utilisateurCourant;
    }

    @GetMapping
    public ResponseEntity<List<PosteDto.Liste>> lister(
            @RequestParam(required = false) Long idService,
            @RequestParam(required = false) Boolean actif,
            @RequestParam(required = false) String recherche) {
        return ResponseEntity.ok(posteService.lister(idService, actif, recherche));
    }

    @PostMapping
    public ResponseEntity<PosteDto.Liste> creer(@Valid @RequestBody PosteDto.Creation demande) {
        utilisateurCourant.exigerRole(Roles.RH);
        return ResponseEntity.ok(posteService.creer(demande));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PosteDto.Liste> modifier(@PathVariable Long id,
                                                   @Valid @RequestBody PosteDto.Modification demande) {
        utilisateurCourant.exigerRole(Roles.RH);
        return ResponseEntity.ok(posteService.modifier(id, demande));
    }

    @PostMapping("/{id}/desactiver")
    public ResponseEntity<PosteDto.Liste> desactiver(@PathVariable Long id) {
        utilisateurCourant.exigerRole(Roles.RH);
        return ResponseEntity.ok(posteService.desactiver(id));
    }
}
