package com.tovo.erp.iam.service;

import com.tovo.erp.shared.BusinessException;
import com.tovo.erp.iam.model.Utilisateur;
import com.tovo.erp.iam.repository.UtilisateurRepository;
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
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UtilisateurRepository utilisateurRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.utilisateurRepository = utilisateurRepository;
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

        return new AuthDto.Reponse(jwtService.genererJeton(utilisateur),
                jwtService.expiration(),
                AuthDto.Profil.de(utilisateur));
    }

    /** Profil de l'utilisateur connecte (RM-003). */
    public AuthDto.Profil profil(Utilisateur utilisateur) {
        return AuthDto.Profil.de(utilisateur);
    }
}
