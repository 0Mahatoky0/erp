package com.tovo.erp.security;

import com.tovo.erp.domain.entity.Utilisateur;
import com.tovo.erp.repository.UtilisateurRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtre d'authentification par jeton (RM-001, RM-003).
 *
 * <p>Le jeton est verifie (signature et expiration), puis l'utilisateur est recharge
 * depuis la base : un compte desactive ne peut plus agir meme si son jeton est
 * encore valide (RM-003).
 *
 * <p>Le filtre est instancie par SecurityConfig pour eviter une dependance circulaire.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIXE = "Bearer ";

    private final JwtDecoder jwtDecoder;
    private final UtilisateurRepository utilisateurRepository;

    public JwtAuthenticationFilter(JwtDecoder jwtDecoder, UtilisateurRepository utilisateurRepository) {
        this.jwtDecoder = jwtDecoder;
        this.utilisateurRepository = utilisateurRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String entete = request.getHeader("Authorization");
        if (entete != null && entete.startsWith(PREFIXE)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            authentifier(request, entete.substring(PREFIXE.length()));
        }
        filterChain.doFilter(request, response);
    }

    private void authentifier(HttpServletRequest request, String jeton) {
        Jwt jwt;
        try {
            jwt = jwtDecoder.decode(jeton);
        } catch (Exception ex) {
            // Jeton invalide ou expire : la requete continuera sans authentification.
            return;
        }

        Long idUtilisateur;
        try {
            idUtilisateur = Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException ex) {
            return;
        }

        Utilisateur utilisateur = utilisateurRepository.findById(idUtilisateur).orElse(null);
        if (utilisateur == null || !utilisateur.isActif()) {
            return;
        }

        String role = jwt.getClaimAsString("role");
        if (role == null) {
            role = utilisateur.getRole().getNom();
        }

        UsernamePasswordAuthenticationToken authentification =
                new UsernamePasswordAuthenticationToken(utilisateur, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        authentification.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentification);
    }
}
