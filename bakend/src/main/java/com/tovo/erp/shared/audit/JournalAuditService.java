package com.tovo.erp.shared.audit;

import com.tovo.erp.shared.UtilisateurConnecte;
import com.tovo.erp.shared.UtilisateurCourant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Ecrit une trace dans le journal d'audit sans jamais faire echouer l'operation
 * metier : l'audit est isole dans sa propre transaction.
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enregistrer(String action, String ressource, Long idRessource,
                            String avant, String apres) {
        UtilisateurConnecte utilisateur = utilisateurCourant.optionnel();
        JournalAudit entree = new JournalAudit();
        entree.setIdUtilisateur(utilisateur == null ? null : utilisateur.getId());
        entree.setRoleUtilisateur(utilisateur == null ? null : utilisateur.getRoleNom());
        entree.setAction(action);
        entree.setRessource(ressource);
        entree.setIdRessource(idRessource);
        entree.setAvant(avant);
        entree.setApres(apres);
        entree.setAdresseIp(adresseIp());
        journal.save(entree);
    }

    private String adresseIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributs) {
            return attributs.getRequest().getRemoteAddr();
        }
        return null;
    }
}
