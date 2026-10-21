package com.tovo.erp.security;

import com.tovo.erp.domain.entity.Utilisateur;
import com.tovo.erp.domain.enums.RoleNom;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Emission des jetons de connexion (JWT signs en HMAC-SHA256).
 *
 * <p>Le jeton ne contient que l'identifiant de l'utilisateur, son role et son
 * departement : aucune donnee sensible. Le controle d'activite est refait a chaque
 * requete sur la base (RM-003).
 */
@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties proprietes;

    public JwtService(JwtEncoder jwtEncoder, JwtProperties proprietes) {
        this.jwtEncoder = jwtEncoder;
        this.proprietes = proprietes;
    }

    /** Cree la cle secrete a partir de la configuration. */
    public static SecretKey cleSecrete(String secret) {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    /** Genere un jeton pour l'utilisateur donne. */
    public String genererJeton(Utilisateur utilisateur) {
        Instant maintenant = Instant.now();
        Instant expiration = maintenant.plus(proprietes.dureeValidite());

        JwtClaimsSet.Builder jeton = JwtClaimsSet.builder()
                .issuer(proprietes.emetteur())
                .issuedAt(maintenant)
                .expiresAt(expiration)
                .subject(String.valueOf(utilisateur.getId()))
                .claim("email", utilisateur.getEmail())
                .claim("nom", utilisateur.getNom())
                .claim("role", utilisateur.getRole().getNom());

        if (utilisateur.getDepartement() != null) {
            jeton.claim("departement", utilisateur.getDepartement().getNom());
        }

        JwsHeader enTete = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(enTete, jeton.build())).getTokenValue();
    }

    /** Instant d'expiration du jeton, pour que le client sache quand se reconnecter. */
    public Instant expiration() {
        return Instant.now().plus(proprietes.dureeValidite());
    }
}
