package com.tovo.erp.shared.audit;

import com.tovo.erp.shared.UtilisateurConnecte;
import com.tovo.erp.shared.UtilisateurCourant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/**
 * Ecrit une trace dans le journal d'audit sans jamais faire echouer l'operation
 * metier : l'audit est isole dans sa propre transaction.
 *
 * <p>Chaque entree est chainee cryptographiquement (cahier 2.3, RG-T-02) :
 * {@code hash = SHA-256(hash_precedent + contenu)}. Toute alteration d'une ligne
 * casse la chaine et est detectee par {@link #verifierChaine()}.
 */
@Service
public class JournalAuditService {

    private final JournalAuditRepository journal;
    private final UtilisateurCourant utilisateurCourant;

    public JournalAuditService(JournalAuditRepository journal,
                               UtilisateurCourant utilisateurCourant) {
        this.journal = journal;
        this.utilisateurCourant = utilisateurCourant;
    }

    /** Signature historique conservee pour les appels existants. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enregistrer(String action, String ressource, Long idRessource,
                            String avant, String apres) {
        enregistrer(action, ressource, idRessource, avant, apres, null, null);
    }

    /**
     * Enregistre une trace avec motif et identifiant de correlation, puis calcule
     * son empreinte dans la chaine d'audit.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public synchronized void enregistrer(String action, String ressource, Long idRessource,
                                         String avant, String apres, String motif,
                                         String correlationId) {
        UtilisateurConnecte utilisateur = utilisateurCourant.optionnel();
        JournalAudit entree = new JournalAudit();
        entree.setIdUtilisateur(utilisateur == null ? null : utilisateur.getId());
        entree.setRoleUtilisateur(utilisateur == null ? null : utilisateur.getRoleNom());
        entree.setAction(action);
        entree.setRessource(ressource);
        entree.setIdRessource(idRessource);
        entree.setAvant(avant);
        entree.setApres(apres);
        entree.setMotif(motif);
        entree.setCorrelationId(correlationId);
        entree.setAdresseIp(adresseIp());
        entree.setUserAgent(userAgent());

        String hashPrecedent = journal.findTopByOrderByIdDesc()
                .map(JournalAudit::getHash)
                .orElse(null);
        entree.setHashPrecedent(hashPrecedent);
        entree.setHash(calculerHash(hashPrecedent, entree));
        journal.save(entree);
    }

    /**
     * Verifie l'integrite de toute la chaine. Les lignes anterieures a la mise en
     * place du chainage (hash null) sont ignorees.
     *
     * @return le nombre d'anomalies detectees
     */
    @Transactional(readOnly = true)
    public int verifierChaine() {
        List<JournalAudit> entrees = journal.findAllByOrderByIdAsc();
        String precedent = null;
        int anomalies = 0;
        for (JournalAudit entree : entrees) {
            if (entree.getHash() == null) {
                precedent = null;
                continue;
            }
            String attendu = calculerHash(precedent, entree);
            boolean hashPrecedentValide = java.util.Objects.equals(precedent, entree.getHashPrecedent());
            if (!attendu.equals(entree.getHash()) || !hashPrecedentValide) {
                anomalies++;
            }
            precedent = entree.getHash();
        }
        return anomalies;
    }

    /** Recherche les traces d'une ressource, les plus recentes d'abord. */
    @Transactional(readOnly = true)
    public List<JournalAudit> historique(String ressource, Long idRessource) {
        return journal.findByRessourceAndIdRessourceOrderByDateActionDesc(ressource, idRessource);
    }

    private String calculerHash(String hashPrecedent, JournalAudit entree) {
        String contenu = String.join("|",
                nullToVide(entree.getIdUtilisateur()),
                nullToVide(entree.getRoleUtilisateur()),
                nullToVide(entree.getAction()),
                nullToVide(entree.getRessource()),
                nullToVide(entree.getIdRessource()),
                nullToVide(entree.getAvant()),
                nullToVide(entree.getApres()),
                nullToVide(entree.getAdresseIp()),
                nullToVide(entree.getUserAgent()),
                nullToVide(entree.getMotif()),
                nullToVide(entree.getCorrelationId()));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] octets = digest.digest((nullToVide(hashPrecedent) + contenu)
                    .getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(octets);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }

    private String nullToVide(Object valeur) {
        return valeur == null ? "" : valeur.toString();
    }

    private String adresseIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributs) {
            return attributs.getRequest().getRemoteAddr();
        }
        return null;
    }

    private String userAgent() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributs) {
            return tronquer(attributs.getRequest().getHeader("User-Agent"), 512);
        }
        return null;
    }

    private String tronquer(String valeur, int longueurMax) {
        if (valeur == null || valeur.length() <= longueurMax) {
            return valeur;
        }
        return valeur.substring(0, longueurMax);
    }
}
