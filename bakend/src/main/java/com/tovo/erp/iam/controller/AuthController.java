package com.tovo.erp.web.controller;

import com.tovo.erp.security.UtilisateurCourant;
import com.tovo.erp.service.AuthService;
import com.tovo.erp.web.dto.AuthDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Connexion et profil de l'utilisateur connecte (RM-001, RM-003). */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UtilisateurCourant utilisateurCourant;

    public AuthController(AuthService authService, UtilisateurCourant utilisateurCourant) {
        this.authService = authService;
        this.utilisateurCourant = utilisateurCourant;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDto.Reponse> connexion(@Valid @RequestBody AuthDto.Connexion demande) {
        return ResponseEntity.ok(authService.connexion(demande.email(), demande.motDePasse()));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthDto.Profil> profil() {
        return ResponseEntity.ok(authService.profil(utilisateurCourant.exiger()));
    }
}
