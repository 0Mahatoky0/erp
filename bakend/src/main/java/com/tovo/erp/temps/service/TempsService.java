package com.tovo.erp.temps.service;

import com.tovo.erp.shared.BusinessException;
import com.tovo.erp.temps.dto.TempsDtos;
import com.tovo.erp.temps.model.AnomaliePointage;
import com.tovo.erp.temps.model.HeureSupplementaire;
import com.tovo.erp.temps.model.JourTravaille;
import com.tovo.erp.temps.model.PlanningTravail;
import com.tovo.erp.temps.model.PeriodeTempsClose;
import com.tovo.erp.temps.model.Pointage;
import com.tovo.erp.temps.repository.AnomaliePointageRepository;
import com.tovo.erp.temps.repository.HeureSupplementaireRepository;
import com.tovo.erp.temps.repository.JourTravailleRepository;
import com.tovo.erp.temps.repository.PlanningTravailRepository;
import com.tovo.erp.temps.repository.PeriodeTempsCloseRepository;
import com.tovo.erp.temps.repository.PointageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

@Service
public class TempsService {

    private static final int HEURES_HEBDO = 40;
    private static final int PLAFOND_HS_HEBDO_MINUTES = 20 * 60;
    private static final int SEUIL_PAUSE_MINUTES = 6 * 60;
    private static final int DUREE_EXCESSIVE_MINUTES = 12 * 60;

    private final PlanningTravailRepository planningRepository;
    private final PointageRepository pointageRepository;
    private final JourTravailleRepository joursRepository;
    private final AnomaliePointageRepository anomaliesRepository;
    private final HeureSupplementaireRepository heuresSupRepository;
    private final IndisponibiliteProvider indisponibilites;
    private final CalendrierTempsService calendrier;
    private final PeriodeTempsCloseRepository periodesCloses;

    public TempsService(PlanningTravailRepository planningRepository, PointageRepository pointageRepository,
                        JourTravailleRepository joursRepository, AnomaliePointageRepository anomaliesRepository,
                        HeureSupplementaireRepository heuresSupRepository,
                        IndisponibiliteProvider indisponibilites, CalendrierTempsService calendrier,
                        PeriodeTempsCloseRepository periodesCloses) {
        this.planningRepository = planningRepository;
        this.pointageRepository = pointageRepository;
        this.joursRepository = joursRepository;
        this.anomaliesRepository = anomaliesRepository;
        this.heuresSupRepository = heuresSupRepository;
        this.indisponibilites = indisponibilites;
        this.calendrier = calendrier;
        this.periodesCloses = periodesCloses;
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
        verifierPeriodeOuverte(demande.dateDebut());
        PlanningTravail planning = new PlanningTravail();
        planning.setIdEmploye(demande.idEmploye());
        planning.setJoursTravailles(demande.joursTravailles().trim());
        planning.setHeureDebut(demande.heureDebut());
        planning.setHeureFin(demande.heureFin());
        planning.setDateDebut(demande.dateDebut());
        planning.setDateFin(demande.dateFin());
        if (demande.pauseMinutes() != null) {
            if (demande.pauseMinutes() < 0) {
                throw new BusinessException("PAUSE_INVALIDE", "La pause ne peut pas etre negative.");
            }
            planning.setPauseMinutes(demande.pauseMinutes());
        }
        if (demande.toleranceMinutes() != null) {
            if (demande.toleranceMinutes() < 0) {
                throw new BusinessException("TOLERANCE_INVALIDE", "La tolerance ne peut pas etre negative.");
            }
            planning.setToleranceMinutes(demande.toleranceMinutes());
        }
        return TempsDtos.PlanningDto.de(planningRepository.save(planning));
    }

    @Transactional
    public TempsDtos.PointageDto pointerEntree(TempsDtos.PointageRequest demande) {
        LocalDate date = demande.date() == null ? LocalDate.now() : demande.date();
        LocalTime heure = demande.heure() == null ? LocalTime.now().withNano(0) : demande.heure();
        verifierPeriodeOuverte(date);
        verifierPasEnConge(demande.idEmploye(), date);
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
        verifierPeriodeOuverte(date);
        verifierPasEnConge(demande.idEmploye(), date);
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

    @Transactional
    public TempsDtos.JourTravailleDto calculerJour(Long idEmploye, LocalDate date) {
        LocalDate jour = date == null ? LocalDate.now() : date;
        PlanningTravail planning = planningPour(idEmploye, jour);
        Pointage pointage = pointageRepository.findByIdEmployeAndDatePointage(idEmploye, jour).orElse(null);
        boolean enConge = indisponibilites.estIndisponible(idEmploye, jour);
        boolean ferie = calendrier.estFerie(jour);

        if (enConge && pointage == null) {
            return enregistrer(idEmploye, jour, planning, null, JourTravaille.Statut.CONGE, List.of());
        }
        if (ferie && pointage == null) {
            if (planning == null) {
                return null;
            }
            return enregistrer(idEmploye, jour, planning, null, JourTravaille.Statut.FERIE, List.of());
        }
        if (pointage == null) {
            if (planning == null) {
                return null;
            }
            return enregistrer(idEmploye, jour, planning, null, JourTravaille.Statut.ABSENT,
                    List.of(anomalie(idEmploye, jour, "ABSENCE_NON_JUSTIFIEE", "HAUTE")));
        }

        List<AnomaliePointage> anomalies = new ArrayList<>();
        if (enConge) {
            anomalies.add(anomalie(idEmploye, jour, "POINTAGE_PENDANT_CONGE", "HAUTE"));
        }
        if (planning == null) {
            anomalies.add(anomalie(idEmploye, jour, "HORS_PLANNING", "HAUTE"));
        }
        if (ferie) {
            anomalies.add(anomalie(idEmploye, jour, "TRAVAIL_JOUR_FERIE_NON_PLANIFIE", "MOYENNE"));
        }

        LocalTime entree = pointage.getHeureEntree();
        LocalTime sortie = pointage.getHeureSortie();
        int presence = 0;
        int pause = 0;
        int travaillees = 0;
        int theoriques = 0;
        int retard = 0;
        int departAnticipe = 0;
        int nuit = 0;
        int dimanche = 0;
        int ferieMinutes = 0;

        if (sortie == null) {
            anomalies.add(anomalie(idEmploye, jour, "SORTIE_MANQUANTE", "HAUTE"));
        } else if (entree != null && sortie.isAfter(entree)) {
            presence = (int) Duration.between(entree, sortie).toMinutes();
            if (presence > SEUIL_PAUSE_MINUTES && planning != null) {
                pause = planning.getPauseMinutes();
            }
            travaillees = Math.max(0, presence - pause);
            nuit = minutesNuit(entree, sortie);
            if (jour.getDayOfWeek() == DayOfWeek.SUNDAY) {
                dimanche = travaillees;
            }
            if (ferie) {
                ferieMinutes = travaillees;
            }
            if (presence > DUREE_EXCESSIVE_MINUTES) {
                anomalies.add(anomalie(idEmploye, jour, "DUREE_EXCESSIVE", "HAUTE"));
            }
        }

        if (planning != null) {
            theoriques = Math.max(0, (int) Duration.between(planning.getHeureDebut(), planning.getHeureFin()).toMinutes()
                    - planning.getPauseMinutes());
            if (entree != null) {
                long ecart = Duration.between(planning.getHeureDebut(), entree).toMinutes();
                if (ecart > planning.getToleranceMinutes()) {
                    retard = (int) ecart;
                    anomalies.add(anomalie(idEmploye, jour, "RETARD_NON_JUSTIFIE", "MOYENNE"));
                }
            }
            if (sortie != null) {
                long ecart = Duration.between(sortie, planning.getHeureFin()).toMinutes();
                if (ecart > planning.getToleranceMinutes()) {
                    departAnticipe = (int) ecart;
                    anomalies.add(anomalie(idEmploye, jour, "DEPART_ANTICIPE", "MOYENNE"));
                }
            }
        }

        int hs = Math.max(0, travaillees - theoriques);
        JourTravaille.Statut statut = anomalies.isEmpty()
                ? JourTravaille.Statut.A_VALIDER : JourTravaille.Statut.ANOMALIE;
        return enregistrer(idEmploye, jour, planning, pointage, statut, anomalies,
                presence, pause, travaillees, theoriques, retard, departAnticipe, hs, nuit, dimanche, ferieMinutes);
    }

    @Transactional
    public TempsDtos.JourTravailleDto validerJour(Long idEmploye, LocalDate date) {
        LocalDate jourCalcule = date == null ? LocalDate.now() : date;
        verifierPeriodeOuverte(jourCalcule);
        TempsDtos.JourTravailleDto jour = calculerJour(idEmploye, jourCalcule);
        if (jour == null) {
            throw new BusinessException("JOUR_INTROUVABLE", "Aucun planning ni pointage pour cette journee.");
        }
        if (jour.anomalies().stream().anyMatch(a -> "OUVERTE".equals(a.statut()))) {
            throw new BusinessException("ANOMALIE_OUVERTE",
                    "La journee ne peut pas etre validee tant qu'une anomalie est ouverte.");
        }
        JourTravaille entite = joursRepository.findByIdEmployeAndDateJour(idEmploye, jourCalcule)
                .orElseThrow(() -> new BusinessException("JOUR_INTROUVABLE", "Synthese introuvable."));
        entite.setStatut(JourTravaille.Statut.VALIDE);
        entite.setDateCalcul(LocalDateTime.now());
        joursRepository.save(entite);
        return TempsDtos.JourTravailleDto.de(entite,
                anomaliesRepository.findByIdEmployeAndDateAnomalieOrderByIdAsc(idEmploye, jourCalcule));
    }

    @Transactional
    public TempsDtos.AnomalieDto traiterAnomalie(Long idAnomalie, String statut, String resolution, Long idUtilisateur) {
        AnomaliePointage anomalie = anomaliesRepository.findById(idAnomalie)
                .orElseThrow(() -> new BusinessException("ANOMALIE_INTROUVABLE", "Anomalie introuvable."));
        verifierPeriodeOuverte(anomalie.getDateAnomalie());
        AnomaliePointage.Statut cible;
        try {
            cible = AnomaliePointage.Statut.valueOf(statut);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException("STATUT_ANOMALIE_INVALIDE", "Statut d'anomalie invalide.");
        }
        if (cible == AnomaliePointage.Statut.OUVERTE) {
            throw new BusinessException("STATUT_ANOMALIE_INVALIDE", "Le statut OUVERTE ne peut pas etre applique manuellement.");
        }
        anomalie.setStatut(cible);
        anomalie.setResolution(resolution);
        anomalie.setIdResoluPar(idUtilisateur);
        anomalie.setDateResolution(LocalDateTime.now());
        anomaliesRepository.save(anomalie);

        joursRepository.findById(anomalie.getIdJourTravaille() == null ? -1L : anomalie.getIdJourTravaille())
                .ifPresent(jour -> {
                    boolean resteOuverte = anomaliesRepository.existsByIdEmployeAndDateAnomalieAndStatut(
                            jour.getIdEmploye(), jour.getDateJour(), AnomaliePointage.Statut.OUVERTE);
                    if (!resteOuverte && jour.getStatut() == JourTravaille.Statut.ANOMALIE) {
                        jour.setStatut(JourTravaille.Statut.A_VALIDER);
                        joursRepository.save(jour);
                    }
                });
        return TempsDtos.AnomalieDto.de(anomalie);
    }

    @Transactional
    public TempsDtos.HeureSupplementaireDto calculerSemaine(Long idEmploye, LocalDate dateDansSemaine) {
        LocalDate semaine = (dateDansSemaine == null ? LocalDate.now() : dateDansSemaine)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        int total = 0;
        int nuit = 0;
        int dimanche = 0;
        int ferie = 0;
        for (int i = 0; i < 7; i++) {
            TempsDtos.JourTravailleDto jour = calculerJour(idEmploye, semaine.plusDays(i));
            if (jour != null) {
                total += jour.minutesTravaillees();
                nuit += jour.minutesNuit();
                dimanche += jour.minutesDimanche();
                ferie += jour.minutesFerie();
            }
        }
        int hs = Math.max(0, total - HEURES_HEBDO * 60);
        int hs30 = Math.min(hs, 8 * 60);
        int hs50 = Math.max(0, hs - 8 * 60);
        HeureSupplementaire heureSup = heuresSupRepository
                .findByIdEmployeAndSemaineDebut(idEmploye, semaine)
                .orElseGet(HeureSupplementaire::new);
        heureSup.setIdEmploye(idEmploye);
        heureSup.setSemaineDebut(semaine);
        heureSup.setMinutesTotal(total);
        heureSup.setMinutesHs30(hs30);
        heureSup.setMinutesHs50(hs50);
        heureSup.setMinutesNuit(nuit);
        heureSup.setMinutesDimanche(dimanche);
        heureSup.setMinutesFerie(ferie);
        heureSup.setAlertePlafond(hs > PLAFOND_HS_HEBDO_MINUTES);
        heureSup.setDateCalcul(LocalDateTime.now());
        return TempsDtos.HeureSupplementaireDto.de(heuresSupRepository.save(heureSup));
    }

    @Transactional
    public TempsDtos.HeureSupplementaireDto heuresSupplementaires(Long idEmploye, LocalDate semaine) {
        LocalDate lundi = (semaine == null ? LocalDate.now() : semaine)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return heuresSupRepository.findByIdEmployeAndSemaineDebut(idEmploye, lundi)
                .map(TempsDtos.HeureSupplementaireDto::de)
                .orElseGet(() -> calculerSemaine(idEmploye, lundi));
    }

    @Transactional(readOnly = true)
    public List<TempsDtos.PeriodeCloseDto> periodesCloturees() {
        return periodesCloses.findAllByOrderByAnneeDescMoisDesc().stream()
                .map(TempsDtos.PeriodeCloseDto::de).toList();
    }

    @Transactional
    public TempsDtos.PeriodeCloseDto cloturer(TempsDtos.ClotureRequest request, Long idUtilisateur) {
        YearMonth mois = YearMonth.of(request.annee(), request.mois());
        if (periodesCloses.findByAnneeAndMois(mois.getYear(), mois.getMonthValue()).isPresent()) {
            throw new BusinessException("PERIODE_DEJA_CLOTUREE",
                    "La periode " + mois + " est deja cloturee.");
        }
        LocalDate debut = mois.atDay(1);
        LocalDate fin = mois.atEndOfMonth();
        if (fin.isAfter(LocalDate.now())) {
            throw new BusinessException("PERIODE_NON_TERMINEE",
                    "La periode ne peut etre cloturee avant sa fin.");
        }
        if (anomaliesRepository.existsByDateAnomalieBetweenAndStatut(debut, fin,
                AnomaliePointage.Statut.OUVERTE)) {
            throw new BusinessException("ANOMALIE_OUVERTE",
                    "La periode contient des anomalies ouvertes : validation impossible.");
        }
        PeriodeTempsClose periode = new PeriodeTempsClose();
        periode.setAnnee(mois.getYear());
        periode.setMois(mois.getMonthValue());
        periode.setDateDebut(debut);
        periode.setDateFin(fin);
        periode.setIdUtilisateur(idUtilisateur);
        return TempsDtos.PeriodeCloseDto.de(periodesCloses.save(periode));
    }

    private void verifierPeriodeOuverte(LocalDate date) {
        periodesCloses.couvrant(date).ifPresent(periode -> {
            throw new BusinessException("PERIODE_CLOTUREE",
                    "La periode " + periode.getAnnee() + "-" + String.format("%02d", periode.getMois())
                            + " est cloturee : aucune modification n'est autorisee.");
        });
    }

    private TempsDtos.JourTravailleDto enregistrer(Long idEmploye, LocalDate date, PlanningTravail planning,
                                                  Pointage pointage, JourTravaille.Statut statut,
                                                  List<AnomaliePointage> anomalies) {
        return enregistrer(idEmploye, date, planning, pointage, statut, anomalies, 0, 0, 0,
                planning == null ? 0 : Math.max(0, (int) Duration.between(planning.getHeureDebut(),
                        planning.getHeureFin()).toMinutes() - planning.getPauseMinutes()),
                0, 0, 0, 0, 0, 0);
    }

    private TempsDtos.JourTravailleDto enregistrer(Long idEmploye, LocalDate date, PlanningTravail planning,
                                                  Pointage pointage, JourTravaille.Statut statut,
                                                  List<AnomaliePointage> anomalies, int presence, int pause,
                                                  int travaillees, int theoriques, int retard, int departAnticipe,
                                                  int hs, int nuit, int dimanche, int ferie) {
        anomaliesRepository.deleteByIdEmployeAndDateAnomalie(idEmploye, date);
        JourTravaille jour = joursRepository.findByIdEmployeAndDateJour(idEmploye, date)
                .orElseGet(JourTravaille::new);
        jour.setIdEmploye(idEmploye);
        jour.setDateJour(date);
        jour.setIdPlanning(planning == null ? null : planning.getId());
        jour.setHeureEntree(pointage == null ? null : pointage.getHeureEntree());
        jour.setHeureSortie(pointage == null ? null : pointage.getHeureSortie());
        jour.setMinutesPresence(presence);
        jour.setMinutesPause(pause);
        jour.setMinutesTravaillees(travaillees);
        jour.setMinutesTheoriques(theoriques);
        jour.setMinutesRetard(retard);
        jour.setMinutesDepartAnticipe(departAnticipe);
        jour.setMinutesHs(hs);
        jour.setMinutesNuit(nuit);
        jour.setMinutesDimanche(dimanche);
        jour.setMinutesFerie(ferie);
        jour.setStatut(statut);
        jour.setDateCalcul(LocalDateTime.now());
        jour = joursRepository.save(jour);

        List<AnomaliePointage> enregistrees = new ArrayList<>();
        for (AnomaliePointage anomalie : anomalies) {
            anomalie.setIdJourTravaille(jour.getId());
            enregistrees.add(anomaliesRepository.save(anomalie));
        }
        return TempsDtos.JourTravailleDto.de(jour, enregistrees);
    }

    private AnomaliePointage anomalie(Long idEmploye, LocalDate date, String type, String gravite) {
        AnomaliePointage anomalie = new AnomaliePointage();
        anomalie.setIdEmploye(idEmploye);
        anomalie.setDateAnomalie(date);
        anomalie.setType(type);
        anomalie.setGravite(gravite);
        anomalie.setStatut(AnomaliePointage.Statut.OUVERTE);
        return anomalie;
    }

    private static int minutesNuit(LocalTime entree, LocalTime sortie) {
        return chevauchement(entree, sortie, LocalTime.MIDNIGHT, LocalTime.of(5, 0))
                + chevauchement(entree, sortie, LocalTime.of(22, 0), LocalTime.MAX);
    }

    private static int chevauchement(LocalTime debut1, LocalTime fin1, LocalTime debut2, LocalTime fin2) {
        LocalTime debut = debut1.isAfter(debut2) ? debut1 : debut2;
        LocalTime fin = fin1.isBefore(fin2) ? fin1 : fin2;
        return fin.isAfter(debut) ? (int) Duration.between(debut, fin).toMinutes() : 0;
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
                .filter(planning -> jourTravaille(planning, date))
                .orElse(null);
    }

    private static boolean jourTravaille(PlanningTravail planning, LocalDate date) {
        String brut = planning.getJoursTravailles();
        if (brut == null || brut.isBlank()) {
            return true;
        }
        String jours = brut.toUpperCase().replace(" ", "");
        int cible = date.getDayOfWeek().getValue();
        if (jours.contains("-") && !jours.contains(",")) {
            String[] bornes = jours.split("-", 2);
            int debut = indiceJour(bornes[0]);
            int fin = indiceJour(bornes[1]);
            if (debut > 0 && fin > 0) {
                return debut <= fin ? cible >= debut && cible <= fin : cible >= debut || cible <= fin;
            }
        }
        for (String token : jours.split("[,;]")) {
            if (indiceJour(token) == cible) {
                return true;
            }
        }
        return false;
    }

    private static int indiceJour(String token) {
        if (token == null || token.isBlank()) {
            return 0;
        }
        return switch (token.trim().toUpperCase()) {
            case "LUNDI", "LUN", "MONDAY", "MON", "1" -> 1;
            case "MARDI", "MAR", "TUESDAY", "TUE", "2" -> 2;
            case "MERCREDI", "MER", "WEDNESDAY", "WED", "3" -> 3;
            case "JEUDI", "JEU", "THURSDAY", "THU", "4" -> 4;
            case "VENDREDI", "VEN", "FRIDAY", "FRI", "5" -> 5;
            case "SAMEDI", "SAM", "SATURDAY", "SAT", "6" -> 6;
            case "DIMANCHE", "DIM", "SUNDAY", "SUN", "7" -> 7;
            default -> 0;
        };
    }

    private void verifierPasEnConge(Long idEmploye, LocalDate date) {
        if (indisponibilites.estIndisponible(idEmploye, date)) {
            throw new BusinessException("POINTAGE_INTERDIT_CONGE",
                    "Le pointage est interdit pendant un conge valide.");
        }
    }
}
