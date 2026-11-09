package com.tovo.erp.iam.service;

import com.tovo.erp.shared.BusinessException;
import com.tovo.erp.shared.UtilisateurConnecte;
import com.tovo.erp.iam.model.Utilisateur;
import com.tovo.erp.iam.model.Role;
import com.tovo.erp.iam.model.RoleNom;
import com.tovo.erp.iam.repository.UtilisateurRepository;
import com.tovo.erp.iam.repository.RoleRepository;
import com.tovo.erp.shared.JwtService;
import com.tovo.erp.iam.dto.AuthDto;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Connexion et deconnexion des utilisateurs (RM-001 a RM-003).
 */
@Service
public class AuthService {

    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UtilisateurRepository utilisateurRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.utilisateurRepository = utilisateurRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** Verifie les identifiants et renvoie un jeton de connexion. */
    @Transactional(readOnly = true)
    public AuthDto.Reponse connexion(String email, String motDePasse) {
        Utilisateur utilisateur = utilisateurRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(BusinessException::identifiantsInvalides);

        if (!utilisateur.isActif()) {
            throw new BusinessException("COMPTE_INACTIF",
                    "Ce compte est desactive. Contactez l'administrateur.",
                    org.springframework.http.HttpStatus.UNAUTHORIZED);
        }

        if (!passwordEncoder.matches(motDePasse, utilisateur.getMotDePasse())) {
            throw BusinessException.identifiantsInvalides();
        }

        return reponse(utilisateur);
    }

    @Transactional
    public AuthDto.Reponse inscrireCandidat(AuthDto.Inscription demande) {
        Utilisateur existant = utilisateurRepository.findByEmailIgnoreCase(demande.email().trim()).orElse(null);
        if (existant != null) {
            if (!RoleNom.CANDIDAT.name().equals(existant.getRole().getNom()) || existant.isActif()) {
                throw new BusinessException("EMAIL_DEJA_UTILISE",
                        "Un compte existe deja avec cette adresse e-mail.");
            }
            existant.setNom(demande.prenom().trim() + " " + demande.nom().trim());
            existant.setMotDePasse(passwordEncoder.encode(demande.motDePasse()));
            existant.setActif(true);
            utilisateurRepository.save(existant);
            return reponse(existant);
        }
        Role role = roleRepository.findByNom(RoleNom.CANDIDAT.name())
                .orElseThrow(() -> new BusinessException("ROLE_CANDIDAT_ABSENT",
                        "Le role candidat n'est pas configure."));
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setNom(demande.prenom().trim() + " " + demande.nom().trim());
        utilisateur.setEmail(demande.email().trim());
        utilisateur.setMotDePasse(passwordEncoder.encode(demande.motDePasse()));
        utilisateur.setRole(role);
        utilisateur.setActif(true);
        utilisateurRepository.save(utilisateur);
        return reponse(utilisateur);
    }

    /** Profil de l'utilisateur connecte (RM-003), sans recharger l'entite. */
    public AuthDto.Profil profil(UtilisateurConnecte utilisateur) {
        return new AuthDto.Profil(
                utilisateur.getId(),
                utilisateur.getNom(),
                utilisateur.getEmail(),
                RoleNom.valueOf(utilisateur.getRoleNom()),
                utilisateur.getDepartement() == null ? null : utilisateur.getDepartement().getId(),
                utilisateur.getDepartement() == null ? null : utilisateur.getDepartement().getNom(),
                true);
    }

    private AuthDto.Reponse reponse(Utilisateur utilisateur) {
        String departementNom = utilisateur.getIdDepartement() == null
                ? null
                : utilisateurRepository.trouverNomDepartement(utilisateur.getIdDepartement());
        return new AuthDto.Reponse(
                jwtService.genererJeton(utilisateur.getId(), utilisateur.getEmail(), utilisateur.getNom(),
                        utilisateur.getRole().getNom(), utilisateur.getIdDepartement(), departementNom),
                jwtService.expiration(),
                AuthDto.Profil.de(utilisateur, departementNom));
    }
}
