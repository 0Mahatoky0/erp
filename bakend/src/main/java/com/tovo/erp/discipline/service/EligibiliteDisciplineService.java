package com.tovo.erp.discipline.service;

import com.tovo.erp.discipline.model.StatutProcedure;
import com.tovo.erp.discipline.model.StatutSanction;
import com.tovo.erp.discipline.model.TypeSanction;
import com.tovo.erp.discipline.repository.ProcedureRepository;
import com.tovo.erp.discipline.repository.SanctionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Service en lecture seule utilise par les autres modules pour controler
 * l'eligibilite d'un salarie sans exposer les donnees disciplinaires.
 */
@Service
public class EligibiliteDisciplineService {

    private static final List<StatutSanction> SANCTIONS_ACTIVES = List.of(
            StatutSanction.NOTIFIEE, StatutSanction.APPLIQUEE, StatutSanction.CONTESTEE);

    private final ProcedureRepository procedures;
    private final SanctionRepository sanctions;

    public EligibiliteDisciplineService(ProcedureRepository procedures,
                                        SanctionRepository sanctions) {
        this.procedures = procedures;
        this.sanctions = sanctions;
    }

    @Transactional(readOnly = true)
    public boolean sanctionActive(Long idEmploye) {
        if (idEmploye == null) {
            return false;
        }
        LocalDate aujourdHui = LocalDate.now();
        boolean sanctionEnCours = sanctions
                .findByIdEmployeAndStatutInAndDateEffacementAfter(idEmploye, SANCTIONS_ACTIVES, aujourdHui)
                .stream()
                .anyMatch(s -> s.getDateEffet() == null || !s.getDateEffet().isAfter(aujourdHui));
        if (sanctionEnCours) {
            return true;
        }
        return procedures.findByIdEmployeOrderByDateOuvertureDesc(idEmploye).stream()
                .anyMatch(p -> p.getStatut() == StatutProcedure.DECIDEE
                        && TypeSanction.LICENCIEMENT_DISCIPLINAIRE.name().equals(p.getDecision()));
    }
}
