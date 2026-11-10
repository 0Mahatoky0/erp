package com.tovo.erp.temps.service;

import com.tovo.erp.shared.BusinessException;
import com.tovo.erp.temps.dto.TempsDtos;
import com.tovo.erp.temps.model.PlanningTravail;
import com.tovo.erp.temps.model.Pointage;
import com.tovo.erp.temps.repository.PlanningTravailRepository;
import com.tovo.erp.temps.repository.PointageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

@Service
public class TempsService {

    private final PlanningTravailRepository planningRepository;
    private final PointageRepository pointageRepository;

    public TempsService(PlanningTravailRepository planningRepository, PointageRepository pointageRepository) {
        this.planningRepository = planningRepository;
        this.pointageRepository = pointageRepository;
    }

    @Transactional(readOnly = true)
    public List<TempsDtos.PlanningDto> plannings(Long idEmploye) {
        return planningRepository.findByIdEmployeOrderByDateDebutDesc(idEmploye).stream()
                .map(TempsDtos.PlanningDto::de)
                .toList();
    }

    @Transactional
    public TempsDtos.PlanningDto creerPlanning(TempsDtos.PlanningRequest demande) {
        if (!demande.heureFin().isAfter(demande.heureDebut())) {
            throw new BusinessException("HORAIRES_INVALIDES", "L'heure de fin doit etre posterieure a l'heure de debut.");
        }
        if (demande.dateFin() != null && demande.dateFin().isBefore(demande.dateDebut())) {
            throw new BusinessException("DATES_INVALIDES", "La date de fin doit etre posterieure ou egale a la date de debut.");
        }
        PlanningTravail planning = new PlanningTravail();
        planning.setIdEmploye(demande.idEmploye());
        planning.setJoursTravailles(demande.joursTravailles().trim());
        planning.setHeureDebut(demande.heureDebut());
        planning.setHeureFin(demande.heureFin());
        planning.setDateDebut(demande.dateDebut());
        planning.setDateFin(demande.dateFin());
        return TempsDtos.PlanningDto.de(planningRepository.save(planning));
    }

    @Transactional
    public TempsDtos.PointageDto pointerEntree(TempsDtos.PointageRequest demande) {
        LocalDate date = demande.date() == null ? LocalDate.now() : demande.date();
        LocalTime heure = demande.heure() == null ? LocalTime.now().withNano(0) : demande.heure();
        Pointage pointage = pointageRepository.findByIdEmployeAndDatePointage(demande.idEmploye(), date)
                .orElseGet(Pointage::new);
        if (pointage.getHeureEntree() != null) {
            throw new BusinessException("ENTREE_DEJA_ENREGISTREE", "L'entree de cet employe est deja enregistree pour cette journee.");
        }
        pointage.setIdEmploye(demande.idEmploye());
        pointage.setDatePointage(date);
        pointage.setHeureEntree(heure);
        return dto(pointageRepository.save(pointage));
    }

    @Transactional
    public TempsDtos.PointageDto pointerSortie(TempsDtos.PointageRequest demande) {
        LocalDate date = demande.date() == null ? LocalDate.now() : demande.date();
        LocalTime heure = demande.heure() == null ? LocalTime.now().withNano(0) : demande.heure();
        Pointage pointage = pointageRepository.findByIdEmployeAndDatePointage(demande.idEmploye(), date)
                .orElseThrow(() -> new BusinessException("ENTREE_MANQUANTE", "Enregistrez l'entree avant la sortie."));
        if (pointage.getHeureSortie() != null) {
            throw new BusinessException("SORTIE_DEJA_ENREGISTREE", "La sortie de cet employe est deja enregistree pour cette journee.");
        }
        if (heure.isBefore(pointage.getHeureEntree())) {
            throw new BusinessException("HEURES_INVALIDES", "La sortie ne peut pas etre avant l'entree.");
        }
        pointage.setHeureSortie(heure);
        return dto(pointageRepository.save(pointage));
    }

    @Transactional(readOnly = true)
    public TempsDtos.PointageMoisDto pointagesDuMois(Long idEmploye, YearMonth mois) {
        LocalDate debut = mois.atDay(1);
        LocalDate fin = mois.atEndOfMonth();
        List<TempsDtos.PointageDto> pointages = pointageRepository
                .findByIdEmployeAndDatePointageBetweenOrderByDatePointageDesc(idEmploye, debut, fin).stream()
                .map(this::dto)
                .toList();
        return new TempsDtos.PointageMoisDto(idEmploye, mois.toString(), pointages.size(),
                pointages.stream().filter(p -> p.heureSortie() != null).count(),
                pointages.stream().mapToLong(TempsDtos.PointageDto::minutesTravaillees).sum(),
                pointages.stream().mapToLong(TempsDtos.PointageDto::minutesRetard).sum(),
                pointages.stream().mapToLong(TempsDtos.PointageDto::minutesSupplementaires).sum(), pointages);
    }

    private TempsDtos.PointageDto dto(Pointage pointage) {
        PlanningTravail planning = planningPour(pointage.getIdEmploye(), pointage.getDatePointage());
        long travaillees = pointage.getHeureEntree() == null || pointage.getHeureSortie() == null
                ? 0 : Duration.between(pointage.getHeureEntree(), pointage.getHeureSortie()).toMinutes();
        long prevues = planning == null ? 0 : Duration.between(planning.getHeureDebut(), planning.getHeureFin()).toMinutes();
        long retard = planning == null || pointage.getHeureEntree() == null
                ? 0 : Math.max(0, Duration.between(planning.getHeureDebut(), pointage.getHeureEntree()).toMinutes());
        long supplementaires = planning == null ? 0 : Math.max(0, travaillees - prevues);
        return new TempsDtos.PointageDto(pointage.getId(), pointage.getIdEmploye(), pointage.getDatePointage(),
                pointage.getHeureEntree(), pointage.getHeureSortie(), travaillees, retard, supplementaires,
                pointage.getSource());
    }

    private PlanningTravail planningPour(Long idEmploye, LocalDate date) {
        return planningRepository
                .findFirstByIdEmployeAndDateDebutLessThanEqualAndDateFinIsNullOrderByDateDebutDesc(idEmploye, date)
                .or(() -> planningRepository.findFirstByIdEmployeAndDateDebutLessThanEqualAndDateFinGreaterThanEqualOrderByDateDebutDesc(idEmploye, date, date))
                .orElse(null);
    }
}
