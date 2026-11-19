package com.tovo.erp.paie.service;

import com.tovo.erp.contrat.dto.CarriereDtos;
import com.tovo.erp.contrat.service.CarriereService;
import com.tovo.erp.paie.model.BulletinPaie;
import com.tovo.erp.paie.model.PeriodePaie;
import com.tovo.erp.paie.repository.BulletinPaieRepository;
import com.tovo.erp.paie.repository.PeriodePaieRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BulletinCoherenceService {

    private static final Set<com.tovo.erp.paie.model.StatutPeriodePaie> PERIODES_PASSEES = Set.of(
            com.tovo.erp.paie.model.StatutPeriodePaie.VALIDEE,
            com.tovo.erp.paie.model.StatutPeriodePaie.PUBLIEE,
            com.tovo.erp.paie.model.StatutPeriodePaie.PAYEE,
            com.tovo.erp.paie.model.StatutPeriodePaie.CLOTUREE);

    public record EcartSalaire(Long idBulletin, Long idEmploye, LocalDate mois,
                               BigDecimal salaireBulletin, BigDecimal salaireHistorique) {
    }

    private final BulletinPaieRepository bulletins;
    private final PeriodePaieRepository periodes;
    private final CarriereService carriere;

    public BulletinCoherenceService(BulletinPaieRepository bulletins,
                                    PeriodePaieRepository periodes,
                                    CarriereService carriere) {
        this.bulletins = bulletins;
        this.periodes = periodes;
        this.carriere = carriere;
    }

    @Transactional(readOnly = true)
    public List<EcartSalaire> ecarts() {
        Map<Long, PeriodePaie> parId = periodes.findAllByOrderByMoisDesc().stream()
                .collect(Collectors.toMap(PeriodePaie::getId, Function.identity()));
        Map<Long, List<CarriereDtos.SalaireDto>> historiques = new java.util.HashMap<>();
        List<EcartSalaire> resultats = new java.util.ArrayList<>();
        for (BulletinPaie bulletin : bulletins.findAll()) {
            PeriodePaie periode = parId.get(bulletin.getIdPeriode());
            if (periode == null || !PERIODES_PASSEES.contains(periode.getStatut())) {
                continue;
            }
            LocalDate mois = periode.getMois();
            LocalDate debut = mois.withDayOfMonth(1);
            LocalDate fin = mois.with(TemporalAdjusters.lastDayOfMonth());
            var salaires = historiques.computeIfAbsent(bulletin.getIdEmploye(), carriere::salaires);
            CarriereDtos.SalaireDto enVigueur = salaires.stream()
                    .filter(s -> !s.dateEffet().isAfter(fin))
                    .filter(s -> s.dateFin() == null || !s.dateFin().isBefore(debut))
                    .filter(s -> s.enregistreLe() == null || !s.enregistreLe().toLocalDate().isAfter(fin))
                    .filter(s -> s.remplaceLe() == null || s.remplaceLe().toLocalDate().isAfter(fin))
                    .findFirst()
                    .orElse(null);
            if (enVigueur == null) {
                continue;
            }
            BigDecimal salaireBulletin = bulletin.getSalaireBasePeriode();
            if (salaireBulletin == null || salaireBulletin.signum() == 0) {
                salaireBulletin = bulletin.getSalaireBrut();
            }
            if (salaireBulletin == null
                    || salaireBulletin.subtract(enVigueur.montantBrut()).abs().compareTo(new BigDecimal("0.01")) > 0) {
                resultats.add(new EcartSalaire(bulletin.getId(), bulletin.getIdEmploye(), mois,
                        salaireBulletin, enVigueur.montantBrut()));
            }
        }
        return resultats;
    }
}
