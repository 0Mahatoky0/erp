package com.tovo.erp.iam.service;

import com.tovo.erp.iam.model.Role;
import com.tovo.erp.iam.model.Utilisateur;
import com.tovo.erp.iam.repository.RoleRepository;
import com.tovo.erp.iam.repository.UtilisateurRepository;
import com.tovo.erp.shared.BusinessException;
import com.tovo.erp.shared.Roles;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleMobiliteService {

    private final UtilisateurRepository utilisateurs;
    private final RoleRepository roles;

    public RoleMobiliteService(UtilisateurRepository utilisateurs, RoleRepository roles) {
        this.utilisateurs = utilisateurs;
        this.roles = roles;
    }

    @Transactional
    public void definirManager(Long idUtilisateur) {
        Utilisateur utilisateur = utilisateur(idUtilisateur);
        if (!Roles.MANAGER.equals(utilisateur.getRole().getNom())) {
            utilisateur.setRole(role(Roles.MANAGER));
            utilisateurs.save(utilisateur);
        }
    }

    @Transactional
    public void retirerManager(Long idUtilisateur) {
        Utilisateur utilisateur = utilisateur(idUtilisateur);
        if (utilisateur.getRole() == null || !Roles.MANAGER.equals(utilisateur.getRole().getNom())) {
            return;
        }
        utilisateur.setRole(role(Roles.EMPLOYE));
        utilisateurs.save(utilisateur);
    }

    private Utilisateur utilisateur(Long id) {
        return utilisateurs.findById(id)
                .orElseThrow(() -> BusinessException.notFound("L'utilisateur " + id + " n'existe pas."));
    }

    private Role role(String nom) {
        return roles.findByNom(nom)
                .orElseThrow(() -> BusinessException.notFound("Le role " + nom + " n'existe pas."));
    }
}
