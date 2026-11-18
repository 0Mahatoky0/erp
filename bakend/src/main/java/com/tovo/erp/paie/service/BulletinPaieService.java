package com.tovo.erp.paie.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tovo.erp.contrat.dto.ContratDtos;
import com.tovo.erp.contrat.service.CarriereService;
import com.tovo.erp.contrat.service.ContratService;
import com.tovo.erp.employe.dto.EmployeDto;
import com.tovo.erp.employe.service.EmployeService;
import com.tovo.erp.paie.dto.BulletinPaieDtos;
import com.tovo.erp.paie.dto.PaieDtos;
import com.tovo.erp.paie.model.*;
import com.tovo.erp.paie.repository.*;
import com.tovo.erp.shared.BusinessException;
import com.tovo.erp.shared.Roles;
import com.tovo.erp.shared.UtilisateurConnecte;
import com.tovo.erp.shared.UtilisateurCourant;
import com.tovo.erp.shared.audit.JournalAuditService;
import com.tovo.erp.shared.reference.ReferenceService;
import com.tovo.erp.temps.dto.TempsDtos;
import com.tovo.erp.temps.service.TempsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BulletinPaieService {

    private static final String VERSION_REGLES = "IRSA-2026-CNAPS1-OSTIE1";
    private static final BigDecimal JOURS_MOIS = BigDecimal.valueOf(30);
    private static final BigDecimal HEURES_MOIS = new BigDecimal("173.33");
    private static final BigDecimal CENT = BigDecimal.valueOf(100);
    private static final BigDecimal DEFAULT_SMIG = new BigDecimal("1400000");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ParametrePaieRepository parametres;
    private final PeriodePaieRepository periodes;
    private final BulletinPaieRepository bulletins;
    private final LigneBulletinRepository lignes;
    private final FichePaieRepository fiches;
    private final BaremeIrsaRepository baremes;
    private final RubriquePaieRepository rubriques;
    private final EmployeService employes;
    private final ContratService contrats;
    private final CarriereService carriere;
    private final TempsService temps;
    private final PaieService variablesConges;
    private final ReferenceService references;
    private final UtilisateurCourant utilisateurCourant;
    private final JournalAuditService audit;

    public BulletinPaieService(ParametrePaieRepository parametres, PeriodePaieRepository periodes,
                               BulletinPaieRepository bulletins, LigneBulletinRepository lignes,
                               FichePaieRepository fiches, BaremeIrsaRepository baremes,
                               RubriquePaieRepository rubriques, EmployeService employes,
                               ContratService contrats, CarriereService carriere, TempsService temps,
                               PaieService variablesConges, ReferenceService references,
                               UtilisateurCourant utilisateurCourant, JournalAuditService audit) {
        this.parametres = parametres;
        this.periodes = periodes;
        this.bulletins = bulletins;
        this.lignes = lignes;
        this.fiches = fiches;
        this.baremes = baremes;
        this.rubriques = rubriques;
        this.employes = employes;
        this.contrats = contrats;
        this.carriere = carriere;
        this.temps = temps;
        this.variablesConges = variablesConges;
        this.references = references;
        this.utilisateurCourant = utilisateurCourant;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public BulletinPaieDtos.ParametresDto parametres() {
        return parametres.findFirstByActifTrueOrderByDateModificationDesc()
                .map(BulletinPaieDtos.ParametresDto::de)
                .orElse(null);
    }

    @Transactional
    public BulletinPaieDtos.ParametresDto enregistrerParametres(BulletinPaieDtos.ParametresRequest request) {
        ParametrePaie precedent = parametres.findFirstByActifTrueOrderByDateModificationDesc().orElse(null);
        if (precedent != null) {
            precedent.setActif(false);
            parametres.save(precedent);
        }
        ParametrePaie parametre = new ParametrePaie();
        parametre.setActif(true);
        parametre.setDateModification(LocalDateTime.now());
        parametre.setTauxCotisationsSalariales(request.tauxCotisationsSalariales());
        parametre.setTauxCotisationsPatronales(request.tauxCotisationsPatronales());
        parametre.setTauxIrsa(request.tauxIrsa());
        parametre.setSmig(coalesce(request.smig(), precedent, ParametrePaie::getSmig, DEFAULT_SMIG));
        parametre.setPlafondCnapsSmig(coalesce(request.plafondCnapsSmig(), precedent,
                ParametrePaie::getPlafondCnapsSmig, BigDecimal.valueOf(8)));
        parametre.setTauxCnapsSalarial(coalesce(request.tauxCnapsSalarial(), precedent,
                ParametrePaie::getTauxCnapsSalarial, BigDecimal.ONE));
        parametre.setTauxCnapsPatronal(coalesce(request.tauxCnapsPatronal(), precedent,
                ParametrePaie::getTauxCnapsPatronal, BigDecimal.valueOf(13)));
        parametre.setTauxOstieSalarial(coalesce(request.tauxOstieSalarial(), precedent,
                ParametrePaie::getTauxOstieSalarial, BigDecimal.ONE));
        parametre.setTauxOstiePatronal(coalesce(request.tauxOstiePatronal(), precedent,
                ParametrePaie::getTauxOstiePatronal, BigDecimal.valueOf(5)));
        parametre.setTauxFmfpPatronal(coalesce(request.tauxFmfpPatronal(), precedent,
                ParametrePaie::getTauxFmfpPatronal, BigDecimal.ONE));
        parametre.setMajorationHs30(coalesce(request.majorationHs30(), precedent,
                ParametrePaie::getMajorationHs30, BigDecimal.valueOf(30)));
        parametre.setMajorationHs50(coalesce(request.majorationHs50(), precedent,
                ParametrePaie::getMajorationHs50, BigDecimal.valueOf(50)));
        parametre.setMajorationNuit(coalesce(request.majorationNuit(), precedent,
                ParametrePaie::getMajorationNuit, BigDecimal.valueOf(30)));
        parametre.setMajorationDimanche(coalesce(request.majorationDimanche(), precedent,
                ParametrePaie::getMajorationDimanche, BigDecimal.valueOf(50)));
        parametre.setMajorationFerie(coalesce(request.majorationFerie(), precedent,
                ParametrePaie::getMajorationFerie, BigDecimal.valueOf(100)));
        parametre.setMinimumIrsa(coalesce(request.minimumIrsa(), precedent,
                ParametrePaie::getMinimumIrsa, BigDecimal.ZERO));
        parametre.setReductionCharge(coalesce(request.reductionCharge(), precedent,
                ParametrePaie::getReductionCharge, BigDecimal.ZERO));
        parametre.setQuotientRetenue(coalesce(request.quotientRetenue(), precedent,
                ParametrePaie::getQuotientRetenue, BigDecimal.valueOf(20)));
        parametre.setDateEffet(request.dateEffet());
        BulletinPaieDtos.ParametresDto cree = BulletinPaieDtos.ParametresDto.de(parametres.save(parametre));
        audit.enregistrer("PARAMETRES_PAIE", "parametre_paie", cree.id(), null, cree.toString());
        return cree;
    }

    @Transactional
    public BulletinPaieDtos.PeriodeDto consolider(Long idPeriode) {
        PeriodePaie periode = periode(idPeriode);
        verifierModifiable(periode);
        periode.setStatut(StatutPeriodePaie.VARIABLES_CONSOLIDEES);
        periode.setDateClotureVariables(LocalDate.now());
        periode.setVersionRegles(VERSION_REGLES);
        audit.enregistrer("CONSOLIDATION_PAIE", "periode_paie", periode.getId(), null, VERSION_REGLES);
        return BulletinPaieDtos.PeriodeDto.de(periodes.save(periode));
    }

    @Transactional
    public BulletinPaieDtos.PeriodeAvecBulletinsDto calculer(LocalDate mois) {
        ParametrePaie parametre = parametres.findFirstByActifTrueOrderByDateModificationDesc()
                .orElseThrow(() -> new BusinessException("PARAMETRES_PAIE_MANQUANTS",
                        "Configurez les paramètres réglementaires avant de calculer une période."));
        if (baremes.findByDateEffetLessThanEqualOrderByBorneInfAsc(mois).isEmpty()) {
            throw new BusinessException("BAREME_IRSA_MANQUANT",
                    "Aucun barème IRSA n'est applicable à cette période.");
        }
        LocalDate debutMois = mois.withDayOfMonth(1);
        if (debutMois.isAfter(YearMonth.now().atDay(1))) {
            throw new BusinessException("PERIODE_FUTURE", "Une période future ne peut pas être calculée.");
        }
        PeriodePaie periode = periodes.findByMois(debutMois).orElseGet(() -> {
            PeriodePaie nouvelle = new PeriodePaie();
            nouvelle.setMois(debutMois);
            nouvelle.setStatut(StatutPeriodePaie.OUVERTE);
            return periodes.save(nouvelle);
        });
        verifierModifiable(periode);

        List<EmployeDto.Actif> actifs = employes.actifs();
        Map<Long, ContratDtos.ContratDto> contratsParEmploye = contrats.actifs().stream()
                .collect(Collectors.toMap(ContratDtos.ContratDto::idEmploye, Function.identity(),
                        (a, b) -> a, LinkedHashMap::new));
        List<Long> ids = actifs.stream().map(EmployeDto.Actif::id).toList();
        Map<Long, BigDecimal> salaires = carriere.salairesBaseParIds(ids);
        Map<Long, TempsDtos.VariablePaieDto> tempsParEmploye = temps
                .variablesPaie(debutMois, debutMois.with(TemporalAdjusters.lastDayOfMonth())).stream()
                .collect(Collectors.toMap(TempsDtos.VariablePaieDto::idEmploye, Function.identity()));
        Map<Long, BigDecimal> joursNonPayes = joursNonPayes(actifs, debutMois);
        Map<String, RubriquePaie> rubriquesParCode = rubriques
                .findByDateEffetLessThanEqualOrderByOrdreAsc(debutMois).stream()
                .collect(Collectors.toMap(RubriquePaie::getCode, Function.identity(), (a, b) -> a));
        List<BaremeIrsa> tranches = baremes.findByDateEffetLessThanEqualOrderByBorneInfAsc(debutMois);

        for (EmployeDto.Actif employe : actifs) {
            ContratDtos.ContratDto contrat = contratsParEmploye.get(employe.id());
            BigDecimal salaireBase = salaires.get(employe.id());
            if (salaireBase == null && contrat != null) {
                salaireBase = contrat.salaireBase();
            }
            if (contrat == null || salaireBase == null) {
                continue;
            }
            BulletinPaie bulletin = bulletins.findByIdPeriodeAndIdEmploye(periode.getId(), employe.id())
                    .orElseGet(BulletinPaie::new);
            bulletin.setIdPeriode(periode.getId());
            bulletin.setIdEmploye(employe.id());
            bulletin.setIdContrat(contrat.id());
            bulletin.setDateCreation(bulletin.getDateCreation() == null ? LocalDateTime.now()
                    : bulletin.getDateCreation());
            if (bulletin.getStatut() == StatutBulletinPaie.ANNULE) {
                bulletin.setStatut(StatutBulletinPaie.BROUILLON);
            }
            calculerBulletin(bulletin, salaireBase,
                    tempsParEmploye.getOrDefault(employe.id(), new TempsDtos.VariablePaieDto(employe.id(), 0, 0, 0, 0, 0)),
                    joursNonPayes.getOrDefault(employe.id(), BigDecimal.ZERO),
                    parametre, tranches, rubriquesParCode);
        }
        periode.setStatut(StatutPeriodePaie.CALCULEE);
        periode.setVersionRegles(VERSION_REGLES);
        periodes.save(periode);
        audit.enregistrer("CALCUL_PAIE", "periode_paie", periode.getId(), null, VERSION_REGLES);
        return lire(periode.getId());
    }

    @Transactional(readOnly = true)
    public BulletinPaieDtos.PeriodeAvecBulletinsDto lire(Long idPeriode) {
        PeriodePaie periode = periode(idPeriode);
        return new BulletinPaieDtos.PeriodeAvecBulletinsDto(
                BulletinPaieDtos.PeriodeDto.de(periode),
                bulletins.findByIdPeriodeOrderByIdEmployeAsc(idPeriode).stream()
                        .map(BulletinPaieDtos.BulletinDto::de).toList());
    }

    @Transactional(readOnly = true)
    public List<BulletinPaieDtos.PeriodeDto> periodes() {
        return periodes.findAllByOrderByMoisDesc().stream()
                .map(BulletinPaieDtos.PeriodeDto::de).toList();
    }

    @Transactional(readOnly = true)
    public BulletinPaieDtos.BulletinDetailDto detail(Long idBulletin) {
        BulletinPaie bulletin = bulletin(idBulletin);
        verifierAccesFiche(bulletin.getIdEmploye());
        return new BulletinPaieDtos.BulletinDetailDto(
                BulletinPaieDtos.BulletinDto.de(bulletin),
                lignes.findByIdBulletinOrderByOrdreAsc(idBulletin).stream()
                        .map(BulletinPaieDtos.LigneDto::de).toList());
    }

    @Transactional(readOnly = true)
    public List<BulletinPaieDtos.LigneDto> lignes(Long idBulletin) {
        bulletin(idBulletin);
        verifierAccesFiche(bulletin(idBulletin).getIdEmploye());
        return lignes.findByIdBulletinOrderByOrdreAsc(idBulletin).stream()
                .map(BulletinPaieDtos.LigneDto::de).toList();
    }

    @Transactional
    public BulletinPaieDtos.BulletinDto enregistrerRetenue(
            Long idBulletin, BulletinPaieDtos.RetenueRequest request) {
        return ajuster(idBulletin, new BulletinPaieDtos.AjustementRequest(null, request.montant(),
                null, "Retenue manuelle"));
    }

    @Transactional
    public BulletinPaieDtos.BulletinDto ajuster(Long idBulletin, BulletinPaieDtos.AjustementRequest request) {
        BulletinPaie bulletin = bulletin(idBulletin);
        PeriodePaie periode = periode(bulletin.getIdPeriode());
        verifierModifiable(periode);
        if (bulletin.getStatut() != StatutBulletinPaie.BROUILLON
                && bulletin.getStatut() != StatutBulletinPaie.CALCULE) {
            throw new BusinessException("BULLETIN_NON_MODIFIABLE",
                    "Un bulletin validé ou une période clôturée ne peut plus être modifié.");
        }
        bulletin.setPrimeExceptionnelle(request.primeExceptionnelle() == null
                ? bulletin.getPrimeExceptionnelle() : request.primeExceptionnelle());
        bulletin.setAvance(request.avance() == null ? bulletin.getAvance() : request.avance());
        bulletin.setPersonnesCharge(request.personnesCharge() == null
                ? bulletin.getPersonnesCharge() : request.personnesCharge());
        bulletin.setMotifAjustement(request.motif());
        bulletin.setRetenueManuelle(BigDecimal.ZERO);

        ParametrePaie parametre = parametres.findFirstByActifTrueOrderByDateModificationDesc()
                .orElseThrow(() -> new BusinessException("PARAMETRES_PAIE_MANQUANTS",
                        "Paramètres de paie manquants."));
        BigDecimal salaireBase = carriere.salairesBaseParIds(List.of(bulletin.getIdEmploye()))
                .get(bulletin.getIdEmploye());
        if (salaireBase == null) {
            ContratDtos.ContratDto contrat = contrats.courant(bulletin.getIdEmploye());
            salaireBase = contrat == null ? bulletin.getSalaireBasePeriode() : contrat.salaireBase();
        }
        TempsDtos.VariablePaieDto variable = temps.variablesPaie(periode.getMois(),
                periode.getMois().with(TemporalAdjusters.lastDayOfMonth())).stream()
                .filter(v -> v.idEmploye().equals(bulletin.getIdEmploye()))
                .findFirst().orElseGet(() -> new TempsDtos.VariablePaieDto(bulletin.getIdEmploye(), 0, 0, 0, 0, 0));
        BigDecimal joursNonPayes = BigDecimal.valueOf(30 - bulletin.getJourPayes());
        calculerBulletin(bulletin, salaireBase, variable, joursNonPayes, parametre,
                baremes.findByDateEffetLessThanEqualOrderByBorneInfAsc(periode.getMois()),
                rubriques.findByDateEffetLessThanEqualOrderByOrdreAsc(periode.getMois()).stream()
                        .collect(Collectors.toMap(RubriquePaie::getCode, Function.identity(), (a, b) -> a)));
        audit.enregistrer("AJUSTEMENT_PAIE", "bulletin_paie", bulletin.getId(), null, request.motif());
        return BulletinPaieDtos.BulletinDto.de(bulletin);
    }

    @Transactional
    public BulletinPaieDtos.PeriodeDto valider(Long idPeriode) {
        PeriodePaie periode = periode(idPeriode);
        if (periode.getStatut() != StatutPeriodePaie.CALCULEE
                && periode.getStatut() != StatutPeriodePaie.VARIABLES_CONSOLIDEES) {
            throw new BusinessException("PERIODE_NON_CALCULEE",
                    "Calculez la période avant de la valider.");
        }
        List<BulletinPaie> bulletinsPeriode = bulletins.findByIdPeriodeOrderByIdEmployeAsc(idPeriode);
        if (bulletinsPeriode.isEmpty()) {
            throw new BusinessException("PAIE_SANS_BULLETIN", "Aucun bulletin à valider pour cette période.");
        }
        List<Long> netsNegatifs = bulletinsPeriode.stream()
                .filter(b -> b.getSalaireNet().signum() < 0).map(BulletinPaie::getId).toList();
        if (!netsNegatifs.isEmpty()) {
            throw new BusinessException("NET_NEGATIF",
                    "La validation est bloquée : net négatif sur le bulletin " + netsNegatifs.getFirst() + ".");
        }
        for (BulletinPaie bulletin : bulletinsPeriode) {
            bulletin.setStatut(StatutBulletinPaie.VALIDE);
            bulletin.setDateValidation(LocalDateTime.now());
            if (bulletin.getNumero() == null) {
                bulletin.setNumero(references.suivante("BULLETIN_PAIE", "BUL", 6));
            }
            bulletins.save(bulletin);
        }
        periode.setStatut(StatutPeriodePaie.VALIDEE);
        audit.enregistrer("VALIDATION_PAIE", "periode_paie", periode.getId(), null,
                String.valueOf(bulletinsPeriode.size()));
        return BulletinPaieDtos.PeriodeDto.de(periodes.save(periode));
    }

    @Transactional
    public BulletinPaieDtos.PeriodeDto cloturer(Long idPeriode) {
        PeriodePaie periode = periode(idPeriode);
        if (periode.getStatut() != StatutPeriodePaie.VALIDEE
                && periode.getStatut() != StatutPeriodePaie.PAYEE) {
            throw new BusinessException("PERIODE_NON_VALIDEE", "Validez la période avant de la clôturer.");
        }
        List<BulletinPaie> bulletinsPeriode = bulletins.findByIdPeriodeOrderByIdEmployeAsc(idPeriode);
        for (BulletinPaie bulletin : bulletinsPeriode) {
            if (fiches.findByIdPeriodeAndIdEmploye(idPeriode, bulletin.getIdEmploye()).isPresent()) {
                continue;
            }
            BulletinPaieDtos.BulletinDetailDto detail = detail(bulletin.getId());
            String contenu;
            try {
                contenu = MAPPER.writeValueAsString(detail);
            } catch (Exception ex) {
                throw new BusinessException("FICHE_PAIE_SERIALISATION",
                        "Impossible de figer la fiche de paie : " + ex.getMessage());
            }
            FichePaie fiche = new FichePaie();
            fiche.setIdBulletin(bulletin.getId());
            fiche.setIdPeriode(periode.getId());
            fiche.setIdEmploye(bulletin.getIdEmploye());
            fiche.setMois(periode.getMois());
            fiche.setNumero(bulletin.getNumero());
            fiche.setNetAPayer(bulletin.getSalaireNet());
            fiche.setContenuJson(contenu);
            fiche.setHash(empreinte(contenu));
            fiches.save(fiche);
            if (fiche.getNetAPayer().compareTo(bulletin.getSalaireNet()) != 0) {
                throw new BusinessException("FICHE_PAIE_INCOHERENTE",
                        "Le net de la fiche ne correspond pas au bulletin.");
            }
        }
        periode.setStatut(StatutPeriodePaie.CLOTUREE);
        periode.setDateCloture(LocalDateTime.now());
        audit.enregistrer("CLOTURE_PAIE", "periode_paie", periode.getId(), null, VERSION_REGLES);
        return BulletinPaieDtos.PeriodeDto.de(periodes.save(periode));
    }

    @Transactional
    public BulletinPaieDtos.PeriodeDto changerStatut(Long idPeriode, String statut) {
        StatutPeriodePaie cible;
        try {
            cible = StatutPeriodePaie.valueOf(statut.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new BusinessException("STATUT_PAIE_INVALIDE", "Statut de période de paie invalide.");
        }
        return changerStatut(idPeriode, cible);
    }

    @Transactional
    public BulletinPaieDtos.PeriodeDto changerStatut(Long idPeriode, StatutPeriodePaie cible) {
        PeriodePaie periode = periode(idPeriode);
        StatutPeriodePaie actuel = periode.getStatut();
        boolean transition = (actuel == StatutPeriodePaie.VALIDEE && cible == StatutPeriodePaie.PUBLIEE)
                || (actuel == StatutPeriodePaie.PUBLIEE && cible == StatutPeriodePaie.PAYEE);
        if (!transition) {
            throw new BusinessException("TRANSITION_PAIE_INVALIDE",
                    "Transition non autorisée : " + actuel + " -> " + cible + ".");
        }
        periode.setStatut(cible);
        if (cible == StatutPeriodePaie.PAYEE) {
            periode.setDatePaiement(LocalDate.now());
        }
        audit.enregistrer("STATUT_PAIE", "periode_paie", periode.getId(), actuel.name(), cible.name());
        return BulletinPaieDtos.PeriodeDto.de(periodes.save(periode));
    }

    @Transactional(readOnly = true)
    public List<BulletinPaieDtos.FicheDto> fiches(Long idPeriode) {
        periode(idPeriode);
        return fiches.findByIdPeriodeOrderByIdEmployeAsc(idPeriode).stream()
                .filter(fiche -> peutLire(fiche.getIdEmploye()))
                .map(BulletinPaieDtos.FicheDto::de).toList();
    }

    @Transactional(readOnly = true)
    public List<BulletinPaieDtos.FicheDto> fichesEmploye(Long idEmploye) {
        verifierAccesFiche(idEmploye);
        return fiches.findByIdEmployeOrderByMoisDesc(idEmploye).stream()
                .map(BulletinPaieDtos.FicheDto::de).toList();
    }

    @Transactional(readOnly = true)
    public List<BulletinPaieDtos.FicheDto> mesFiches() {
        UtilisateurConnecte utilisateur = utilisateurCourant.exiger();
        Long idEmploye = employes.idParUtilisateur(utilisateur.getId())
                .orElseThrow(() -> BusinessException.notFound("Dossier employé introuvable pour ce compte."));
        return fiches.findByIdEmployeOrderByMoisDesc(idEmploye).stream()
                .map(BulletinPaieDtos.FicheDto::de).toList();
    }

    void calculerBulletin(BulletinPaie bulletin, BigDecimal salaireBase,
                                  TempsDtos.VariablePaieDto variable, BigDecimal joursNonPayes,
                                  ParametrePaie parametre, List<BaremeIrsa> tranches,
                                  Map<String, RubriquePaie> rubriquesParCode) {
        int joursPayes = BigDecimal.valueOf(30).subtract(joursNonPayes).max(BigDecimal.ZERO)
                .intValueExact();
        BigDecimal salairePeriode = arrondi(salaireBase.multiply(BigDecimal.valueOf(joursPayes))
                .divide(JOURS_MOIS, 8, RoundingMode.HALF_UP));
        BigDecimal tauxHoraire = salaireBase.divide(HEURES_MOIS, 6, RoundingMode.HALF_UP);
        BigDecimal hs30 = montantHeures(variable.minutesHs30(), tauxHoraire, parametre.getMajorationHs30());
        BigDecimal hs50 = montantHeures(variable.minutesHs50(), tauxHoraire, parametre.getMajorationHs50());
        BigDecimal nuit = montantHeures(variable.minutesNuit(), tauxHoraire, parametre.getMajorationNuit());
        BigDecimal dimanche = montantHeures(variable.minutesDimanche(), tauxHoraire,
                parametre.getMajorationDimanche());
        BigDecimal ferie = montantHeures(variable.minutesFerie(), tauxHoraire, parametre.getMajorationFerie());

        BigDecimal brut = salairePeriode.add(hs30).add(hs50).add(nuit).add(dimanche).add(ferie)
                .add(bulletin.getPrimeExceptionnelle());
        BigDecimal plafond = parametre.getSmig().multiply(parametre.getPlafondCnapsSmig());
        BigDecimal baseCnaps = brut.min(plafond);
        BigDecimal cnapsSal = pourcentage(baseCnaps, parametre.getTauxCnapsSalarial());
        BigDecimal ostieSal = pourcentage(brut, parametre.getTauxOstieSalarial());
        BigDecimal cotisationsSalariales = cnapsSal.add(ostieSal);
        BigDecimal baseImposable = brut.subtract(cotisationsSalariales).max(BigDecimal.ZERO);
        BigDecimal irsaBrut = irsa(baseImposable, tranches);
        BigDecimal reduction = parametre.getReductionCharge()
                .multiply(BigDecimal.valueOf(bulletin.getPersonnesCharge()));
        BigDecimal irsa = irsaBrut.subtract(reduction).max(BigDecimal.ZERO);
        if (irsaBrut.signum() > 0 && irsa.compareTo(parametre.getMinimumIrsa()) < 0) {
            irsa = parametre.getMinimumIrsa();
        }
        BigDecimal retenues = bulletin.getAvance().add(bulletin.getRetenueManuelle());
        BigDecimal net = brut.subtract(cotisationsSalariales).subtract(irsa).subtract(retenues);
        BigDecimal cnapsPat = pourcentage(baseCnaps, parametre.getTauxCnapsPatronal());
        BigDecimal ostiePat = pourcentage(brut, parametre.getTauxOstiePatronal());
        BigDecimal fmfp = pourcentage(brut, parametre.getTauxFmfpPatronal());

        bulletin.setJourPayes(joursPayes);
        bulletin.setSalaireBasePeriode(salairePeriode);
        bulletin.setSalaireBrut(brut);
        bulletin.setCotisationsSalariales(cotisationsSalariales);
        bulletin.setBaseImposable(baseImposable);
        bulletin.setImpot(irsa);
        bulletin.setRetenues(retenues);
        bulletin.setSalaireNet(net);
        bulletin.setCotisationsPatronales(cnapsPat.add(ostiePat).add(fmfp));
        bulletin.setStatut(StatutBulletinPaie.CALCULE);
        bulletins.save(bulletin);

        List<LigneBulletin> lignesCalculees = new ArrayList<>();
        int ordre = 0;
        lignesCalculees.add(ligne(bulletin, "SAL_BASE", salairePeriode, BigDecimal.ZERO,
                BigDecimal.valueOf(joursPayes), "salaire_base * jours_payes / 30", "POINTAGE,CONGE", ++ordre,
                rubriquesParCode));
        lignesCalculees.add(ligne(bulletin, "HS_30", hs30, BigDecimal.ZERO,
                BigDecimal.valueOf(variable.minutesHs30()), "minutes/60 * salaire/173.33 * (1 + 30%)", "POINTAGE", ++ordre,
                rubriquesParCode));
        lignesCalculees.add(ligne(bulletin, "HS_50", hs50, BigDecimal.ZERO,
                BigDecimal.valueOf(variable.minutesHs50()), "minutes/60 * salaire/173.33 * (1 + 50%)", "POINTAGE", ++ordre,
                rubriquesParCode));
        lignesCalculees.add(ligne(bulletin, "MAJ_NUIT", nuit, BigDecimal.ZERO,
                BigDecimal.valueOf(variable.minutesNuit()), "minutes/60 * salaire/173.33 * majoration_nuit", "POINTAGE", ++ordre,
                rubriquesParCode));
        lignesCalculees.add(ligne(bulletin, "MAJ_DIMANCHE", dimanche, BigDecimal.ZERO,
                BigDecimal.valueOf(variable.minutesDimanche()), "minutes/60 * salaire/173.33 * majoration_dimanche", "POINTAGE", ++ordre,
                rubriquesParCode));
        lignesCalculees.add(ligne(bulletin, "MAJ_FERIE", ferie, BigDecimal.ZERO,
                BigDecimal.valueOf(variable.minutesFerie()), "minutes/60 * salaire/173.33 * majoration_ferie", "POINTAGE", ++ordre,
                rubriquesParCode));
        lignesCalculees.add(ligne(bulletin, "PRIME", bulletin.getPrimeExceptionnelle(), BigDecimal.ZERO,
                BigDecimal.ONE, "prime_contractuelle + prime_exceptionnelle", "AJUSTEMENT", ++ordre,
                rubriquesParCode));
        lignesCalculees.add(ligne(bulletin, "CNAPS_SAL", cnapsSal, BigDecimal.ZERO, baseCnaps,
                "taux * min(brut, 8 * smig)", "PARAMETRE", ++ordre, rubriquesParCode));
        lignesCalculees.add(ligne(bulletin, "OSTIE_SAL", ostieSal, BigDecimal.ZERO, brut,
                "taux * brut", "PARAMETRE", ++ordre, rubriquesParCode));
        lignesCalculees.add(ligne(bulletin, "IRSA", irsa, BigDecimal.ZERO, baseImposable,
                "bareme(base_imposable) - reduction * personnes_charge", "BAREME_IRSA", ++ordre,
                rubriquesParCode));
        lignesCalculees.add(ligne(bulletin, "AVANCE", bulletin.getAvance(), BigDecimal.ZERO, bulletin.getAvance(),
                "avance", "AJUSTEMENT", ++ordre, rubriquesParCode));
        lignesCalculees.add(ligne(bulletin, "CNAPS_PAT", BigDecimal.ZERO, cnapsPat, baseCnaps,
                "taux_patronal * min(brut, 8 * smig)", "PARAMETRE", ++ordre, rubriquesParCode));
        lignesCalculees.add(ligne(bulletin, "OSTIE_PAT", BigDecimal.ZERO, ostiePat, brut,
                "taux_patronal * brut", "PARAMETRE", ++ordre, rubriquesParCode));
        lignesCalculees.add(ligne(bulletin, "FMFP", BigDecimal.ZERO, fmfp, brut,
                "taux_patronal * brut", "PARAMETRE", ++ordre, rubriquesParCode));

        lignes.deleteByIdBulletin(bulletin.getId());
        lignes.saveAll(lignesCalculees);
    }

    private LigneBulletin ligne(BulletinPaie bulletin, String code, BigDecimal salarial,
                                BigDecimal patronal, BigDecimal quantite, String formule,
                                String sources, int ordre, Map<String, RubriquePaie> rubriquesParCode) {
        RubriquePaie rubrique = rubriquesParCode.get(code);
        LigneBulletin ligne = new LigneBulletin();
        ligne.setIdBulletin(bulletin.getId());
        ligne.setCodeRubrique(code);
        ligne.setLibelle(rubrique == null ? code : rubrique.getLibelle());
        ligne.setOrdre(ordre);
        ligne.setBase(quantite);
        ligne.setTaux(BigDecimal.ZERO);
        ligne.setQuantite(quantite);
        ligne.setMontantSalarial(salarial);
        ligne.setMontantPatronal(patronal);
        ligne.setFormuleAppliquee(rubrique == null || rubrique.getFormule() == null ? formule
                : rubrique.getFormule());
        ligne.setVersionRegle(VERSION_REGLES);
        ligne.setSources(sources);
        return ligne;
    }

    private Map<Long, BigDecimal> joursNonPayes(List<EmployeDto.Actif> actifs, LocalDate debutMois) {
        LocalDate finMois = debutMois.with(TemporalAdjusters.lastDayOfMonth());
        Map<Long, BigDecimal> resultat = new HashMap<>();
        for (PaieDtos.VariableCongeDto variable : variablesConges.variables(debutMois, finMois)) {
            String type = variable.typeConge() == null ? "" : variable.typeConge().toUpperCase(Locale.ROOT);
            if (type.contains("SANS_SOLDE")) {
                resultat.merge(variable.idEmploye(), variable.joursConges(), BigDecimal::add);
            }
        }
        actifs.forEach(actif -> resultat.putIfAbsent(actif.id(), BigDecimal.ZERO));
        return resultat;
    }

    private BigDecimal irsa(BigDecimal base, List<BaremeIrsa> tranches) {
        BigDecimal impot = BigDecimal.ZERO;
        for (BaremeIrsa tranche : tranches) {
            if (base.compareTo(tranche.getBorneInf()) <= 0) {
                continue;
            }
            BigDecimal borneSup = tranche.getBorneSup();
            BigDecimal imposable = borneSup == null || base.compareTo(borneSup) > 0
                    ? borneSup == null ? base : borneSup
                    : base;
            BigDecimal part = imposable.subtract(tranche.getBorneInf()).max(BigDecimal.ZERO);
            impot = impot.add(pourcentage(part, tranche.getTaux()));
        }
        return arrondi(impot);
    }

    private BigDecimal montantHeures(int minutes, BigDecimal tauxHoraire, BigDecimal majoration) {
        if (minutes <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal majorationFactor = BigDecimal.ONE.add(majoration.divide(CENT, 6, RoundingMode.HALF_UP));
        return arrondi(BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 6, RoundingMode.HALF_UP)
                .multiply(tauxHoraire).multiply(majorationFactor));
    }

    private BigDecimal pourcentage(BigDecimal base, BigDecimal taux) {
        if (taux == null || taux.signum() == 0 || base == null) {
            return BigDecimal.ZERO;
        }
        return arrondi(base.multiply(taux).divide(CENT, 8, RoundingMode.HALF_UP));
    }

    private BigDecimal arrondi(BigDecimal valeur) {
        return valeur.setScale(2, RoundingMode.HALF_UP);
    }

    private <T> BigDecimal coalesce(BigDecimal valeur, T precedent, Function<T, BigDecimal> extracteur,
                                    BigDecimal defaut) {
        if (valeur != null) {
            return valeur;
        }
        if (precedent != null && extracteur.apply(precedent) != null) {
            return extracteur.apply(precedent);
        }
        return defaut;
    }

    private PeriodePaie periode(Long idPeriode) {
        return periodes.findById(idPeriode)
                .orElseThrow(() -> BusinessException.notFound("Période de paie introuvable."));
    }

    private BulletinPaie bulletin(Long idBulletin) {
        return bulletins.findById(idBulletin)
                .orElseThrow(() -> BusinessException.notFound("Bulletin de paie introuvable."));
    }

    private void verifierModifiable(PeriodePaie periode) {
        if (periode.getStatut() == StatutPeriodePaie.CLOTUREE
                || periode.getStatut() == StatutPeriodePaie.VALIDEE
                || periode.getStatut() == StatutPeriodePaie.PUBLIEE
                || periode.getStatut() == StatutPeriodePaie.PAYEE) {
            throw new BusinessException("PERIODE_NON_MODIFIABLE",
                    "Cette période de paie est verrouillée (" + periode.getStatut() + ").");
        }
    }

    private boolean peutLire(Long idEmploye) {
        UtilisateurConnecte utilisateur = utilisateurCourant.optionnel();
        if (utilisateur == null) {
            return false;
        }
        String role = utilisateur.getRoleNom();
        if (Roles.RH.equals(role) || Roles.FINANCE.equals(role) || Roles.DIRECTION.equals(role)) {
            return true;
        }
        if (!Roles.EMPLOYE.equals(role)) {
            return false;
        }
        return employes.idParUtilisateur(utilisateur.getId())
                .map(idEmploye::equals).orElse(false);
    }

    private void verifierAccesFiche(Long idEmploye) {
        if (!peutLire(idEmploye)) {
            throw BusinessException.forbidden("Vous n'avez pas accès à cette fiche de paie.");
        }
    }

    private String empreinte(String contenu) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(contenu.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new BusinessException("HASH_INDISPONIBLE",
                    "Empreinte SHA-256 indisponible : " + ex.getMessage());
        }
    }
}
