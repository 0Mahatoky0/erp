package com.tovo.erp.employe.service;

import com.tovo.erp.employe.dto.MissionDtos;
import com.tovo.erp.employe.model.PrioriteMission;
import com.tovo.erp.shared.MouvementAppliqueEvent;
import com.tovo.erp.shared.audit.JournalAuditService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class MouvementAppliqueEmployeListener {

    private final MissionService missions;
    private final JournalAuditService audit;

    public MouvementAppliqueEmployeListener(MissionService missions, JournalAuditService audit) {
        this.missions = missions;
        this.audit = audit;
    }

    @EventListener
    public void surMouvementApplique(MouvementAppliqueEvent evenement) {
        LocalDate debut = evenement.dateEffet() == null ? LocalDate.now() : evenement.dateEffet();
        String titre = "Prise en charge - " + evenement.type();
        missions.creer(evenement.idEmploye(), new MissionDtos.CreationMissionRequest(
                titre,
                "Mission de prise en charge generee par le mouvement " + evenement.idMouvement()
                        + ". Verifier le nouveau poste, le manager et le lieu d'affectation.",
                evenement.lieuDestination(),
                debut,
                debut.plusMonths(1),
                PrioriteMission.NORMALE,
                evenement.idManagerDestination()));
        audit.enregistrer("MISSION_TRANSFERT_MOBILITE", "employe", evenement.idEmploye(), null,
                String.valueOf(evenement.idMouvement()));
    }
}
