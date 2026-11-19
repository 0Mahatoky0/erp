package com.tovo.erp.rupture.service;

import com.tovo.erp.besoin.dto.PosteDto;
import com.tovo.erp.besoin.service.DemandeService;
import com.tovo.erp.besoin.service.PosteService;
import com.tovo.erp.contrat.dto.ContratDtos;
import com.tovo.erp.contrat.service.CarriereService;
import com.tovo.erp.contrat.service.ContratService;
import com.tovo.erp.conges.dto.CongeDtos;
import com.tovo.erp.conges.service.CongeService;
import com.tovo.erp.discipline.service.ProcedureService;
import com.tovo.erp.employe.dto.DocumentEmbaucheDto;
import com.tovo.erp.employe.dto.EmployeEmbaucheDto;
import com.tovo.erp.employe.service.EmployeService;
import com.tovo.erp.employe.service.MissionService;
import com.tovo.erp.evaluation.dto.EvaluationDtos;
import com.tovo.erp.evaluation.service.FormationService;
import com.tovo.erp.iam.service.UtilisateurService;
import com.tovo.erp.rupture.dto.RuptureDtos;
import com.tovo.erp.rupture.model.DocumentRupture;
import com.tovo.erp.rupture.model.DossierRupture;
import com.tovo.erp.rupture.model.DroitsRupture;
import com.tovo.erp.rupture.model.EntretienRupture;
import com.tovo.erp.rupture.model.InitiateurRupture;
import com.tovo.erp.rupture.model.MaterielRupture;
import com.tovo.erp.rupture.model.ModePreavis;
import com.tovo.erp.rupture.model.MotifRupture;
import com.tovo.erp.rupture.model.SoldeToutCompte;
import com.tovo.erp.rupture.model.StatutDossierRupture;
import com.tovo.erp.rupture.model.StatutMaterielRupture;
import com.tovo.erp.rupture.model.StatutTacheRupture;
import com.tovo.erp.rupture.model.TacheDepart;
import com.tovo.erp.rupture.repository.DocumentRuptureRepository;
import com.tovo.erp.rupture.repository.DossierRuptureRepository;
import com.tovo.erp.rupture.repository.DroitsRuptureRepository;
import com.tovo.erp.rupture.repository.EntretienRuptureRepository;
import com.tovo.erp.rupture.repository.MaterielRuptureRepository;
import com.tovo.erp.rupture.repository.SoldeToutCompteRepository;
import com.tovo.erp.rupture.repository.TacheDepartRepository;
import com.tovo.erp.shared.BusinessException;
import com.tovo.erp.shared.Roles;
import com.tovo.erp.shared.UtilisateurConnecte;
import com.tovo.erp.shared.UtilisateurCourant;
import com.tovo.erp.shared.audit.JournalAuditService;
import com.tovo.erp.shared.reference.ReferenceService;
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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DossierRuptureService {

    private static final Set<StatutDossierRupture> STATUTS_OUVERTS = Set.of(
            StatutDossierRupture.INITIEE, StatutDossierRupture.EN_VALIDATION,
            StatutDossierRupture.NOTIFIEE, StatutDossierRupture.EN_PREAVIS,
            StatutDossierRupture.DROITS_ARRETES, StatutDossierRupture.SOLDE_EN_PREPARATION,
            StatutDossierRupture.SOLDE_VALIDE, StatutDossierRupture.SOLDE_PAYE,
            StatutDossierRupture.DOCUMENTS_REMIS);
    private static final Set<StatutDossierRupture> STATUTS_CLOTURES = Set.of(
            StatutDossierRupture.CLOTUREE, StatutDossierRupture.ARCHIVEE,
            StatutDossierRupture.ANNULEE);

    private final DossierRuptureRepository dossiers;
    private final DroitsRuptureRepository droitsRepository;
    private final SoldeToutCompteRepository soldes;
    private final TacheDepartRepository taches;
    private final MaterielRuptureRepository materiels;
    private final DocumentRuptureRepository documents;
    private final EntretienRuptureRepository entretiens;
    private final ReferenceService references;
    private final ContratService contrats;
    private final CarriereService carrieres;
    private final EmployeService employes;
    private final ProcedureService procedures;
    private final CongeService conges;
    private final TempsService temps;
    private final MissionService missions;
    private final DemandeService demandes;
    private final PosteService postes;
    private final FormationService formations;
    private final UtilisateurService utilisateurs;
    private final UtilisateurCourant utilisateurCourant;
    private final JournalAuditService audit;
    private final RuptureProperties properties;
    private final SoldeRuptureService soldeService;

    public DossierRuptureService(DossierRuptureRepository dossiers,
                                 DroitsRuptureRepository droitsRepository,
                                 SoldeToutCompteRepository soldes,
                                 TacheDepartRepository taches,
                                 MaterielRuptureRepository materiels,
                                 DocumentRuptureRepository documents,
                                 EntretienRuptureRepository entretiens,
                                 ReferenceService references,
                                 ContratService contrats,
                                 CarriereService carrieres,
                                 EmployeService employes,
                                 ProcedureService procedures,
                                 CongeService conges,
                                 TempsService temps,
                                 MissionService missions,
                                 DemandeService demandes,
                                 PosteService postes,
                                 FormationService formations,
                                 UtilisateurService utilisateurs,
                                 UtilisateurCourant utilisateurCourant,
                                 JournalAuditService audit,
                                 RuptureProperties properties,
                                 SoldeRuptureService soldeService) {
        this.dossiers = dossiers;
        this.droitsRepository = droitsRepository;
        this.soldes = soldes;
        this.taches = taches;
        this.materiels = materiels;
        this.documents = documents;
        this.entretiens = entretiens;
        this.references = references;
        this.contrats = contrats;
        this.carrieres = carrieres;
        this.employes = employes;
        this.procedures = procedures;
        this.conges = conges;
        this.temps = temps;
        this.missions = missions;
        this.demandes = demandes;
        this.postes = postes;
        this.formations = formations;
        this.utilisateurs = utilisateurs;
        this.utilisateurCourant = utilisateurCourant;
        this.audit = audit;
        this.properties = properties;
        this.soldeService = soldeService;
    }

    @Transactional(readOnly = true)
    public List<RuptureDtos.DossierDto> lister(String statut, String motif, String recherche) {
        Map<Long, String> nomsEmployes = employes.dossiersParIds(
                dossiers.findAll().stream().map(DossierRupture::getIdEmploye).toList()).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> nom(e.getValue().prenom(), e.getValue().nom())));
        Map<Long, String> numerosContrats = contrats.lister(null).stream()
                .collect(Collectors.toMap(ContratDtos.ContratDto::id,
                        ContratDtos.ContratDto::numero, (a, b) -> a));
        Map<Long, BigDecimal> nets = soldes.findAll().stream()
                .filter(s -> s.getIdDossier() != null)
                .collect(Collectors.toMap(SoldeToutCompte::getIdDossier,
                        SoldeToutCompte::getNetAvantCotisations, (a, b) -> a));
        String filtre = recherche == null ? "" : recherche.trim().toUpperCase(Locale.ROOT);
        return dossiers.findAllByOrderByDateCreationDesc().stream()
                .filter(d -> statut == null || statut.isBlank()
                        || d.getStatut().name().equalsIgnoreCase(statut))
                .filter(d -> motif == null || motif.isBlank()
                        || d.getMotif().name().equalsIgnoreCase(motif))
                .filter(d -> filtre.isBlank()
                        || d.getReference().toUpperCase(Locale.ROOT).contains(filtre)
                        || nomsEmployes.getOrDefault(d.getIdEmploye(), "").toUpperCase(Locale.ROOT).contains(filtre)
                        || numerosContrats.getOrDefault(d.getIdContrat(), "")
                                .toUpperCase(Locale.ROOT).contains(filtre))
                .map(d -> RuptureDtos.DossierDto.de(d,
                        nomsEmployes.get(d.getIdEmploye()), numerosContrats.get(d.getIdContrat()),
                        nets.get(d.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detail(Long idDossier) {
        DossierRupture dossier = dossier(idDossier);
        EmployeEmbaucheDto employe = employes.dossier(dossier.getIdEmploye());
        ContratDtos.ContratDto contrat = contrats.contrat(dossier.getIdContrat());
        RuptureDtos.SoldeDto soldeDto = soldeService.soldeDto(idDossier);
        Map<String, Object> resultat = new LinkedHashMap<>();
        resultat.put("id", dossier.getId());
        resultat.put("reference", dossier.getReference());
        resultat.put("statut", dossier.getStatut().name());
        resultat.put("motif", dossier.getMotif().name());
        resultat.put("dateSortie", dossier.getDateSortie());
        resultat.put("dossier", RuptureDtos.DossierDto.de(dossier,
                nom(employe.prenom(), employe.nom()), contrat.numero(),
                soldeDto == null ? null : soldeDto.netAvantCotisations()));
        DroitsRupture droits = droitsRepository.findByIdDossier(idDossier).orElse(null);
        resultat.put("droits", droits == null ? null : RuptureDtos.DroitsDto.de(droits));
        resultat.put("solde", soldeDto);
        resultat.put("taches", taches.findByIdDossierOrderByIdAsc(idDossier).stream()
                .map(RuptureDtos.TacheDto::de).toList());
        resultat.put("materiels", materiels.findByIdDossierOrderByIdAsc(idDossier).stream()
                .map(RuptureDtos.MaterielDto::de).toList());
        resultat.put("documents", documents.findByIdDossierOrderByIdAsc(idDossier).stream()
                .map(RuptureDtos.DocumentDto::de).toList());
        resultat.put("entretien", entretiens.findByIdDossier(idDossier)
                .map(RuptureDtos.EntretienDto::de).orElse(null));
        return resultat;
    }

    @Transactional
    public RuptureDtos.DossierDto creer(RuptureDtos.CreationDossier demande) {
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION, Roles.MANAGER);
        if (demande == null || demande.idContrat() == null || demande.motif() == null
                || demande.initiateur() == null || demande.dateNotification() == null) {
            throw new BusinessException("RUPTURE_INCOMPLETE",
                    "Contrat, motif, initiateur et date de notification sont obligatoires.");
        }
        if (demande.motif() == MotifRupture.AUTRE
                && (demande.motifAutre() == null || demande.motifAutre().isBlank())) {
            throw new BusinessException("RUPTURE_MOTIF_AUTRE",
                    "Le motif precise est obligatoire pour un motif AUTRE.");
        }
        ContratDtos.ContratDto contrat = contrats.contrat(demande.idContrat());
        if (!"EN_COURS".equals(String.valueOf(contrat.statutExecution()))) {
            throw new BusinessException("CONTRAT_NON_ACTIF",
                    "Seul un contrat en cours peut faire l'objet d'une rupture.");
        }
        if (dossiers.existsByIdEmployeAndStatutNotIn(contrat.idEmploye(), STATUTS_CLOTURES)) {
            throw new BusinessException("RUPTURE_DEJA_EN_COURS",
                    "Un autre dossier de rupture est encore ouvert pour cet employe.");
        }
        if (demande.motif() == MotifRupture.LICENCIEMENT_DISCIPLINAIRE) {
            if (demande.idProcedureDisciplinaire() == null
                    || !procedures.licenciementDecide(demande.idProcedureDisciplinaire(),
                            contrat.idEmploye())) {
                throw new BusinessException("LICENCIEMENT_NON_AUTORISE",
                        "Un licenciement disciplinaire exige une procedure decidee avec sanction de licenciement.");
            }
        }
        int jours = demande.preavisJours() == null
                ? preavisParDefaut(demande.motif()) : Math.max(demande.preavisJours(), 0);
        ModePreavis mode = demande.modePreavis() == null ? ModePreavis.EFFECTUE : demande.modePreavis();
        DossierRupture dossier = new DossierRupture();
        dossier.setReference(references.suivante("RUPTURE", "RUP", 4));
        dossier.setIdContrat(contrat.id());
        dossier.setIdEmploye(contrat.idEmploye());
        dossier.setMotif(demande.motif());
        dossier.setInitiateur(demande.initiateur());
        dossier.setDateNotification(demande.dateNotification());
        dossier.setPreavisJours(jours);
        dossier.setPreavisMode(mode);
        dossier.setMotifAutre(demande.motifAutre());
        dossier.setDescription(demande.description());
        dossier.setIdProcedureDisciplinaire(demande.idProcedureDisciplinaire());
        dossier.setIdInitiateur(utilisateurCourant.exiger().getId());
        dossier.setStatut(StatutDossierRupture.INITIEE);
        if (demande.dateSortieSouhaitee() != null) {
            dossier.setDateSortie(demande.dateSortieSouhaitee());
        }
        DossierRupture enregistre = dossiers.save(dossier);
        audit.enregistrer("RUPTURE_CREEE", "dossier_rupture", enregistre.getId(), null,
                enregistre.getReference());
        return RuptureDtos.DossierDto.de(enregistre);
    }

    @Transactional
    public RuptureDtos.DossierDto valider(Long idDossier) {
        UtilisateurConnecte acteur = utilisateurCourant.exiger();
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION);
        DossierRupture dossier = dossier(idDossier);
        if (dossier.getStatut() != StatutDossierRupture.INITIEE) {
            throw new BusinessException("RUPTURE_STATUT_INVALIDE",
                    "Seul un dossier initie peut etre valide.");
        }
        boolean initiativeEmployeur = dossier.getInitiateur() == InitiateurRupture.EMPLOYEUR
                || dossier.getInitiateur() == InitiateurRupture.COMMUN_ACCORD;
        if (initiativeEmployeur && dossier.getIdInitiateur() != null
                && dossier.getIdInitiateur().equals(acteur.getId())) {
            throw new BusinessException("SEPARATION_TACHES",
                    "L'initiateur d'une rupture a l'initiative employeur ne peut pas la valider seul.");
        }
        dossier.setStatut(StatutDossierRupture.EN_VALIDATION);
        dossier.setIdValidateur(acteur.getId());
        dossier.setDateValidation(LocalDateTime.now());
        dossier.setDateDecision(LocalDate.now());
        dossiers.save(dossier);
        audit.enregistrer("RUPTURE_VALIDEE", "dossier_rupture", idDossier, null,
                "validateur=" + acteur.getId());
        return RuptureDtos.DossierDto.de(dossier);
    }

    @Transactional
    public RuptureDtos.DossierDto notifier(Long idDossier) {
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION, Roles.MANAGER);
        DossierRupture dossier = dossier(idDossier);
        if (dossier.getStatut() != StatutDossierRupture.INITIEE
                && dossier.getStatut() != StatutDossierRupture.EN_VALIDATION
                && dossier.getStatut() != StatutDossierRupture.NOTIFIEE) {
            throw new BusinessException("RUPTURE_STATUT_INVALIDE",
                    "Ce dossier ne peut plus etre notifie.");
        }
        LocalDate debut = dossier.getDateNotification().plusDays(1);
        LocalDate fin = dossier.getDateNotification().plusDays(dossier.getPreavisJours());
        dossier.setDatePreavisDebut(debut);
        dossier.setDatePreavisFin(fin);
        dossier.setDateSortie(fin);
        dossier.setStatut(StatutDossierRupture.EN_PREAVIS);
        dossiers.save(dossier);
        genererChecklist(dossier);
        String motif = "Rupture " + dossier.getReference();
        employes.idUtilisateurDe(dossier.getIdEmploye())
                .ifPresent(id -> audit.enregistrer("RUPTURE_NOTIFIEE", "dossier_rupture", idDossier,
                        null, "compte=" + id));
        audit.enregistrer("PREAVIS_CALCULE", "dossier_rupture", idDossier, null,
                debut + " -> " + fin + " (" + motif + ")");
        return RuptureDtos.DossierDto.de(dossier);
    }

    @Transactional
    public RuptureDtos.DossierDto modifierDateSortie(Long idDossier,
                                                     RuptureDtos.ModificationDateSortie demande) {
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION);
        DossierRupture dossier = dossier(idDossier);
        if (STATUTS_CLOTURES.contains(dossier.getStatut())) {
            throw new BusinessException("RUPTURE_CLOTUREE",
                    "Un dossier clos ou archive ne peut plus etre modifie.");
        }
        if (demande == null || demande.dateSortie() == null) {
            throw new BusinessException("DATE_SORTIE_MANQUANTE", "La date de sortie est obligatoire.");
        }
        if (!demande.avenantSigne() || demande.referencePiece() == null
                || demande.referencePiece().isBlank()) {
            throw new BusinessException("AVENANT_REQUIS",
                    "Toute modification de date de sortie exige un avenant signe et sa reference.");
        }
        dossier.setDateSortie(demande.dateSortie());
        dossiers.save(dossier);
        audit.enregistrer("DATE_SORTIE_MODIFIEE", "dossier_rupture", idDossier, null,
                demande.dateSortie() + " / " + demande.referencePiece());
        return RuptureDtos.DossierDto.de(dossier);
    }

    @Transactional
    public RuptureDtos.DroitsDto arreterDroits(Long idDossier) {
        UtilisateurConnecte acteur = utilisateurCourant.exiger();
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION, Roles.MANAGER);
        DossierRupture dossier = dossier(idDossier);
        if (dossier.getStatut() != StatutDossierRupture.NOTIFIEE
                && dossier.getStatut() != StatutDossierRupture.EN_PREAVIS) {
            throw new BusinessException("RUPTURE_STATUT_INVALIDE",
                    "Les droits ne peuvent etre arretes qu'en preavis.");
        }
        if (dossier.getDateSortie() == null) {
            throw new BusinessException("RUPTURE_DATE_SORTIE_MANQUANTE",
                    "La date de sortie est obligatoire pour arreter les droits.");
        }
        int annee = dossier.getDateSortie().getYear();
        List<CongeDtos.SoldeDto> soldats = conges.soldes(dossier.getIdEmploye(), annee);
        BigDecimal congeSolde = soldats.stream()
                .filter(s -> s.typeConge() != null)
                .filter(s -> {
                    String type = s.typeConge().toUpperCase(Locale.ROOT);
                    return type.contains("CONGE") && !type.contains("SANS");
                })
                .map(CongeDtos.SoldeDto::disponible)
                .filter(valeur -> valeur.signum() > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        String congeDetails = soldats.stream()
                .map(s -> s.typeConge() + "=" + nvl(s.disponible()))
                .collect(Collectors.joining("; "));
        int annules = 0;
        for (CongeDtos.DemandeDto demande : conges.demandesEmploye(dossier.getIdEmploye())) {
            if (demande.dateDebut() == null || !demande.dateDebut().isAfter(dossier.getDateSortie())) {
                continue;
            }
            String statut = String.valueOf(demande.statut());
            if (Set.of("ANNULEE", "TERMINEE", "REFUSEE").contains(statut)) {
                continue;
            }
            conges.annuler(demande.id(), "Annulation suite a la sortie " + dossier.getReference(), acteur);
            annules++;
        }
        BigDecimal dedit = calculerDeditFormation(dossier);
        DroitsRupture droits = droitsRepository.findByIdDossier(idDossier).orElseGet(DroitsRupture::new);
        droits.setIdDossier(idDossier);
        droits.setCongeSolde(congeSolde);
        droits.setCongeDetails(congeDetails);
        droits.setCongesFutursAnnules(annules);
        droits.setPrimesDues(BigDecimal.ZERO.setScale(2));
        droits.setAvancesRestantes(BigDecimal.ZERO.setScale(2));
        droits.setDeditFormation(dedit);
        droits.setDetails("soldes=" + congeDetails + "; conges_annules=" + annules);
        droits.setDateArret(LocalDateTime.now());
        DroitsRupture enregistres = droitsRepository.save(droits);
        int plannings = temps.supprimerPlanningsFuturs(dossier.getIdEmploye(), dossier.getDateSortie());
        int missionsAnnulees = missions.annulerFutures(dossier.getIdEmploye(), dossier.getDateSortie(),
                "Sortie " + dossier.getReference());
        dossier.setStatut(StatutDossierRupture.DROITS_ARRETES);
        dossiers.save(dossier);
        audit.enregistrer("DROITS_ARRETES", "dossier_rupture", idDossier, null,
                "conge=" + congeSolde + "; plannings=" + plannings + "; missions=" + missionsAnnulees
                        + "; dedit=" + dedit + "; droitsId=" + enregistres.getId());
        return RuptureDtos.DroitsDto.de(enregistres);
    }

    @Transactional
    public RuptureDtos.TacheDto gererTache(Long idDossier, Long idTache,
                                           RuptureDtos.TacheRequest demande) {
        utilisateurCourant.exiger();
        DossierRupture dossier = dossier(idDossier);
        refuserSiArchive(dossier);
        TacheDepart tache = taches.findById(idTache)
                .orElseThrow(() -> BusinessException.notFound("Tache de depart introuvable."));
        if (!tache.getIdDossier().equals(idDossier)) {
            throw BusinessException.notFound("Tache de depart introuvable pour ce dossier.");
        }
        if (demande == null || demande.statut() == null) {
            throw new BusinessException("TACHE_STATUT_MANQUANT", "Le statut de la tache est obligatoire.");
        }
        tache.setStatut(demande.statut());
        tache.setCommentaire(demande.commentaire());
        if (demande.statut() == StatutTacheRupture.TERMINEE) {
            tache.setDateCloture(LocalDateTime.now());
            tache.setIdResponsable(utilisateurCourant.optionnel().getId());
        } else {
            tache.setDateCloture(null);
        }
        taches.save(tache);
        audit.enregistrer("TACHE_DEPART_MISE_A_JOUR", "dossier_rupture", idDossier, null,
                tache.getCode() + "=" + tache.getStatut());
        return RuptureDtos.TacheDto.de(tache);
    }

    @Transactional
    public RuptureDtos.MaterielDto ajouterMateriel(Long idDossier, RuptureDtos.MaterielRequest demande) {
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION, Roles.MANAGER);
        DossierRupture dossier = dossier(idDossier);
        refuserSiArchive(dossier);
        if (demande == null || demande.libelle() == null || demande.libelle().isBlank()) {
            throw new BusinessException("MATERIEL_INCOMPLET", "Le libelle du materiel est obligatoire.");
        }
        MaterielRupture materiel = new MaterielRupture();
        materiel.setIdDossier(idDossier);
        materiel.setLibelle(demande.libelle());
        materiel.setType(demande.type());
        materiel.setNumeroSerie(demande.numeroSerie());
        materiel.setStatut(StatutMaterielRupture.A_RESTITUER);
        materiel.setMontantRetenue(nvl(demande.montantRetenue()));
        MaterielRupture enregistre = materiels.save(materiel);
        audit.enregistrer("MATERIEL_SORTIE_AJOUTE", "dossier_rupture", idDossier, null,
                demande.libelle());
        return RuptureDtos.MaterielDto.de(enregistre);
    }

    @Transactional
    public RuptureDtos.EntretienDto enregistrerEntretien(Long idDossier,
                                                        RuptureDtos.EntretienRequest demande) {
        DossierRupture dossier = dossier(idDossier);
        refuserSiArchive(dossier);
        if (demande == null) {
            throw new BusinessException("ENTRETIEN_INCOMPLET", "Les informations de l'entretien sont obligatoires.");
        }
        EntretienRupture entretien = entretiens.findByIdDossier(idDossier).orElseGet(EntretienRupture::new);
        entretien.setIdDossier(idDossier);
        entretien.setDateEntretien(demande.dateEntretien());
        entretien.setAnimateurId(demande.animateurId());
        entretien.setQuestionnaireJson(demande.questionnaireJson());
        entretien.setSatisfaction(demande.satisfaction());
        entretien.setCommentaireConfidentiel(demande.commentaireConfidentiel());
        EntretienRupture enregistre = entretiens.save(entretien);
        audit.enregistrer("ENTRETIEN_SORTIE_ENREGISTRE", "dossier_rupture", idDossier, null,
                String.valueOf(enregistre.getId()));
        return RuptureDtos.EntretienDto.de(enregistre);
    }

    @Transactional
    public List<RuptureDtos.DocumentDto> genererDocuments(Long idDossier) {
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION);
        DossierRupture dossier = dossier(idDossier);
        refuserSiArchive(dossier);
        if (soldeService.soldeDto(idDossier) == null) {
            throw new BusinessException("SOLDE_NON_PREPARE",
                    "Le solde de tout compte doit etre prepare avant la generation des documents.");
        }
        EmployeEmbaucheDto employe = employes.dossier(dossier.getIdEmploye());
        List<RuptureDtos.DocumentDto> resultat = new ArrayList<>();
        resultat.add(genererDocument(dossier, "CERTIFICAT_TRAVAIL", "Attestation de travail",
                attestation(employe, dossier)));
        resultat.add(genererDocument(dossier, "SOLDE_TOUT_COMPTE", "Solde de tout compte",
                soldeDocument(dossier)));
        resultat.add(genererDocument(dossier, "LETTRE_REMISE", "Lettre de remise des documents",
                lettreRemise(dossier)));
        audit.enregistrer("DOCUMENTS_SORTIE_GENERES", "dossier_rupture", idDossier, null,
                String.valueOf(resultat.size()));
        return resultat;
    }

    @Transactional
    public RuptureDtos.DocumentDto remettreDocument(Long idDocument, RuptureDtos.RemiseDocument demande) {
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION, Roles.MANAGER);
        DocumentRupture document = documents.findById(idDocument)
                .orElseThrow(() -> BusinessException.notFound("Document de sortie introuvable."));
        DossierRupture dossier = dossier(document.getIdDossier());
        refuserSiArchive(dossier);
        if (demande == null || demande.modeRemise() == null || demande.modeRemise().isBlank()) {
            throw new BusinessException("REMISE_INCOMPLETE", "Le mode de remise est obligatoire.");
        }
        document.setDateRemise(LocalDate.now());
        document.setModeRemise(demande.modeRemise());
        document.setReferencePreuve(demande.referencePreuve());
        documents.save(document);
        List<DocumentRupture> tous = documents.findByIdDossierOrderByIdAsc(dossier.getId());
        if (!tous.isEmpty() && tous.stream().allMatch(d -> d.getDateRemise() != null)) {
            dossier.setStatut(StatutDossierRupture.DOCUMENTS_REMIS);
            dossiers.save(dossier);
        }
        audit.enregistrer("DOCUMENT_SORTIE_REMIS", "dossier_rupture", dossier.getId(), null,
                idDocument + " / " + demande.modeRemise());
        return RuptureDtos.DocumentDto.de(document);
    }

    @Transactional
    public RuptureDtos.DossierDto cloturer(Long idDossier) {
        UtilisateurConnecte acteur = utilisateurCourant.exiger();
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION);
        DossierRupture dossier = dossier(idDossier);
        if (STATUTS_CLOTURES.contains(dossier.getStatut())) {
            throw new BusinessException("RUPTURE_DEJA_CLOTUREE", "Ce dossier est deja clos ou archive.");
        }
        if (dossier.getDateSortie() == null || dossier.getDateSortie().isAfter(LocalDate.now())) {
            throw new BusinessException("SORTIE_NON_ATTEINTE",
                    "La date de sortie doit etre atteinte avant la cloture.");
        }
        var solde = soldes.findByIdDossier(idDossier)
                .orElseThrow(() -> BusinessException.notFound("Le solde de tout compte est obligatoire."));
        if (solde.getStatut() != com.tovo.erp.rupture.model.StatutSoldeRupture.PAYE) {
            throw new BusinessException("SOLDE_NON_PAYE", "Le solde doit etre paye avant la cloture.");
        }
        long bloquantes = taches.countByIdDossierAndBloquanteTrueAndStatutNot(
                idDossier, StatutTacheRupture.TERMINEE);
        if (bloquantes > 0) {
            throw new BusinessException("CHECKLIST_INCOMPLETE",
                    bloquantes + " tache(s) bloquante(s) restent a traiter.");
        }
        List<DocumentRupture> docs = documents.findByIdDossierOrderByIdAsc(idDossier);
        if (docs.isEmpty() || docs.stream().anyMatch(d -> d.getDateRemise() == null)) {
            throw new BusinessException("DOCUMENTS_NON_REMIS",
                    "Tous les documents de sortie doivent avoir une preuve de remise.");
        }

        ContratDtos.ContratDto contrat = contrats.contrat(dossier.getIdContrat());
        boolean rompu = dossier.getMotif().name().startsWith("LICENCIEMENT")
                || dossier.getMotif() == MotifRupture.ABANDON_POSTE
                || dossier.getMotif() == MotifRupture.RUPTURE_PERIODE_ESSAI;
        contrats.clorePourRupture(dossier.getIdContrat(), dossier.getDateSortie(), rompu);
        employes.marquerSorti(dossier.getIdEmploye(), dossier.getDateSortie());
        carrieres.enregistrerDepart(dossier.getIdEmploye(), dossier.getDateSortie(), dossier.getReference());
        employes.idUtilisateurDe(dossier.getIdEmploye()).ifPresent(utilisateurs::desactiverCompte);
        creerDemandeRemplacement(dossier, contrat);
        dossier.setStatut(StatutDossierRupture.CLOTUREE);
        dossiers.save(dossier);
        audit.enregistrer("RUPTURE_CLOTUREE", "dossier_rupture", idDossier, null,
                "acteur=" + acteur.getId() + "; contrat=" + contrat.numero());
        return RuptureDtos.DossierDto.de(dossier);
    }

    @Transactional
    public RuptureDtos.DossierDto archiver(Long idDossier) {
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION);
        DossierRupture dossier = dossier(idDossier);
        if (dossier.getStatut() != StatutDossierRupture.CLOTUREE) {
            throw new BusinessException("RUPTURE_NON_CLOTUREE",
                    "Seul un dossier cloture peut etre archive.");
        }
        dossier.setStatut(StatutDossierRupture.ARCHIVEE);
        dossier.setDateArchivage(LocalDate.now());
        dossiers.save(dossier);
        audit.enregistrer("RUPTURE_ARCHIVEE", "dossier_rupture", idDossier, null,
                String.valueOf(dossier.getDateArchivage()));
        return RuptureDtos.DossierDto.de(dossier);
    }

    @Transactional
    public int archiverAutomatiques() {
        LocalDateTime limite = LocalDateTime.now().minusYears(properties.conservationAnnees());
        List<DossierRupture> aArchiver = dossiers.findByStatutAndDateModificationBefore(
                StatutDossierRupture.CLOTUREE, limite);
        for (DossierRupture dossier : aArchiver) {
            dossier.setStatut(StatutDossierRupture.ARCHIVEE);
            dossier.setDateArchivage(LocalDate.now());
            dossiers.save(dossier);
            audit.enregistrer("RUPTURE_ARCHIVEE_AUTO", "dossier_rupture", dossier.getId(), null,
                    String.valueOf(dossier.getDateArchivage()));
        }
        return aArchiver.size();
    }

    @Transactional
    public int desactiverAccesSortants() {
        LocalDate aujourdHui = LocalDate.now();
        List<DossierRupture> sortants = dossiers.findByDateSortieBetweenOrderByDateSortieAsc(
                aujourdHui.minusDays(1), aujourdHui).stream()
                .filter(d -> EnumSetCompat.OUVERTS.contains(d.getStatut()))
                .toList();
        for (DossierRupture dossier : sortants) {
            employes.idUtilisateurDe(dossier.getIdEmploye()).ifPresent(utilisateurs::desactiverCompte);
            audit.enregistrer("ACCES_SORTIE_DESACTIVE", "dossier_rupture", dossier.getId(), null,
                    String.valueOf(dossier.getDateSortie()));
        }
        return sortants.size();
    }

    @Transactional(readOnly = true)
    public RuptureDtos.IndicateursDto indicateurs(LocalDate debut, LocalDate fin) {
        LocalDate finEffective = fin == null ? LocalDate.now() : fin;
        LocalDate debutEffective = debut == null ? finEffective.minusMonths(12) : debut;
        List<DossierRupture> departs = dossiers.findByDateSortieBetweenOrderByDateSortieAsc(
                debutEffective, finEffective).stream()
                .filter(d -> d.getStatut() == StatutDossierRupture.CLOTUREE
                        || d.getStatut() == StatutDossierRupture.ARCHIVEE)
                .toList();
        long effectifActuel = employes.actifs().size();
        BigDecimal effectifMoyen = BigDecimal.valueOf(effectifActuel + departs.size())
                .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        BigDecimal taux = effectifMoyen.signum() == 0 ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(departs.size()).multiply(BigDecimal.valueOf(100))
                        .divide(effectifMoyen, 2, RoundingMode.HALF_UP);
        Map<Long, EmployeEmbaucheDto> dossiersEmployes = employes.dossiersParIds(
                departs.stream().map(DossierRupture::getIdEmploye).toList());
        long mois = departs.stream()
                .mapToLong(d -> {
                    EmployeEmbaucheDto employe = dossiersEmployes.get(d.getIdEmploye());
                    LocalDate entre = employe == null ? null : employe.datePremiereEmbauche();
                    if (entre == null && employe != null) {
                        entre = employe.dateEmbauche();
                    }
                    return entre == null ? 0 : ChronoUnit.MONTHS.between(entre, d.getDateSortie());
                })
                .filter(valeur -> valeur >= 0)
                .sum();
        BigDecimal ancienneteMoyenne = departs.isEmpty() ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(mois).divide(BigDecimal.valueOf(departs.size()), 2, RoundingMode.HALF_UP);
        return new RuptureDtos.IndicateursDto(departs.size(), effectifMoyen, taux, ancienneteMoyenne);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> types() {
        Map<String, Object> resultat = new LinkedHashMap<>();
        resultat.put("motifs", Arrays.stream(MotifRupture.values())
                .map(m -> new RuptureDtos.TypeDto(m.name(), libelleMotif(m))).toList());
        resultat.put("initiateurs", Arrays.stream(InitiateurRupture.values())
                .map(i -> new RuptureDtos.TypeDto(i.name(), libelleInitiateur(i))).toList());
        resultat.put("modesPreavis", Arrays.stream(ModePreavis.values())
                .map(m -> new RuptureDtos.TypeDto(m.name(), libellePreavis(m))).toList());
        resultat.put("statuts", Arrays.stream(StatutDossierRupture.values())
                .map(s -> new RuptureDtos.TypeDto(s.name(), libelleStatut(s))).toList());
        resultat.put("statutsTache", Arrays.stream(StatutTacheRupture.values())
                .map(s -> new RuptureDtos.TypeDto(s.name(), s.name().replace('_', ' '))).toList());
        resultat.put("modesPaiement", List.of(
                new RuptureDtos.TypeDto("VIREMENT", "Virement"),
                new RuptureDtos.TypeDto("CHEQUE", "Cheque"),
                new RuptureDtos.TypeDto("ESPECES", "Especes")));
        return resultat;
    }

    @Transactional(readOnly = true)
    public DossierRupture dossier(Long idDossier) {
        return dossiers.findById(idDossier)
                .orElseThrow(() -> BusinessException.notFound("Dossier de rupture introuvable."));
    }

    private void genererChecklist(DossierRupture dossier) {
        if (!taches.findByIdDossierOrderByIdAsc(dossier.getId()).isEmpty()) {
            return;
        }
        LocalDate sortie = dossier.getDateSortie() == null ? LocalDate.now() : dossier.getDateSortie();
        List<TacheDepart> tacheList = new ArrayList<>();
        tacheList.add(tache(dossier, "RESTITUTION_MATERIEL", "Restitution du materiel", "LOGISTIQUE",
                "Employe / RH", "RH", true, sortie.minusDays(1)));
        tacheList.add(tache(dossier, "DESACTIVATION_COMPTE", "Desactivation des acces", "SI",
                "DSI", null, true, sortie));
        tacheList.add(tache(dossier, "SOLDE_TOUT_COMPTE", "Solde de tout compte", "PAIE",
                "Paie", null, true, sortie.plusDays(properties.delaiPaiementJours())));
        tacheList.add(tache(dossier, "TRANSMISSION_DOSSIER", "Transmission du dossier", "RH",
                "Manager / RH", null, true, sortie.minusDays(1)));
        boolean posteCritique = estPosteCritique(dossier);
        tacheList.add(tache(dossier, "PASSATION_POSTE",
                posteCritique ? "Passation obligatoire (poste critique)" : "Passation des duties", "MANAGEMENT",
                "Manager", null, posteCritique, sortie.minusDays(2)));
        taches.saveAll(tacheList);
    }

    private TacheDepart tache(DossierRupture dossier, String code, String libelle, String categorie,
                              String responsable, String roleResponsable, boolean bloquante,
                              LocalDate echeance) {
        TacheDepart tache = new TacheDepart();
        tache.setIdDossier(dossier.getId());
        tache.setCode(code);
        tache.setLibelle(libelle);
        tache.setCategorie(categorie);
        tache.setResponsable(responsable);
        tache.setStatut(StatutTacheRupture.A_FAIRE);
        tache.setBloquante(bloquante);
        tache.setDateEcheance(echeance);
        if (roleResponsable != null && utilisateurCourant.aLeRole(roleResponsable)) {
            tache.setIdResponsable(utilisateurCourant.optionnel().getId());
        }
        return tache;
    }

    private boolean estPosteCritique(DossierRupture dossier) {
        Long idPoste = employes.dossier(dossier.getIdEmploye()).idPoste();
        if (idPoste == null) {
            return false;
        }
        PosteDto.Liste poste = postes.trouver(idPoste);
        if (poste == null || poste.categorie() == null) {
            return false;
        }
        String categorie = poste.categorie().toUpperCase(Locale.ROOT);
        return categorie.contains("CRITIQUE") || categorie.contains("STRATEGIQUE");
    }

    private void creerDemandeRemplacement(DossierRupture dossier, ContratDtos.ContratDto contrat) {
        EmployeEmbaucheDto employe = employes.dossier(dossier.getIdEmploye());
        if (employe.idService() == null) {
            return;
        }
        demandes.creerRemplacementSortie(utilisateurCourant.exiger(), employe.idService(),
                employe.idPoste(), dossier.getIdEmploye(),
                "Remplacement suite a " + dossier.getReference() + " (" + dossier.getMotif() + ")",
                dossier.getDateSortie(),
                "Remplacement de " + nom(employe.prenom(), employe.nom())
                        + " - contrat " + contrat.numero());
    }

    private RuptureDtos.DocumentDto genererDocument(DossierRupture dossier, String code,
                                                    String libelle, String contenu) {
        byte[] octets = contenu.getBytes(StandardCharsets.UTF_8);
        DocumentEmbaucheDto documentEmploye = employes.enregistrerDocumentRupture(
                dossier.getIdEmploye(), libelle, code.toLowerCase(Locale.ROOT) + "-" + dossier.getId() + ".txt",
                "text/plain", octets, dossier.getIdContrat(), LocalDate.now());
        DocumentRupture document = documents.findByIdDossierOrderByIdAsc(dossier.getId()).stream()
                .filter(d -> code.equals(d.getCode()))
                .findFirst().orElseGet(DocumentRupture::new);
        document.setIdDossier(dossier.getId());
        document.setCode(code);
        document.setLibelle(libelle);
        document.setFormat("TXT");
        document.setHashSha256(empreinte(octets));
        document.setCheminOuHash(document.getHashSha256());
        document.setIdDocumentEmploye(documentEmploye.id());
        document.setDateGeneration(LocalDateTime.now());
        DocumentRupture enregistre = documents.save(document);
        return RuptureDtos.DocumentDto.de(enregistre);
    }

    private String attestation(EmployeEmbaucheDto employe, DossierRupture dossier) {
        List<String> fonctions = contrats.affectationsDe(dossier.getIdEmploye()).stream()
                .filter(a -> a.idPoste() != null)
                .map(a -> postes.trouver(a.idPoste()))
                .filter(java.util.Objects::nonNull)
                .map(PosteDto.Liste::intitule)
                .distinct()
                .sorted()
                .toList();
        return "ATTESTATION DE TRAVAIL\n\n"
                + "Date d'entree : " + (employe.datePremiereEmbauche() == null
                        ? employe.dateEmbauche() : employe.datePremiereEmbauche()) + "\n"
                + "Date de sortie : " + dossier.getDateSortie() + "\n"
                + "Fonctions : " + String.join(", ", fonctions) + "\n";
    }

    private String soldeDocument(DossierRupture dossier) {
        RuptureDtos.SoldeDto solde = soldeService.soldeDto(dossier.getId());
        StringBuilder contenu = new StringBuilder("SOLDE DE TOUT COMPTE\n\n")
                .append("Reference : ").append(dossier.getReference()).append("\n")
                .append("Date de sortie : ").append(dossier.getDateSortie()).append("\n\n");
        if (solde != null && solde.lignes() != null) {
            for (RuptureDtos.LigneDto ligne : solde.lignes()) {
                contenu.append(ligne.libelle()).append(" : ").append(ligne.montant())
                        .append(" ").append(solde.devise()).append("\n");
            }
            contenu.append("\nTotal brut : ").append(solde.totalBrut())
                    .append("\nTotal retenues : ").append(solde.totalRetenues())
                    .append("\nNet indicatif avant cotisations : ")
                    .append(solde.netAvantCotisations()).append("\n");
        }
        return contenu.toString();
    }

    private String lettreRemise(DossierRupture dossier) {
        return "REMISE DES DOCUMENTS DE SORTIE\n\n"
                + "Reference : " + dossier.getReference() + "\n"
                + "Les documents de sortie sont remis au salarie. "
                + "La preuve de remise doit etre enregistree dans le dossier.\n";
    }

    private BigDecimal calculerDeditFormation(DossierRupture dossier) {
        Map<Long, LocalDate> datesSessions = formations.sessions().stream()
                .collect(Collectors.toMap(EvaluationDtos.SessionDto::id,
                        s -> s.dateFin() == null ? s.dateDebut() : s.dateFin(), (a, b) -> a));
        List<EvaluationDtos.InscriptionDto> inscriptions = formations.inscriptionsEmploye(
                dossier.getIdEmploye());
        BigDecimal total = BigDecimal.ZERO;
        for (EvaluationDtos.InscriptionDto inscription : inscriptions) {
            LocalDate finFormation = datesSessions.get(inscription.idSession());
            if (finFormation == null || !finFormation.isBefore(dossier.getDateSortie())) {
                continue;
            }
            long moisEcoulés = ChronoUnit.MONTHS.between(finFormation, dossier.getDateSortie());
            int moisRestants = (int) Math.max(0, 1200 - moisEcoulés);
            if (moisRestants == 0) {
                continue;
            }
            EvaluationDtos.RemboursementDto remboursement = formations.remboursement(
                    inscription.id(), moisRestants);
            if (remboursement != null && remboursement.remboursement() != null) {
                total = total.add(remboursement.remboursement());
            }
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private String empreinte(byte[] contenu) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenu));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponible", ex);
        }
    }

    private int preavisParDefaut(MotifRupture motif) {
        return switch (motif) {
            case DEMISSION -> properties.preavisDemissionJours();
            case LICENCIEMENT_DISCIPLINAIRE, LICENCIEMENT_ECONOMIQUE,
                    LICENCIEMENT_INAPTITUDE, LICENCIEMENT_INSUFFISANCE ->
                    properties.preavisLicenciementJours();
            case RUPTURE_AMIABLE -> properties.preavisRuptureAmiableJours();
            case RETRAITE -> properties.preavisRetraiteJours();
            default -> properties.preavisDefautJours();
        };
    }

    private String libelleMotif(MotifRupture motif) {
        return motif.name().replace('_', ' ').toLowerCase(Locale.ROOT);
    }

    private String libelleInitiateur(InitiateurRupture initiateur) {
        return switch (initiateur) {
            case SALARIE -> "Salarie";
            case EMPLOYEUR -> "Employeur";
            case COMMUN_ACCORD -> "Commun accord";
            case ECHEANCE -> "Echeance";
            case AUTRE -> "Autre";
        };
    }

    private String libellePreavis(ModePreavis mode) {
        return switch (mode) {
            case EFFECTUE -> "Effectue";
            case DISPENSE_EMPLOYEUR -> "Dispense employeur";
            case DISPENSE_SALARIE -> "Dispense salarie";
            case SANS_PREAVIS -> "Sans preavis";
        };
    }

    private String libelleStatut(StatutDossierRupture statut) {
        return statut.name().replace('_', ' ').toLowerCase(Locale.ROOT);
    }

    private String nom(String prenom, String nom) {
        return (prenom == null ? "" : prenom + " ") + (nom == null ? "" : nom);
    }

    private BigDecimal nvl(BigDecimal valeur) {
        return valeur == null ? BigDecimal.ZERO.setScale(2) : valeur;
    }

    private void refuserSiArchive(DossierRupture dossier) {
        if (dossier.getStatut() == StatutDossierRupture.ARCHIVEE) {
            throw new BusinessException("DOSSIER_ARCHIVE",
                    "Un dossier archive est consultable mais plus modifiable.");
        }
    }

    private static final class EnumSetCompat {
        private static final Set<StatutDossierRupture> OUVERTS = Set.of(
                StatutDossierRupture.EN_PREAVIS, StatutDossierRupture.DROITS_ARRETES,
                StatutDossierRupture.SOLDE_EN_PREPARATION, StatutDossierRupture.SOLDE_VALIDE,
                StatutDossierRupture.SOLDE_PAYE, StatutDossierRupture.DOCUMENTS_REMIS);
    }
}
