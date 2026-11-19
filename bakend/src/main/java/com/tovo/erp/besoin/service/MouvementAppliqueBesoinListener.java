package com.tovo.erp.besoin.service;

import com.tovo.erp.besoin.dto.DemandeDto;
import com.tovo.erp.besoin.dto.PosteDto;
import com.tovo.erp.besoin.model.TypeBesoin;
import com.tovo.erp.besoin.model.TypeRemplacement;
import com.tovo.erp.employe.service.EmployeService;
import com.tovo.erp.shared.MouvementAppliqueEvent;
import com.tovo.erp.shared.Roles;
import com.tovo.erp.shared.UtilisateurConnecte;
import com.tovo.erp.shared.audit.JournalAuditService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class MouvementAppliqueBesoinListener {

    private final PosteService postes;
    private final DemandeService demandes;
    private final EmployeService employes;
    private final JournalAuditService audit;

    public MouvementAppliqueBesoinListener(PosteService postes, DemandeService demandes,
                                           EmployeService employes, JournalAuditService audit) {
        this.postes = postes;
        this.demandes = demandes;
        this.employes = employes;
        this.audit = audit;
    }

    @EventListener
    public void surMouvementApplique(MouvementAppliqueEvent evenement) {
        Long idPoste = evenement.idPosteOrigine();
        if (idPoste == null || idPoste.equals(evenement.idPosteDestination())) {
            return;
        }
        PosteDto.Liste poste = postes.trouver(idPoste);
        int autorise = poste.effectifAutorise() == null ? 1 : Math.max(1, poste.effectifAutorise());
        if (employes.actifsDuPoste(idPoste).size() >= autorise) {
            return;
        }
        audit.enregistrer("POSTE_DEVENU_VACANT", "poste", idPoste, null,
                "Mobilite " + evenement.type() + " " + evenement.idMouvement());
        UtilisateurConnecte auteur = new UtilisateurConnecte(evenement.idUtilisateurApplication(),
                "SYSTEM_MOBILITE", null, Roles.RH, null, "RH");
        LocalDate dateSouhaitee = LocalDate.now().plusMonths(1);
        if (evenement.dateEffet() != null && evenement.dateEffet().isAfter(dateSouhaitee)) {
            dateSouhaitee = evenement.dateEffet();
        }
        DemandeDto.Creation creation = new DemandeDto.Creation(
                "Remplacement suite a " + evenement.type().toLowerCase() + " (" + evenement.idMouvement() + ")",
                dateSouhaitee,
                List.of(new DemandeDto.LigneCreation(
                        evenement.idServiceOrigine() == null ? poste.idService() : evenement.idServiceOrigine(),
                        idPoste, TypeBesoin.REMPLACEMENT, TypeRemplacement.DEFINITIF,
                        evenement.idEmploye(), evenement.type(), evenement.dateEffet(), null,
                        "Remplacement de poste " + poste.intitule(), null, null, 1)));
        DemandeDto.Resume demande = demandes.creer(auteur, creation);
        demandes.soumettre(demande.id(), auteur);
        audit.enregistrer("DEMANDE_REMPLACEMENT_MOBILITE", "demande_recrutement", demande.id(), null,
                String.valueOf(idPoste));
    }
}
