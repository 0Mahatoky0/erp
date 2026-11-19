package com.tovo.erp.conges.service;

import com.tovo.erp.conges.model.DemandeConge;
import com.tovo.erp.conges.model.StatutDemandeConge;
import com.tovo.erp.conges.repository.DemandeCongeRepository;
import com.tovo.erp.shared.MouvementAppliqueEvent;
import com.tovo.erp.shared.audit.JournalAuditService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MouvementAppliqueCongeListener {

    private final DemandeCongeRepository demandes;
    private final JournalAuditService audit;

    public MouvementAppliqueCongeListener(DemandeCongeRepository demandes, JournalAuditService audit) {
        this.demandes = demandes;
        this.audit = audit;
    }

    @EventListener
    @Transactional
    public void surMouvementApplique(MouvementAppliqueEvent evenement) {
        if (evenement.idManagerDestination() == null || evenement.dateEffet() == null) {
            return;
        }
        int reaffectees = 0;
        for (DemandeConge demande : demandes.findByIdEmployeOrderByDateCreationDesc(evenement.idEmploye())) {
            StatutDemandeConge statut = demande.getStatut();
            if (statut != StatutDemandeConge.SOUMISE && statut != StatutDemandeConge.VALIDEE_MANAGER) {
                continue;
            }
            if (demande.getDateFin() == null || demande.getDateFin().isBefore(evenement.dateEffet())) {
                continue;
            }
            demande.setIdValidateurManager(evenement.idManagerDestination());
            demandes.save(demande);
            reaffectees++;
        }
        audit.enregistrer("CONGES_REAFFECTES_MOBILITE", "employe", evenement.idEmploye(), null,
                reaffectees + " demande(s) -> " + evenement.idManagerDestination());
    }
}
