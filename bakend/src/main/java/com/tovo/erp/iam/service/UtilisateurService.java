package com.tovo.erp.iam.service;

import com.tovo.erp.shared.BusinessException;
import com.tovo.erp.shared.Recherche;
import com.tovo.erp.iam.model.Role;
import com.tovo.erp.iam.model.Utilisateur;
import com.tovo.erp.iam.model.RoleNom;
import com.tovo.erp.iam.repository.RoleRepository;
import com.tovo.erp.iam.repository.UtilisateurRepository;
import com.tovo.erp.iam.dto.AuthDto;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Gestion des comptes utilisateurs (RM-003, RM-004).
 */
@Service
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UtilisateurService(UtilisateurRepository utilisateurRepository,
                               RoleRepository roleRepository,
                               PasswordEncoder passwordEncoder) {
        this.utilisateurRepository = utilisateurRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Liste des utilisateurs filtres par role, departement et recherche. */
    @Transactional(readOnly = true)
    public List<AuthDto.Profil> lister(String role, Long idDepartement, String recherche, Boolean actif) {
        Map<Long, String> nomsDepartements = nomsDepartements();
        return utilisateurRepository.rechercher(role, idDepartement, Recherche.motif(recherche), actif).stream()
                .map(u -> AuthDto.Profil.de(u, nomsDepartements.get(u.getIdDepartement())))
                .toList();
    }

    /** Resumes d'utilisateurs par identifiant, en une seule requete inter-modules. */
    @Transactional(readOnly = true)
    public Map<Long, AuthDto.Profil> profilsParIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> nomsDepartements = nomsDepartements();
        return utilisateurRepository.findAllById(ids).stream()
                .map(u -> AuthDto.Profil.de(u, nomsDepartements.get(u.getIdDepartement())))
                .collect(Collectors.toMap(AuthDto.Profil::id, Function.identity()));
    }

    /** Identifiants dont le nom correspond a la recherche (filtre inter-modules). */
    @Transactional(readOnly = true)
    public List<Long> idsParNom(String recherche) {
        String motif = Recherche.motif(recherche);
        return motif == null ? List.of() : utilisateurRepository.idsParNom(motif);
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
        if (idDepartement != null && utilisateurRepository.trouverNomDepartement(idDepartement) == null) {
            throw BusinessException.notFound("Le departement " + idDepartement + " n'existe pas.");
        }

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setNom(nom.trim());
        utilisateur.setEmail(email.trim());
        utilisateur.setMotDePasse(passwordEncoder.encode(motDePasse));
        utilisateur.setRole(roleEntity);
        utilisateur.setIdDepartement(idDepartement);
        utilisateur.setActif(true);
        utilisateurRepository.save(utilisateur);
        return profil(utilisateur);
    }

    /** Active ou desactive un compte (RM-003). */
    @Transactional
    public AuthDto.Profil changerActivation(Long id, boolean actif) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("L'utilisateur " + id + " n'existe pas."));
        utilisateur.setActif(actif);
        utilisateurRepository.save(utilisateur);
        return profil(utilisateur);
    }

    /** Liste des roles disponibles. */
    @Transactional(readOnly = true)
    public List<String> roles() {
        return roleRepository.findAll().stream().map(Role::getNom).sorted().toList();
    }

    private AuthDto.Profil profil(Utilisateur utilisateur) {
        return AuthDto.Profil.de(utilisateur, utilisateur.getIdDepartement() == null
                ? null : utilisateurRepository.trouverNomDepartement(utilisateur.getIdDepartement()));
    }

    private Map<Long, String> nomsDepartements() {
        Map<Long, String> noms = new HashMap<>();
        for (Object[] ligne : utilisateurRepository.trouverDepartements()) {
            noms.put(((Number) ligne[0]).longValue(), (String) ligne[1]);
        }
        return noms;
    }

}
