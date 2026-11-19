package com.tovo.erp.discipline.service;

import com.tovo.erp.discipline.dto.DisciplineDtos;
import com.tovo.erp.discipline.repository.IncidentRepository;
import com.tovo.erp.discipline.repository.ProcedureRepository;
import com.tovo.erp.discipline.repository.SanctionRepository;
import com.tovo.erp.employe.service.EmployeService;
import com.tovo.erp.shared.Roles;
import com.tovo.erp.shared.UtilisateurCourant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class IndicateurDisciplineService {

    private final IncidentRepository incidents;
    private final ProcedureRepository procedures;
    private final SanctionRepository sanctions;
    private final EmployeService employes;
    private final UtilisateurCourant utilisateurCourant;

    public IndicateurDisciplineService(IncidentRepository incidents, ProcedureRepository procedures,
                                       SanctionRepository sanctions, EmployeService employes,
                                       UtilisateurCourant utilisateurCourant) {
        this.incidents = incidents;
        this.procedures = procedures;
        this.sanctions = sanctions;
        this.employes = employes;
        this.utilisateurCourant = utilisateurCourant;
    }

    @Transactional(readOnly = true)
    public DisciplineDtos.IndicateursDto indicateurs(LocalDate debut, LocalDate fin) {
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION);
        var incidentsPeriode = incidents.findAllByOrderByDateDeclarationDesc().stream()
                .filter(i -> !i.getDateConnaissance().isBefore(debut))
                .filter(i -> !i.getDateConnaissance().isAfter(fin))
                .toList();
        var proceduresPeriode = procedures.findAllByOrderByDateOuvertureDesc().stream()
                .filter(p -> !p.getDateOuverture().toLocalDate().isBefore(debut))
                .filter(p -> !p.getDateOuverture().toLocalDate().isAfter(fin))
                .toList();
        var sanctionsPeriode = sanctions.findAllByOrderByDateNotificationDesc().stream()
                .filter(s -> !s.getDateNotification().toLocalDate().isBefore(debut))
                .filter(s -> !s.getDateNotification().toLocalDate().isAfter(fin))
                .toList();

        int effectif = employes.actifs().size();
        BigDecimal taux = effectif == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(incidentsPeriode.size())
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(effectif), 2, RoundingMode.HALF_UP);
        double moyenne = proceduresPeriode.stream()
                .filter(p -> p.getDateCloture() != null)
                .mapToLong(p -> ChronoUnit.DAYS.between(p.getDateOuverture(), p.getDateCloture()))
                .average().orElse(0.0);
        Map<String, Long> parCategorie = incidentsPeriode.stream().collect(Collectors.groupingBy(
                i -> i.getCategorie().name(), Collectors.counting()));
        Map<String, Long> parSanction = sanctionsPeriode.stream().collect(Collectors.groupingBy(
                s -> s.getType().name(), Collectors.counting()));
        return new DisciplineDtos.IndicateursDto(taux,
                BigDecimal.valueOf(moyenne).setScale(2, RoundingMode.HALF_UP),
                parCategorie, parSanction);
    }
}
