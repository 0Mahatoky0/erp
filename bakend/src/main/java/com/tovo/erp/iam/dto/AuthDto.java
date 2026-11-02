package com.tovo.erp.iam.dto;

import com.tovo.erp.iam.model.Utilisateur;
import com.tovo.erp.iam.model.RoleNom;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

/** Objets de transfert pour l'authentification. */
public final class AuthDto {

    private AuthDto() {
    }

    /** Identifiants de connexion (RM-001). */
    public record Connexion(@Email @NotBlank String email, @NotBlank String motDePasse) {
    }

    /** Reponse de connexion : jeton, expiration et profil. */
    public record Reponse(String token, Instant expiration, Profil profil) {
    }

    /** Profil applicatif de l'utilisateur : sert aussi a la gestion des comptes. */
    public record Profil(Long id, String nom, String email, RoleNom role,
                         Long departementId, String departementNom, boolean actif) {

        public static Profil de(Utilisateur utilisateur) {
            return new Profil(
                    utilisateur.getId(),
                    utilisateur.getNom(),
                    utilisateur.getEmail(),
                    RoleNom.valueOf(utilisateur.getRole().getNom()),
                    utilisateur.getDepartement() == null ? null : utilisateur.getDepartement().getId(),
                    utilisateur.getDepartement() == null ? null : utilisateur.getDepartement().getNom(),
                    utilisateur.isActif());
        }
    }
}
