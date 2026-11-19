package com.tovo.erp.paie.service;

import com.tovo.erp.paie.dto.BulletinPaieDtos;
import com.tovo.erp.paie.model.BulletinPaie;
import com.tovo.erp.paie.model.PeriodePaie;
import com.tovo.erp.paie.model.StatutBulletinPaie;
import com.tovo.erp.paie.model.StatutPeriodePaie;
import com.tovo.erp.paie.repository.BulletinPaieRepository;
import com.tovo.erp.paie.repository.PeriodePaieRepository;
import com.tovo.erp.shared.MouvementAppliqueEvent;
import com.tovo.erp.shared.audit.JournalAuditService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class MouvementAppliquePaieListener {

    private static final Set<StatutPeriodePaie> PERIODES_AJUSTABLES = Set.of(
            StatutPeriodePaie.OUVERTE, StatutPeriodePaie.VARIABLES_CONSOLIDEES, StatutPeriodePaie.CALCULEE);

    private final BulletinPaieRepository bulletins;
    private final PeriodePaieRepository periodes;
    private final BulletinPaieService calcul;
    private final JournalAuditService audit;

    public MouvementAppliquePaieListener(BulletinPaieRepository bulletins,
                                         PeriodePaieRepository periodes,
                                         BulletinPaieService calcul,
                                         JournalAuditService audit) {
        this.bulletins = bulletins;
        this.periodes = periodes;
        this.calcul = calcul;
        this.audit = audit;
    }

    @EventListener
    @Transactional
    public void surMouvementApplique(MouvementAppliqueEvent evenement) {
        if (evenement.dateEffet() == null || evenement.salaireNouveau() == null) {
            return;
        }
        if (evenement.salaireAncien() != null
                && evenement.salaireAncien().compareTo(evenement.salaireNouveau()) == 0) {
            return;
        }
        LocalDate moisEffet = evenement.dateEffet().withDayOfMonth(1);
        Map<Long, PeriodePaie> parId = periodes.findAllByOrderByMoisDesc().stream()
                .collect(Collectors.toMap(PeriodePaie::getId, Function.identity()));
        List<BulletinPaie> aAjuster = bulletins.findAll().stream()
                .filter(b -> evenement.idEmploye().equals(b.getIdEmploye()))
                .filter(b -> parId.containsKey(b.getIdPeriode()))
                .filter(b -> !parId.get(b.getIdPeriode()).getMois().isBefore(moisEffet))
                .filter(b -> PERIODES_AJUSTABLES.contains(parId.get(b.getIdPeriode()).getStatut()))
                .filter(b -> b.getStatut() == StatutBulletinPaie.BROUILLON
                        || b.getStatut() == StatutBulletinPaie.CALCULE)
                .toList();
        for (BulletinPaie bulletin : aAjuster) {
            calcul.ajuster(bulletin.getId(), new BulletinPaieDtos.AjustementRequest(
                    null, null, null,
                    "Mobilite " + evenement.type() + " du " + evenement.dateEffet()));
        }
        audit.enregistrer("PAIE_AJUSTEE_MOBILITE", "employe", evenement.idEmploye(), null,
                aAjuster.size() + " bulletin(s)");
    }
}
