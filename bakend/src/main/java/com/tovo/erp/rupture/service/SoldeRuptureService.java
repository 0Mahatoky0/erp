package com.tovo.erp.rupture.service;

import com.tovo.erp.contrat.dto.ContratDtos;
import com.tovo.erp.contrat.service.ContratService;
import com.tovo.erp.employe.dto.EmployeEmbaucheDto;
import com.tovo.erp.employe.service.EmployeService;
import com.tovo.erp.paie.dto.BulletinPaieDtos;
import com.tovo.erp.paie.service.BulletinPaieService;
import com.tovo.erp.rupture.dto.RuptureDtos;
import com.tovo.erp.rupture.model.CategorieLigneSolde;
import com.tovo.erp.rupture.model.DossierRupture;
import com.tovo.erp.rupture.model.DroitsRupture;
import com.tovo.erp.rupture.model.LigneSoldeRupture;
import com.tovo.erp.rupture.model.MaterielRupture;
import com.tovo.erp.rupture.model.MotifRupture;
import com.tovo.erp.rupture.model.ModePreavis;
import com.tovo.erp.rupture.model.SoldeToutCompte;
import com.tovo.erp.rupture.model.StatutDossierRupture;
import com.tovo.erp.rupture.model.StatutMaterielRupture;
import com.tovo.erp.rupture.model.StatutSoldeRupture;
import com.tovo.erp.rupture.repository.DossierRuptureRepository;
import com.tovo.erp.rupture.repository.DroitsRuptureRepository;
import com.tovo.erp.rupture.repository.LigneSoldeRuptureRepository;
import com.tovo.erp.rupture.repository.MaterielRuptureRepository;
import com.tovo.erp.rupture.repository.SoldeToutCompteRepository;
import com.tovo.erp.shared.BusinessException;
import com.tovo.erp.shared.Roles;
import com.tovo.erp.shared.UtilisateurConnecte;
import com.tovo.erp.shared.UtilisateurCourant;
import com.tovo.erp.shared.audit.JournalAuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class SoldeRuptureService {

    private static final int JOURS_PAR_MOIS = 30;

    private final SoldeToutCompteRepository soldes;
    private final LigneSoldeRuptureRepository lignes;
    private final DossierRuptureRepository dossiers;
    private final DroitsRuptureRepository droitsRepository;
    private final MaterielRuptureRepository materiels;
    private final ContratService contrats;
    private final EmployeService employes;
    private final BulletinPaieService bulletins;
    private final RuptureProperties properties;
    private final UtilisateurCourant utilisateurCourant;
    private final JournalAuditService audit;

    public SoldeRuptureService(SoldeToutCompteRepository soldes,
                               LigneSoldeRuptureRepository lignes,
                               DossierRuptureRepository dossiers,
                               DroitsRuptureRepository droitsRepository,
                               MaterielRuptureRepository materiels,
                                ContratService contrats,
                                EmployeService employes,
                                BulletinPaieService bulletins,
                                RuptureProperties properties,
                               UtilisateurCourant utilisateurCourant,
                               JournalAuditService audit) {
        this.soldes = soldes;
        this.lignes = lignes;
        this.dossiers = dossiers;
        this.droitsRepository = droitsRepository;
        this.materiels = materiels;
        this.contrats = contrats;
        this.employes = employes;
        this.bulletins = bulletins;
        this.properties = properties;
        this.utilisateurCourant = utilisateurCourant;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public RuptureDtos.SoldeDto soldeDto(Long idDossier) {
        SoldeToutCompte solde = soldes.findByIdDossier(idDossier).orElse(null);
        if (solde == null) {
            return null;
        }
        return RuptureDtos.SoldeDto.de(solde, lignes.findByIdSoldeOrderByIdAsc(solde.getId()).stream()
                .map(RuptureDtos.LigneDto::de).toList());
    }

    @Transactional
    public RuptureDtos.SoldeDto preparer(Long idDossier) {
        utilisateurCourant.exigerRole(Roles.FINANCE, Roles.RH, Roles.DIRECTION);
        DossierRupture dossier = dossier(idDossier);
        if (dossier.getDateSortie() == null) {
            throw new BusinessException("RUPTURE_DATE_SORTIE_MANQUANTE",
                    "La date de sortie doit etre notifiee avant de preparer le solde.");
        }
        SoldeToutCompte existant = soldes.findByIdDossier(idDossier).orElse(null);
        if (existant != null && (existant.getStatut() == StatutSoldeRupture.VALIDE
                || existant.getStatut() == StatutSoldeRupture.PAYE)) {
            throw new BusinessException("SOLDE_IMMUTABLE",
                    "Un solde valide ou paye ne peut plus etre recalcule.");
        }
        DroitsRupture droits = droitsRepository.findByIdDossier(idDossier)
                .orElseThrow(() -> BusinessException.notFound(
                        "Les droits de sortie doivent d'abord etre arretes."));
        ContratDtos.ContratDto contrat = contrats.contrat(dossier.getIdContrat());
        EmployeEmbaucheDto employe = employes.dossier(dossier.getIdEmploye());
        BigDecimal salaire = contrat.salaireBase() == null ? BigDecimal.ZERO : contrat.salaireBase();
        BigDecimal journalier = salaire.divide(BigDecimal.valueOf(JOURS_PAR_MOIS), 2, RoundingMode.HALF_UP);
        int joursTravailles = Math.min(dossier.getDateSortie().getDayOfMonth(), JOURS_PAR_MOIS);
        BigDecimal salaireDu = journalier.multiply(BigDecimal.valueOf(joursTravailles))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal indemnitePreavis = calculerIndemnitePreavis(dossier, journalier);
        BigDecimal indemniteLicenciement = calculerIndemniteLicenciement(dossier, employe, journalier);
        BigDecimal indemniteConges = journalier.multiply(nvl(droits.getCongeSolde()))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal primes = nvl(droits.getPrimesDues());
        BigDecimal retenuesAvances = nvl(droits.getAvancesRestantes());
        BigDecimal retenuesDedit = nvl(droits.getDeditFormation());
        BigDecimal retenuesMateriel = retenuesMateriel(idDossier);

        BigDecimal totalBrut = salaireDu.add(indemnitePreavis).add(indemniteLicenciement)
                .add(indemniteConges).add(primes);
        BigDecimal totalRetenues = retenuesAvances.add(retenuesDedit).add(retenuesMateriel);

        SoldeToutCompte solde = existant == null ? new SoldeToutCompte() : existant;
        solde.setIdDossier(idDossier);
        solde.setSalaireDu(salaireDu);
        solde.setIndemnitePreavis(indemnitePreavis);
        solde.setIndemniteLicenciement(indemniteLicenciement);
        solde.setIndemniteConges(indemniteConges);
        solde.setPrimes(primes);
        solde.setRetenuesAvances(retenuesAvances);
        solde.setRetenuesDedit(retenuesDedit);
        solde.setRetenuesMateriel(retenuesMateriel);
        solde.setTotalBrut(totalBrut);
        solde.setTotalRetenues(totalRetenues);
        solde.setNetAvantCotisations(totalBrut.subtract(totalRetenues));
        solde.setDevise(contrat.devise() == null ? "MGA" : contrat.devise());
        solde.setStatut(StatutSoldeRupture.EN_ATTENTE_VALIDATION);
        solde.setDatePreparation(LocalDateTime.now());
        solde.setIdCalculateur(utilisateurCourant.exiger().getId());
        solde.setDateValidation(null);
        solde.setIdValidateur(null);
        solde.setMotifRefus(null);
        solde.setDatePaiement(null);
        solde.setReferencePaiement(null);
        solde.setModePaiement(null);
        SoldeToutCompte enregistre = soldes.save(solde);

        lignes.deleteByIdSolde(enregistre.getId());
        List<LigneSoldeRupture> nouvelles = new ArrayList<>();
        nouvelles.add(ligne(enregistre, "SALAIRE_DU", "Salaire du", CategorieLigneSolde.GAIN,
                salaire, BigDecimal.valueOf(joursTravailles), journalier, salaireDu, "bloc8.salaire_base"));
        if (indemnitePreavis.signum() > 0) {
            nouvelles.add(ligne(enregistre, "INDEMNITE_PREAVIS", "Indemnite de preavis",
                    CategorieLigneSolde.GAIN, salaire, BigDecimal.valueOf(dossier.getPreavisJours()),
                    journalier, indemnitePreavis, "bloc12.regle_preavis"));
        }
        if (indemniteLicenciement.signum() > 0) {
            nouvelles.add(ligne(enregistre, "INDEMNITE_LICENCIEMENT", "Indemnite de licenciement",
                    CategorieLigneSolde.GAIN, salaire, BigDecimal.valueOf(anneesAnciennete(employe, dossier)),
                    journalier.multiply(BigDecimal.valueOf(properties.indemniteJoursParAnnee())),
                    indemniteLicenciement, "bloc12.barème"));
        }
        if (indemniteConges.signum() > 0) {
            nouvelles.add(ligne(enregistre, "INDEMNITE_CONGES", "Conges non pris",
                    CategorieLigneSolde.GAIN, journalier, nvl(droits.getCongeSolde()), journalier,
                    indemniteConges, "conges.soldes"));
        }
        if (primes.signum() > 0) {
            nouvelles.add(ligne(enregistre, "PRIMES", "Primes dues", CategorieLigneSolde.GAIN,
                    primes, BigDecimal.ONE, BigDecimal.ONE, primes, "contrat.primes"));
        }
        if (retenuesAvances.signum() > 0) {
            nouvelles.add(ligne(enregistre, "AVANCES", "Avances sur salaire", CategorieLigneSolde.RETENUE,
                    retenuesAvances, BigDecimal.ONE, BigDecimal.ONE, retenuesAvances, "bloc8.avances"));
        }
        if (retenuesDedit.signum() > 0) {
            nouvelles.add(ligne(enregistre, "DEDIT_FORMATION", "Dedit de formation", CategorieLigneSolde.RETENUE,
                    retenuesDedit, BigDecimal.ONE, BigDecimal.ONE, retenuesDedit, "evaluation.convention"));
        }
        if (retenuesMateriel.signum() > 0) {
            nouvelles.add(ligne(enregistre, "MATERIEL", "Materiel non restitue", CategorieLigneSolde.RETENUE,
                    retenuesMateriel, BigDecimal.ONE, BigDecimal.ONE, retenuesMateriel, "rupture.materiel"));
        }
        lignes.saveAll(nouvelles);

        dossier.setStatut(StatutDossierRupture.SOLDE_EN_PREPARATION);
        dossiers.save(dossier);
        audit.enregistrer("SOLDE_PREPARE", "dossier_rupture", idDossier, null,
                "net=" + enregistre.getNetAvantCotisations());
        return soldeDto(idDossier);
    }

    @Transactional
    public RuptureDtos.SoldeDto valider(Long idDossier, RuptureDtos.DecisionSolde decision) {
        UtilisateurConnecte acteur = utilisateurCourant.exiger();
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION);
        DossierRupture dossier = dossier(idDossier);
        SoldeToutCompte solde = soldes.findByIdDossier(idDossier)
                .orElseThrow(() -> BusinessException.notFound("Solde de tout compte introuvable."));
        if (solde.getStatut() != StatutSoldeRupture.EN_ATTENTE_VALIDATION) {
            throw new BusinessException("SOLDE_STATUT_INVALIDE",
                    "Seul un solde en attente de validation peut etre valide.");
        }
        if (solde.getIdCalculateur() != null && solde.getIdCalculateur().equals(acteur.getId())) {
            throw new BusinessException("SEPARATION_TACHES",
                    "Le calcul et la validation du solde doivent etre effectues par deux personnes distinctes.");
        }
        boolean refuse = decision != null && decision.motifRefus() != null
                && !decision.motifRefus().isBlank();
        if (refuse) {
            solde.setStatut(StatutSoldeRupture.REFUSE);
            solde.setMotifRefus(decision.motifRefus());
            dossier.setStatut(StatutDossierRupture.DROITS_ARRETES);
            audit.enregistrer("SOLDE_REFUSE", "dossier_rupture", idDossier,
                    null, decision.motifRefus());
        } else {
            solde.setStatut(StatutSoldeRupture.VALIDE);
            solde.setIdValidateur(acteur.getId());
            solde.setDateValidation(LocalDateTime.now());
            dossier.setStatut(StatutDossierRupture.SOLDE_VALIDE);
            audit.enregistrer("SOLDE_VALIDE", "dossier_rupture", idDossier,
                    null, "validateur=" + acteur.getId());
        }
        soldes.save(solde);
        dossiers.save(dossier);
        return soldeDto(idDossier);
    }

    @Transactional
    public RuptureDtos.SoldeDto payer(Long idDossier, RuptureDtos.PaiementSolde demande) {
        utilisateurCourant.exigerRole(Roles.FINANCE, Roles.DIRECTION);
        if (demande == null || demande.referencePaiement() == null || demande.referencePaiement().isBlank()) {
            throw new BusinessException("PAIEMENT_REFERENCE_MANQUANTE",
                    "La reference de paiement est obligatoire.");
        }
        DossierRupture dossier = dossier(idDossier);
        SoldeToutCompte solde = soldes.findByIdDossier(idDossier)
                .orElseThrow(() -> BusinessException.notFound("Solde de tout compte introuvable."));
        if (solde.getStatut() != StatutSoldeRupture.VALIDE) {
            throw new BusinessException("SOLDE_NON_VALIDE",
                    "Le solde doit etre valide avant d'etre paye.");
        }
        solde.setStatut(StatutSoldeRupture.PAYE);
        solde.setMontantPaye(solde.getNetAvantCotisations());
        solde.setDatePaiement(LocalDate.now());
        solde.setReferencePaiement(demande.referencePaiement());
        solde.setModePaiement(demande.modePaiement());
        if (solde.getIdBulletinPaie() == null) {
            BulletinPaieDtos.BulletinDto bulletin = bulletins.genererBulletinSolde(
                    demandeBulletinSolde(dossier, solde, demande));
            solde.setIdBulletinPaie(bulletin.id());
        }
        soldes.save(solde);
        dossier.setStatut(StatutDossierRupture.SOLDE_PAYE);
        dossiers.save(dossier);
        audit.enregistrer("SOLDE_PAYE", "dossier_rupture", idDossier, null,
                demande.referencePaiement());
        return soldeDto(idDossier);
    }

    @Transactional(readOnly = true)
    public DossierRupture dossier(Long idDossier) {
        return dossiers.findById(idDossier)
                .orElseThrow(() -> BusinessException.notFound("Dossier de rupture introuvable."));
    }

    private BigDecimal calculerIndemnitePreavis(DossierRupture dossier, BigDecimal journalier) {
        if (dossier.getPreavisMode() != ModePreavis.DISPENSE_EMPLOYEUR) {
            return BigDecimal.ZERO.setScale(2);
        }
        return journalier.multiply(BigDecimal.valueOf(Math.max(dossier.getPreavisJours(), 0)))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculerIndemniteLicenciement(DossierRupture dossier,
                                                     EmployeEmbaucheDto employe,
                                                     BigDecimal journalier) {
        MotifRupture motif = dossier.getMotif();
        if (motif == null || !motif.name().startsWith("LICENCIEMENT")) {
            return BigDecimal.ZERO.setScale(2);
        }
        int annees = anneesAnciennete(employe, dossier);
        return journalier.multiply(BigDecimal.valueOf(annees))
                .multiply(BigDecimal.valueOf(properties.indemniteJoursParAnnee()))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private int anneesAnciennete(EmployeEmbaucheDto employe, DossierRupture dossier) {
        LocalDate debut = employe.datePremiereEmbauche();
        if (debut == null) {
            debut = employe.dateEmbauche();
        }
        if (debut == null || dossier.getDateSortie() == null || debut.isAfter(dossier.getDateSortie())) {
            return 0;
        }
        return (int) ChronoUnit.YEARS.between(debut, dossier.getDateSortie());
    }

    private BigDecimal retenuesMateriel(Long idDossier) {
        return materiels.findByIdDossierOrderByIdAsc(idDossier).stream()
                .filter(m -> m.getStatut() == StatutMaterielRupture.RETENU
                        || m.getStatut() == StatutMaterielRupture.PERDU)
                .map(MaterielRupture::getMontantRetenue)
                .filter(montant -> montant != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private LigneSoldeRupture ligne(SoldeToutCompte solde, String code, String libelle,
                                    CategorieLigneSolde categorie, BigDecimal base,
                                    BigDecimal quantite, BigDecimal taux, BigDecimal montant,
                                    String source) {
        LigneSoldeRupture ligne = new LigneSoldeRupture();
        ligne.setIdSolde(solde.getId());
        ligne.setCode(code);
        ligne.setLibelle(libelle);
        ligne.setCategorie(categorie);
        ligne.setBase(base);
        ligne.setQuantite(quantite);
        ligne.setTaux(taux);
        ligne.setMontant(montant);
        ligne.setSource(source);
        return ligne;
    }

    private BulletinPaieDtos.SoldePaieRequest demandeBulletinSolde(DossierRupture dossier,
                                                                   SoldeToutCompte solde,
                                                                   RuptureDtos.PaiementSolde paiement) {
        LocalDate dateSortie = dossier.getDateSortie() == null ? LocalDate.now() : dossier.getDateSortie();
        ContratDtos.ContratDto contrat = contrats.contrat(dossier.getIdContrat());
        BigDecimal salaireBase = contrat == null || contrat.salaireBase() == null
                ? BigDecimal.ZERO.setScale(2) : contrat.salaireBase();
        List<BulletinPaieDtos.SoldePaieLigneDto> lignesBulletin = lignes
                .findByIdSoldeOrderByIdAsc(solde.getId()).stream()
                .map(ligne -> new BulletinPaieDtos.SoldePaieLigneDto(
                        ligne.getCode(),
                        ligne.getLibelle(),
                        ligne.getCategorie() == null ? null : ligne.getCategorie().name(),
                        ligne.getBase(),
                        ligne.getQuantite(),
                        ligne.getTaux(),
                        nvl(ligne.getMontant()),
                        "rupture." + ligne.getCode(),
                        ligne.getSource()))
                .toList();
        return new BulletinPaieDtos.SoldePaieRequest(
                dossier.getIdEmploye(),
                dossier.getIdContrat(),
                dateSortie.withDayOfMonth(1),
                Math.min(dateSortie.getDayOfMonth(), JOURS_PAR_MOIS),
                salaireBase,
                nvl(solde.getTotalBrut()),
                nvl(solde.getTotalRetenues()),
                nvl(solde.getNetAvantCotisations()),
                dossier.getReference() == null ? "RUP-" + dossier.getId() : dossier.getReference(),
                paiement.referencePaiement(),
                paiement.modePaiement(),
                lignesBulletin);
    }

    private BigDecimal nvl(BigDecimal valeur) {
        return valeur == null ? BigDecimal.ZERO.setScale(2) : valeur;
    }
}
