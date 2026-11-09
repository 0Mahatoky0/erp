package com.tovo.erp.shared;

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

/**
 * Emission des jetons de connexion (JWT signes en HMAC-SHA256).
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

    /** Genere un jeton pour les informations d'identite fournies. */
    public String genererJeton(Long id, String email, String nom, String role,
                               Long idDepartement, String departementNom) {
        Instant maintenant = Instant.now();
        Instant expiration = maintenant.plus(proprietes.dureeValidite());

        JwtClaimsSet.Builder jeton = JwtClaimsSet.builder()
                .issuer(proprietes.emetteur())
                .issuedAt(maintenant)
                .expiresAt(expiration)
                .subject(String.valueOf(id))
                .claim("email", email)
                .claim("nom", nom)
                .claim("role", role);

        if (idDepartement != null && departementNom != null) {
            jeton.claim("departementId", idDepartement);
            jeton.claim("departement", departementNom);
        }

        JwsHeader enTete = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(enTete, jeton.build())).getTokenValue();
    }

    /** Instant d'expiration du jeton, pour que le client sache quand se reconnecter. */
    public Instant expiration() {
        return Instant.now().plus(proprietes.dureeValidite());
    }
}
