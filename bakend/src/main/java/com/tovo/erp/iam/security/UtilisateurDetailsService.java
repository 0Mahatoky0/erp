package com.tovo.erp.iam.security;

import com.tovo.erp.iam.model.Utilisateur;
import com.tovo.erp.iam.repository.UtilisateurRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Charge l'identite d'un utilisateur pour le filtre JWT. */
@Service
public class UtilisateurDetailsService implements UserDetailsService {

    private final UtilisateurRepository utilisateurs;

    public UtilisateurDetailsService(UtilisateurRepository utilisateurs) {
        this.utilisateurs = utilisateurs;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String id) throws UsernameNotFoundException {
        Utilisateur utilisateur = utilisateurs.findById(Long.valueOf(id))
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur " + id + " introuvable."));
        return new UtilisateurPrincipal(
                utilisateur.getId(),
                utilisateur.getEmail(),
                utilisateur.getRole().getNom(),
                utilisateur.isActif());
    }
}
