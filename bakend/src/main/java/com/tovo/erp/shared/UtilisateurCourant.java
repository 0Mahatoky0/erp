package com.tovo.erp.shared;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Acces a l'utilisateur authentifie et verifications de roles (RM-004, RM-067).
 */
@Component
public class UtilisateurCourant {

    /** Retourne l'utilisateur connecte, ou leve une erreur si l'appel n'est pas authentifie. */
    public UtilisateurConnecte exiger() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UtilisateurConnecte utilisateur)) {
            throw BusinessException.forbidden("Authentification requise.");
        }
        return utilisateur;
    }

    /** Retourne l'utilisateur connecte, ou null s'il n'y en a pas. */
    public UtilisateurConnecte optionnel() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UtilisateurConnecte utilisateur) {
            return utilisateur;
        }
        return null;
    }

    /** Vrai si l'utilisateur connecte a l'un des roles donnes. */
    public boolean aLeRole(String... roles) {
        UtilisateurConnecte utilisateur = optionnel();
        if (utilisateur == null) {
            return false;
        }
        for (String role : roles) {
            if (role.equals(utilisateur.getRoleNom())) {
                return true;
            }
        }
        return false;
    }

    /** Leve une erreur si l'utilisateur connecte n'a pas l'un des roles demandes (RM-067). */
    public void exigerRole(String... roles) {
        UtilisateurConnecte utilisateur = exiger();
        for (String role : roles) {
            if (role.equals(utilisateur.getRoleNom())) {
                return;
            }
        }
        throw BusinessException.forbidden(
                "Cette operation requiert le role " + describe(roles) + " (vous etes "
                        + utilisateur.getRoleNom() + ").");
    }

    private String describe(String... roles) {
        StringBuilder libelles = new StringBuilder();
        for (String role : roles) {
            if (libelles.length() > 0) {
                libelles.append(" ou ");
            }
            libelles.append(role);
        }
        return libelles.toString();
    }
}
