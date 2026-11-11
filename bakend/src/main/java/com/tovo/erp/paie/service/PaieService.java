package com.tovo.erp.paie.service;

import com.tovo.erp.conges.dto.CongeDtos;
import com.tovo.erp.conges.service.CongeService;
import com.tovo.erp.contrat.dto.ContratDtos;
import com.tovo.erp.contrat.service.ContratService;
import com.tovo.erp.paie.dto.PaieDtos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Service
public class PaieService {
    private static final BigDecimal JOURS_MOIS = BigDecimal.valueOf(30);

    private final CongeService conges;
    private final ContratService contrats;

    public PaieService(CongeService conges, ContratService contrats) {
        this.conges = conges;
        this.contrats = contrats;
    }

    @Transactional(readOnly = true)
    public List<PaieDtos.VariableCongeDto> variables(LocalDate debut, LocalDate fin) {
        return conges.variablesPaie().stream()
                .filter(variable -> chevauche(variable.dateDebut(), variable.dateFin(), debut, fin))
                .map(variable -> calculer(variable, contrats.courant(variable.idEmploye()), debut, fin))
                .toList();
    }

    private PaieDtos.VariableCongeDto calculer(CongeDtos.VariablePaieDto variable,
                                                ContratDtos.ContratDto contrat,
                                                LocalDate periodeDebut,
                                                LocalDate periodeFin) {
        LocalDate debut = variable.dateDebut().isAfter(periodeDebut) ? variable.dateDebut() : periodeDebut;
        LocalDate fin = variable.dateFin().isBefore(periodeFin) ? variable.dateFin() : periodeFin;
        BigDecimal jours = BigDecimal.valueOf(joursOuvres(debut, fin));
        BigDecimal salaire = contrat == null ? null : contrat.salaireBase();
        BigDecimal montant = salaire == null
                ? null
                : salaire.multiply(jours).divide(JOURS_MOIS, 2, RoundingMode.HALF_UP);
        return new PaieDtos.VariableCongeDto(
                variable.idEmploye(),
                variable.dateDebut(),
                variable.dateFin(),
                jours,
                variable.typeConge(),
                variable.motif(),
                salaire,
                montant,
                contrat == null ? null : contrat.devise(),
                contrat != null);
    }

    private boolean chevauche(LocalDate variableDebut, LocalDate variableFin,
                              LocalDate periodeDebut, LocalDate periodeFin) {
        return !variableDebut.isAfter(periodeFin) && !variableFin.isBefore(periodeDebut);
    }

    private long joursOuvres(LocalDate debut, LocalDate fin) {
        long total = 0;
        for (LocalDate date = debut; !date.isAfter(fin); date = date.plusDays(1)) {
            if (date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY) {
                total++;
            }
        }
        return total;
    }
}
