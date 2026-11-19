package com.tovo.erp.iam.service;

import com.tovo.erp.employe.service.EmployeService;
import com.tovo.erp.employe.service.ManagerAffectationService;
import com.tovo.erp.shared.MouvementAppliqueEvent;
import com.tovo.erp.shared.audit.JournalAuditService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class MouvementAppliqueIamListener {

    private final RoleMobiliteService roles;
    private final EmployeService employes;
    private final ManagerAffectationService affectations;
    private final JournalAuditService audit;

    public MouvementAppliqueIamListener(RoleMobiliteService roles, EmployeService employes,
                                        ManagerAffectationService affectations, JournalAuditService audit) {
        this.roles = roles;
        this.employes = employes;
        this.affectations = affectations;
        this.audit = audit;
    }

    @EventListener
    public void surMouvementApplique(MouvementAppliqueEvent evenement) {
        Long nouveauManager = evenement.idManagerDestination();
        if (nouveauManager != null) {
            employes.idUtilisateurDe(nouveauManager).ifPresent(id -> {
                roles.definirManager(id);
                audit.enregistrer("ROLE_MANAGER_OCTROYE", "utilisateur", id, null,
                        "Mobilite " + evenement.idMouvement());
            });
        }
        Long ancienManager = evenement.idManagerOrigine();
        if (ancienManager != null && !ancienManager.equals(nouveauManager)
                && !affectations.estEncoreManager(ancienManager)) {
            employes.idUtilisateurDe(ancienManager).ifPresent(id -> {
                roles.retirerManager(id);
                audit.enregistrer("ROLE_MANAGER_RETIRER", "utilisateur", id, null,
                        "Mobilite " + evenement.idMouvement());
            });
        }
    }
}
