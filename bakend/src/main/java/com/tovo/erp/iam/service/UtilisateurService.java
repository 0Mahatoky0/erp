package com.tovo.erp.service;

import com.tovo.erp.common.BusinessException;
import com.tovo.erp.common.Recherche;
import com.tovo.erp.domain.entity.Departement;
import com.tovo.erp.domain.entity.Role;
import com.tovo.erp.domain.entity.Utilisateur;
import com.tovo.erp.domain.enums.RoleNom;
import com.tovo.erp.repository.DepartementRepository;
import com.tovo.erp.repository.RoleRepository;
import com.tovo.erp.repository.UtilisateurRepository;
import com.tovo.erp.web.dto.AuthDto;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Gestion des comptes utilisateurs (RM-003, RM-004).
 */
@Service
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final DepartementRepository departementRepository;
    private final PasswordEncoder passwordEncoder;

    public UtilisateurService(UtilisateurRepository utilisateurRepository,
                               RoleRepository roleRepository,
                               DepartementRepository departementRepository,
                               PasswordEncoder passwordEncoder) {
        this.utilisateurRepository = utilisateurRepository;
        this.roleRepository = roleRepository;
        this.departementRepository = departementRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Liste des utilisateurs filtres par role, departement et recherche. */
    @Transactional(readOnly = true)
    public List<AuthDto.Profil> lister(String role, Long idDepartement, String recherche, Boolean actif) {
        return utilisateurRepository.rechercher(role, idDepartement, Recherche.motif(recherche), actif).stream()
                .map(AuthDto.Profil::de)
                .toList();
    }

    /** Cree un compte avec un mot de passe hache (RM-002). */
    @Transactional
    public AuthDto.Profil creer(String nom, String email, String motDePasse, String role,
                                Long idDepartement) {
        if (utilisateurRepository.existsByEmailIgnoreCase(email.trim())) {
            throw new BusinessException("EMAIL_DEJA_UTILISE",
                    "Un compte existe deja avec l'adresse " + email.trim() + ".");
        }
        if (motDePasse == null || motDePasse.length() < 6) {
            throw new BusinessException("MOT_DE_PASSE_INVALIDE",
                    "Le mot de passe doit contenir au moins 6 caracteres.");
        }
        Role roleEntity = roleRepository.findByNom(role)
                .orElseThrow(() -> BusinessException.notFound("Le role " + role + " n'existe pas."));
        Departement departement = null;
        if (idDepartement != null) {
            departement = departementRepository.findById(idDepartement)
                    .orElseThrow(() -> BusinessException.notFound(
                            "Le departement " + idDepartement + " n'existe pas."));
        }

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setNom(nom.trim());
        utilisateur.setEmail(email.trim());
        utilisateur.setMotDePasse(passwordEncoder.encode(motDePasse));
        utilisateur.setRole(roleEntity);
        utilisateur.setDepartement(departement);
        utilisateur.setActif(true);
        utilisateurRepository.save(utilisateur);
        return AuthDto.Profil.de(utilisateur);
    }

    /** Active ou desactive un compte (RM-003). */
    @Transactional
    public AuthDto.Profil changerActivation(Long id, boolean actif) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("L'utilisateur " + id + " n'existe pas."));
        utilisateur.setActif(actif);
        utilisateurRepository.save(utilisateur);
        return AuthDto.Profil.de(utilisateur);
    }

    /** Liste des roles disponibles. */
    @Transactional(readOnly = true)
    public List<String> roles() {
        return roleRepository.findAll().stream().map(Role::getNom).sorted().toList();
    }

}
