package com.tovo.erp.shared;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
 * <p>Le filtre ne connait que l'interface Spring {@link UserDetailsService} :
 * l'implementation metier reste dans le module iam.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIXE = "Bearer ";

    private final JwtDecoder jwtDecoder;
    private final UserDetailsService utilisateurDetailsService;

    public JwtAuthenticationFilter(JwtDecoder jwtDecoder, UserDetailsService utilisateurDetailsService) {
        this.jwtDecoder = jwtDecoder;
        this.utilisateurDetailsService = utilisateurDetailsService;
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

        UserDetails details;
        try {
            details = utilisateurDetailsService.loadUserByUsername(String.valueOf(idUtilisateur));
        } catch (UsernameNotFoundException ex) {
            return;
        }
        if (!details.isEnabled()) {
            return;
        }

        String role = jwt.getClaimAsString("role");
        if (role == null && !details.getAuthorities().isEmpty()) {
            role = details.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        }
        if (role == null) {
            return;
        }

        UtilisateurConnecte principal = new UtilisateurConnecte(
                idUtilisateur,
                jwt.getClaimAsString("nom"),
                jwt.getClaimAsString("email"),
                role,
                jwt.getClaim("departementId") == null ? null : ((Number) jwt.getClaim("departementId")).longValue(),
                jwt.getClaimAsString("departement"));

        UsernamePasswordAuthenticationToken authentification =
                new UsernamePasswordAuthenticationToken(principal, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        authentification.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentification);
    }
}
