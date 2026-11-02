package com.tovo.erp.security;

import com.tovo.erp.common.BusinessException;
import com.tovo.erp.domain.entity.Utilisateur;
import com.tovo.erp.domain.enums.RoleNom;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Acces a l'utilisateur authentifie et verifications de roles (RM-004, RM-067).
 */
@Component
public class UtilisateurCourant {

    /** Retourne l'utilisateur connecte, ou leve une erreur si l'appel n'est pas authentifie. */
    public Utilisateur exiger() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Utilisateur utilisateur)) {
            throw BusinessException.forbidden("Authentification requise.");
        }
        return utilisateur;
    }

    /** Retourne l'utilisateur connecte, ou null s'il n'y en a pas. */
    public Utilisateur optionnel() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Utilisateur utilisateur) {
            return utilisateur;
        }
        return null;
    }

    /** Vrai si l'utilisateur connecte a l'un des roles donnes. */
    public boolean aLeRole(RoleNom... roles) {
        Utilisateur utilisateur = optionnel();
        if (utilisateur == null) {
            return false;
        }
        for (RoleNom role : roles) {
            if (role.name().equals(utilisateur.getRole().getNom())) {
                return true;
            }
        }
        return false;
    }

    /** Leve une erreur si l'utilisateur connecte n'a pas l'un des roles demandes (RM-067). */
    public void exigerRole(RoleNom... roles) {
        Utilisateur utilisateur = exiger();
        for (RoleNom role : roles) {
            if (role.name().equals(utilisateur.getRole().getNom())) {
                return;
            }
        }
        throw BusinessException.forbidden(
                "Cette operation requiert le role " + describe(roles) + " (vous etes "
                        + utilisateur.getRole().getNom() + ").");
    }

    private String describe(RoleNom... roles) {
        StringBuilder libelles = new StringBuilder();
        for (RoleNom role : roles) {
            if (libelles.length() > 0) {
                libelles.append(" ou ");
            }
            libelles.append(role.name());
        }
        return libelles.toString();
    }
}
