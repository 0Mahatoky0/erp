package com.tovo.erp.temps.service;

import com.tovo.erp.employe.service.EmployeService;
import com.tovo.erp.shared.BusinessException;
import com.tovo.erp.temps.dto.TempsDtos.AffectationRegimeDto;
import com.tovo.erp.temps.dto.TempsDtos.AffectationRegimeRequest;
import com.tovo.erp.temps.dto.TempsDtos.HoraireDto;
import com.tovo.erp.temps.dto.TempsDtos.HoraireRequest;
import com.tovo.erp.temps.dto.TempsDtos.RegimeDto;
import com.tovo.erp.temps.dto.TempsDtos.RegimeRequest;
import com.tovo.erp.temps.model.AffectationRegime;
import com.tovo.erp.temps.model.HoraireTravail;
import com.tovo.erp.temps.model.PlanningTravail;
import com.tovo.erp.temps.model.RegimeTravail;
import com.tovo.erp.temps.model.TypeRegimeTravail;
import com.tovo.erp.temps.repository.AffectationRegimeRepository;
import com.tovo.erp.temps.repository.HoraireTravailRepository;
import com.tovo.erp.temps.repository.PlanningTravailRepository;
import com.tovo.erp.temps.repository.RegimeTravailRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Regimes de travail, plages horaires et affectations historisees (bloc 6,
 * RG-B6-01 a RG-B6-04). Une affectation peut generer automatiquement le
 * planning correspondant utilise par le moteur de pointage.
 */
@Service
public class RegimeTravailService {

    private static final String JOURS_DEFAUT = "LUNDI,MARDI,MERCREDI,JEUDI,VENDREDI";
    private static final Set<String> JOURS_VALIDES = Set.of(
            "LUNDI", "MARDI", "MERCREDI", "JEUDI", "VENDREDI", "SAMEDI", "DIMANCHE");

    private final RegimeTravailRepository regimes;
    private final HoraireTravailRepository horaires;
    private final AffectationRegimeRepository affectations;
    private final PlanningTravailRepository plannings;
    private final EmployeService employes;

    public RegimeTravailService(RegimeTravailRepository regimes,
                                HoraireTravailRepository horaires,
                                AffectationRegimeRepository affectations,
                                PlanningTravailRepository plannings,
                                EmployeService employes) {
        this.regimes = regimes;
        this.horaires = horaires;
        this.affectations = affectations;
        this.plannings = plannings;
        this.employes = employes;
    }

    @Transactional(readOnly = true)
    public List<RegimeDto> lister() {
        return regimes.findAllByOrderByLibelleAsc().stream()
                .map(regime -> RegimeDto.de(regime,
                        horaires.findByIdRegimeOrderByJourSemaineAscHeureDebutAsc(regime.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public RegimeDto regime(Long id) {
        RegimeTravail regime = exigerRegime(id);
        return RegimeDto.de(regime, horaires.findByIdRegimeOrderByJourSemaineAscHeureDebutAsc(id));
    }

    @Transactional
    public RegimeDto creer(RegimeRequest requete) {
        String code = normaliserCode(requete.code());
        if (regimes.findByCodeIgnoreCase(code).isPresent()) {
            throw new BusinessException("REGIME_CODE_EXISTANT",
                    "Un regime avec le code " + code + " existe deja.");
        }
        RegimeTravail regime = new RegimeTravail();
        appliquer(regime, requete, code);
        return RegimeDto.de(regimes.save(regime), List.of());
    }

    @Transactional
    public RegimeDto modifier(Long id, RegimeRequest requete) {
        RegimeTravail regime = exigerRegime(id);
        String code = normaliserCode(requete.code());
        regimes.findByCodeIgnoreCase(code)
                .filter(autre -> !autre.getId().equals(id))
                .ifPresent(autre -> {
                    throw new BusinessException("REGIME_CODE_EXISTANT",
                            "Un regime avec le code " + code + " existe deja.");
                });
        appliquer(regime, requete, code);
        return RegimeDto.de(regimes.save(regime),
                horaires.findByIdRegimeOrderByJourSemaineAscHeureDebutAsc(id));
    }

    @Transactional
    public void supprimer(Long id) {
        exigerRegime(id);
        if (affectations.existsByIdRegime(id)) {
            throw new BusinessException("REGIME_UTILISE",
                    "Ce regime est affecte a au moins un salarie et ne peut pas etre supprime.");
        }
        horaires.deleteByIdRegime(id);
        regimes.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<HoraireDto> horaires(Long idRegime) {
        exigerRegime(idRegime);
        return horaires.findByIdRegimeOrderByJourSemaineAscHeureDebutAsc(idRegime).stream()
                .map(HoraireDto::de)
                .toList();
    }

    @Transactional
    public HoraireDto ajouterHoraire(Long idRegime, HoraireRequest requete) {
        exigerRegime(idRegime);
        validerHoraire(requete);
        HoraireTravail horaire = new HoraireTravail();
        horaire.setIdRegime(idRegime);
        horaire.setJourSemaine(normaliserJour(requete.jourSemaine()));
        horaire.setHeureDebut(requete.heureDebut());
        horaire.setHeureFin(requete.heureFin());
        horaire.setPauseDebut(requete.pauseDebut());
        horaire.setPauseFin(requete.pauseFin());
        return HoraireDto.de(horaires.save(horaire));
    }

    @Transactional
    public void supprimerHoraire(Long idHoraire) {
        if (!horaires.existsById(idHoraire)) {
            throw BusinessException.notFound("L'horaire " + idHoraire + " n'existe pas.");
        }
        horaires.deleteById(idHoraire);
    }

    @Transactional(readOnly = true)
    public List<AffectationRegimeDto> affectations(Long idEmploye) {
        return affectations.findByIdEmployeOrderByDateDebutDesc(idEmploye).stream()
                .map(affectation -> AffectationRegimeDto.de(affectation,
                        exigerRegime(affectation.getIdRegime())))
                .toList();
    }

    @Transactional(readOnly = true)
    public AffectationRegimeDto regimeCourant(Long idEmploye, LocalDate date) {
        LocalDate reference = date == null ? LocalDate.now() : date;
        return affectations.courantes(idEmploye, reference).stream()
                .findFirst()
                .map(affectation -> AffectationRegimeDto.de(affectation,
                        exigerRegime(affectation.getIdRegime())))
                .orElse(null);
    }

    @Transactional
    public AffectationRegimeDto affecter(Long idEmploye, AffectationRegimeRequest requete) {
        if (!employes.existe(idEmploye)) {
            throw BusinessException.notFound("L'employe " + idEmploye + " n'existe pas.");
        }
        if (requete.dateFin() != null && requete.dateFin().isBefore(requete.dateDebut())) {
            throw new BusinessException("DATES_INVALIDES",
                    "La date de fin doit etre posterieure ou egale a la date de debut.");
        }
        LocalDate fin = requete.dateFin() == null ? LocalDate.of(9999, 12, 31) : requete.dateFin();
        if (affectations.countChevauchement(idEmploye, requete.dateDebut(), fin) > 0) {
            throw new BusinessException("REGIME_CHEVAUCHEMENT",
                    "Une affectation de regime existe deja sur cette periode pour ce salarie.");
        }
        RegimeTravail regime = exigerRegime(requete.idRegime());
        AffectationRegime affectation = new AffectationRegime();
        affectation.setIdEmploye(idEmploye);
        affectation.setIdRegime(regime.getId());
        affectation.setDateDebut(requete.dateDebut());
        affectation.setDateFin(requete.dateFin());
        AffectationRegime enregistree = affectations.save(affectation);

        boolean generer = requete.appliquerPlanning() == null || requete.appliquerPlanning();
        if (generer) {
            List<HoraireTravail> plages = horaires
                    .findByIdRegimeOrderByJourSemaineAscHeureDebutAsc(regime.getId());
            if (plages.isEmpty()) {
                throw new BusinessException("HORAIRES_REQUIS",
                        "Ajoutez au moins une plage horaire au regime avant de l'affecter.");
            }
            PlanningTravail planning = new PlanningTravail();
            planning.setIdEmploye(idEmploye);
            planning.setJoursTravailles(regime.getJoursTravailles());
            planning.setHeureDebut(plages.stream().map(HoraireTravail::getHeureDebut)
                    .min(LocalTime::compareTo).orElse(LocalTime.of(8, 0)));
            planning.setHeureFin(plages.stream().map(HoraireTravail::getHeureFin)
                    .max(LocalTime::compareTo).orElse(LocalTime.of(17, 0)));
            planning.setDateDebut(requete.dateDebut());
            planning.setDateFin(requete.dateFin());
            planning.setPauseMinutes(regime.getPauseMinutes());
            planning.setToleranceMinutes(regime.getToleranceRetardMin());
            PlanningTravail enregistre = plannings.save(planning);
            enregistree.setIdPlanning(enregistre.getId());
            enregistree = affectations.save(enregistree);
        }
        return AffectationRegimeDto.de(enregistree, regime);
    }

    private void appliquer(RegimeTravail regime, RegimeRequest requete, String code) {
        regime.setCode(code);
        regime.setLibelle(requete.libelle().trim());
        regime.setType(requete.type() == null ? TypeRegimeTravail.HORAIRE_FIXE : requete.type());
        regime.setHeuresHebdo(requete.heuresHebdo() == null ? 40 : requete.heuresHebdo());
        regime.setJoursTravailles(normaliserJours(requete.joursTravailles()));
        regime.setToleranceRetardMin(requete.toleranceRetardMin() == null ? 10 : requete.toleranceRetardMin());
        regime.setToleranceDepartMin(requete.toleranceDepartMin() == null ? 10 : requete.toleranceDepartMin());
        regime.setPauseMinutes(requete.pauseMinutes() == null ? 60 : requete.pauseMinutes());
        regime.setPausePayee(requete.pausePayee() != null && requete.pausePayee());
        regime.setDecompteHeuresSup(requete.decompteHeuresSup() == null || requete.decompteHeuresSup());
    }

    private void validerHoraire(HoraireRequest requete) {
        if (!requete.heureFin().isAfter(requete.heureDebut())) {
            throw new BusinessException("HORAIRES_INVALIDES",
                    "L'heure de fin doit etre posterieure a l'heure de debut.");
        }
        if (requete.pauseDebut() != null || requete.pauseFin() != null) {
            if (requete.pauseDebut() == null || requete.pauseFin() == null) {
                throw new BusinessException("PAUSE_INVALIDE",
                        "Les heures de debut et de fin de pause doivent etre renseignees ensemble.");
            }
            if (!requete.pauseFin().isAfter(requete.pauseDebut())
                    || requete.pauseDebut().isBefore(requete.heureDebut())
                    || requete.pauseFin().isAfter(requete.heureFin())) {
                throw new BusinessException("PAUSE_INVALIDE",
                        "La pause doit etre comprise dans la plage horaire.");
            }
        }
    }

    private String normaliserCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException("CODE_REQUIS", "Le code du regime est obligatoire.");
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String normaliserJour(String jour) {
        String normalise = jour == null ? "" : jour.trim().toUpperCase(Locale.ROOT);
        if (!JOURS_VALIDES.contains(normalise)) {
            throw new BusinessException("JOUR_INVALIDE",
                    "Le jour « " + jour + " » n'est pas valide.");
        }
        return normalise;
    }

    private String normaliserJours(String jours) {
        if (jours == null || jours.isBlank()) {
            return JOURS_DEFAUT;
        }
        LinkedHashSet<String> retenus = Arrays.stream(jours.split(","))
                .map(String::trim)
                .filter(jour -> !jour.isBlank())
                .map(jour -> normaliserJour(jour))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (retenus.isEmpty()) {
            return JOURS_DEFAUT;
        }
        return String.join(",", retenus);
    }

    private RegimeTravail exigerRegime(Long idRegime) {
        return regimes.findById(idRegime)
                .orElseThrow(() -> BusinessException.notFound(
                        "Le regime de travail " + idRegime + " n'existe pas."));
    }
}
