package com.tovo.erp.web.controller;

import com.tovo.erp.domain.enums.RoleNom;
import com.tovo.erp.security.UtilisateurCourant;
import com.tovo.erp.service.UtilisateurService;
import com.tovo.erp.web.dto.AuthDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Gestion des comptes utilisateurs (RM-003, RM-004). */
@RestController
@RequestMapping("/api/utilisateurs")
public class UtilisateurController {

    private final UtilisateurService utilisateurService;
    private final UtilisateurCourant utilisateurCourant;

    public UtilisateurController(UtilisateurService utilisateurService, UtilisateurCourant utilisateurCourant) {
        this.utilisateurService = utilisateurService;
        this.utilisateurCourant = utilisateurCourant;
    }

    @GetMapping
    public ResponseEntity<List<AuthDto.Profil>> lister(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Long idDepartement,
            @RequestParam(required = false) String recherche,
            @RequestParam(required = false) Boolean actif) {
        utilisateurCourant.exigerRole(RoleNom.RH);
        return ResponseEntity.ok(utilisateurService.lister(role, idDepartement, recherche, actif));
    }

    @GetMapping("/roles")
    public ResponseEntity<List<String>> roles() {
        return ResponseEntity.ok(utilisateurService.roles());
    }

    @PostMapping
    public ResponseEntity<AuthDto.Profil> creer(@RequestBody Map<String, Object> demande) {
        utilisateurCourant.exigerRole(RoleNom.RH);
        return ResponseEntity.ok(utilisateurService.creer(
                (String) demande.get("nom"),
                (String) demande.get("email"),
                (String) demande.get("motDePasse"),
                (String) demande.get("role"),
                demande.get("idDepartement") == null ? null : Long.valueOf(demande.get("idDepartement").toString())));
    }

    @PostMapping("/{id}/activation")
    public ResponseEntity<AuthDto.Profil> changerActivation(@PathVariable Long id,
                                                            @RequestParam boolean actif) {
        utilisateurCourant.exigerRole(RoleNom.RH);
        return ResponseEntity.ok(utilisateurService.changerActivation(id, actif));
    }
}
