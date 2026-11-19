package com.tovo.erp.temps.service;

import com.tovo.erp.shared.MouvementAppliqueEvent;
import com.tovo.erp.shared.audit.JournalAuditService;
import com.tovo.erp.temps.model.AffectationRegime;
import com.tovo.erp.temps.model.PlanningTravail;
import com.tovo.erp.temps.repository.AffectationRegimeRepository;
import com.tovo.erp.temps.repository.PlanningTravailRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class MouvementAppliqueTempsListener {

    private final AffectationRegimeRepository affectations;
    private final PlanningTravailRepository plannings;
    private final JournalAuditService audit;

    public MouvementAppliqueTempsListener(AffectationRegimeRepository affectations,
                                          PlanningTravailRepository plannings,
                                          JournalAuditService audit) {
        this.affectations = affectations;
        this.plannings = plannings;
        this.audit = audit;
    }

    @EventListener
    @Transactional
    public void surMouvementApplique(MouvementAppliqueEvent evenement) {
        if (evenement.dateEffet() == null) {
            return;
        }
        LocalDate effet = evenement.dateEffet();
        LocalDate veille = effet.minusDays(1);
        List<AffectationRegime> enCours = affectations.courantes(evenement.idEmploye(), effet);
        for (AffectationRegime affectation : enCours) {
            if (affectation.getDateFin() != null && !affectation.getDateFin().isAfter(veille)) {
                continue;
            }
            affectation.setDateFin(veille);
            affectations.save(affectation);
            AffectationRegime suivante = new AffectationRegime();
            suivante.setIdEmploye(evenement.idEmploye());
            suivante.setIdRegime(affectation.getIdRegime());
            suivante.setDateDebut(effet);
            suivante.setDateCreation(LocalDateTime.now());
            affectations.save(suivante);
        }
        PlanningTravail source = plannings
                .findFirstByIdEmployeAndDateDebutLessThanEqualAndDateFinIsNullOrderByDateDebutDesc(
                        evenement.idEmploye(), veille)
                .orElse(null);
        for (PlanningTravail planning : plannings.findByIdEmployeOrderByDateDebutDesc(evenement.idEmploye())) {
            if (planning.getDateFin() != null && !planning.getDateFin().isAfter(veille)) {
                continue;
            }
            if (!planning.getDateDebut().isBefore(effet)) {
                plannings.delete(planning);
                continue;
            }
            planning.setDateFin(veille);
            planning.setMotifModification("Mobilite " + evenement.type());
            plannings.save(planning);
        }
        if (source != null) {
            PlanningTravail suivant = new PlanningTravail();
            suivant.setIdEmploye(evenement.idEmploye());
            suivant.setJoursTravailles(source.getJoursTravailles());
            suivant.setHeureDebut(source.getHeureDebut());
            suivant.setHeureFin(source.getHeureFin());
            suivant.setDateDebut(effet);
            suivant.setPauseMinutes(source.getPauseMinutes());
            suivant.setToleranceMinutes(source.getToleranceMinutes());
            suivant.setOrigine("MOBILITE");
            suivant.setStatut("PLANIFIE");
            suivant.setMotifModification("Mobilite " + evenement.type());
            suivant.setDateCreation(LocalDateTime.now());
            plannings.save(suivant);
        }
        audit.enregistrer("REGIME_PLANNING_MOBILITE", "employe", evenement.idEmploye(), null,
                evenement.type() + " au " + effet);
    }
}
