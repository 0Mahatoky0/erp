package com.tovo.erp.carriere.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tovo.erp.besoin.dto.PosteDto;
import com.tovo.erp.besoin.service.PosteService;
import com.tovo.erp.carriere.dto.CarriereMobiliteDtos;
import com.tovo.erp.carriere.model.CampagneAugmentation;
import com.tovo.erp.carriere.model.GrilleSalariale;
import com.tovo.erp.carriere.model.MouvementCarriere;
import com.tovo.erp.carriere.model.Mutation;
import com.tovo.erp.carriere.model.Promotion;
import com.tovo.erp.carriere.model.StatutMouvementCarriere;
import com.tovo.erp.carriere.repository.CampagneAugmentationRepository;
import com.tovo.erp.carriere.repository.GrilleSalarialeRepository;
import com.tovo.erp.carriere.repository.MouvementCarriereRepository;
import com.tovo.erp.carriere.repository.MutationRepository;
import com.tovo.erp.carriere.repository.PromotionRepository;
import com.tovo.erp.contrat.dto.ContratDtos;
import com.tovo.erp.contrat.service.CarriereService;
import com.tovo.erp.contrat.service.ContratService;
import com.tovo.erp.discipline.service.EligibiliteDisciplineService;
import com.tovo.erp.employe.service.EmployeService;
import com.tovo.erp.evaluation.dto.EvaluationDtos;
import com.tovo.erp.evaluation.service.EvaluationService;
import com.tovo.erp.evaluation.service.ReferentielService;
import com.tovo.erp.shared.BusinessException;
import com.tovo.erp.shared.MouvementAppliqueEvent;
import com.tovo.erp.shared.Roles;
import com.tovo.erp.shared.UtilisateurCourant;
import com.tovo.erp.shared.audit.JournalAuditService;
import com.tovo.erp.shared.reference.ReferenceService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class MouvementCarriereService {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final BigDecimal NOTE_MINIMALE = new BigDecimal("3.5");
    private static final int ANCIENNETE_MINIMALE_MOIS = 12;
    private static final int RETROACTIVITE_MAX_MOIS = 3;
    private static final BigDecimal SEUIL_VALIDATION_MAJEURE = new BigDecimal("10");
    private static final BigDecimal CENT = BigDecimal.valueOf(100);

    private final MouvementCarriereRepository mouvements;
    private final PromotionRepository promotions;
    private final MutationRepository mutations;
    private final GrilleSalarialeRepository grilles;
    private final CampagneAugmentationRepository campagnes;
    private final EmployeService employes;
    private final ContratService contrats;
    private final CarriereService historique;
    private final PosteService postes;
    private final EvaluationService evaluations;
    private final ReferentielService referentiel;
    private final EligibiliteDisciplineService eligibiliteDiscipline;
    private final UtilisateurCourant utilisateurCourant;
    private final JournalAuditService audit;
    private final ReferenceService references;
    private final ApplicationEventPublisher evenements;

    public MouvementCarriereService(MouvementCarriereRepository mouvements,
                                    PromotionRepository promotions,
                                    MutationRepository mutations,
                                    GrilleSalarialeRepository grilles,
                                    CampagneAugmentationRepository campagnes,
                                    EmployeService employes,
                                    ContratService contrats,
                                    CarriereService historique,
                                     PosteService postes,
                                     EvaluationService evaluations,
                                     ReferentielService referentiel,
                                     EligibiliteDisciplineService eligibiliteDiscipline,
                                     UtilisateurCourant utilisateurCourant,
                                     JournalAuditService audit,
                                     ReferenceService references,
                                     ApplicationEventPublisher evenements) {

        this.mouvements = mouvements;
        this.promotions = promotions;
        this.mutations = mutations;
        this.grilles = grilles;
        this.campagnes = campagnes;
        this.employes = employes;
        this.contrats = contrats;
        this.historique = historique;
        this.postes = postes;
        this.evaluations = evaluations;
        this.referentiel = referentiel;
        this.eligibiliteDiscipline = eligibiliteDiscipline;
        this.utilisateurCourant = utilisateurCourant;
        this.audit = audit;
        this.references = references;
        this.evenements = evenements;
    }

    @Transactional(readOnly = true)
    public List<CarriereMobiliteDtos.MouvementDto> lister(String statut) {
        List<MouvementCarriere> liste = statut == null || statut.isBlank()
                ? mouvements.findAll()
                : mouvements.findByStatut(statut(statut));
        return liste.stream().map(this::enDto).toList();
    }

    @Transactional(readOnly = true)
    public CarriereMobiliteDtos.MouvementDto mouvement(Long id) {
        return enDto(mouvementEntite(id));
    }

    @Transactional
    public CarriereMobiliteDtos.MouvementDto creer(CarriereMobiliteDtos.MouvementRequest requete) {
        if (!employes.existe(requete.idEmploye())) {
            throw BusinessException.notFound("Le salarie " + requete.idEmploye() + " n'existe pas.");
        }
        var acteur = utilisateurCourant.exiger();
        if (Roles.EMPLOYE.equals(acteur.getRoleNom())
                && (!acteur.getId().equals(requete.idEmploye())
                || requete.type() != com.tovo.erp.carriere.model.TypeMouvementCarriere.MUTATION)) {
            throw BusinessException.forbidden(
                    "Un salarie ne peut demander qu'une mutation le concernant.");
        }
        if (Boolean.TRUE.equals(requete.derogationDirection())) {
            throw BusinessException.forbidden(
                    "Une derogation ne peut etre accordee que par la Direction lors de la validation.");
        }
        MouvementCarriere mouvement = new MouvementCarriere();
        mouvement.setReference(references.suivante("MOUVEMENT_CARRIERE", "MV", 4));
        mouvement.setIdEmploye(requete.idEmploye());
        mouvement.setType(requete.type());
        mouvement.setInitiateur(requete.initiateur());
        mouvement.setMotif(requete.motif());
        mouvement.setJustification(requete.justification());
        mouvement.setDateEffetSouhaitee(requete.dateEffetSouhaitee());
        mouvement.setIdEvaluation(requete.idEvaluation());
        mouvement.setIdAvenant(requete.idAvenant());
        mouvement.setDocuments(requete.documents());
        mouvement.setDerogationDirection(false);
        mouvement.setMotifDerogation(null);
        mouvement.setIdDemandeur(acteur.getId());
        mouvement.setAncienneSituation(json(situationActuelle(requete.idEmploye())));
        if (requete.type() == com.tovo.erp.carriere.model.TypeMouvementCarriere.CHANGEMENT_SALAIRE
                && requete.nouveauSalaire() != null) {
            Map<String, Object> conditionsInitiales = new LinkedHashMap<>();
            conditionsInitiales.put("nouveauSalaire", requete.nouveauSalaire());
            mouvement.setConditionsVerifiees(json(conditionsInitiales));
        }
        MouvementCarriere cree = mouvements.save(mouvement);
        enregistrerDetails(cree, requete);
        audit.enregistrer("MOUVEMENT_CREE", "mouvement_carriere", cree.getId(), null, cree.getReference());
        return enDto(cree);
    }

    @Transactional
    public CarriereMobiliteDtos.MouvementDto soumettre(Long id) {
        MouvementCarriere mouvement = mouvementEntite(id);
        exigerStatut(mouvement, StatutMouvementCarriere.BROUILLON, StatutMouvementCarriere.A_COMPLETER);
        Map<String, Object> controles = controles(mouvement);
        controles.put("circuitValidation", circuitValidation(mouvement));
        controles.put("etapeCourante", 0);
        mouvement.setConditionsVerifiees(json(controles));
        mouvement.setNouvelleSituation(json(situationCible(mouvement)));
        boolean conforme = controles.entrySet().stream()
                .filter(e -> e.getValue() instanceof Boolean)
                .allMatch(e -> Boolean.TRUE.equals(e.getValue()) || cleDerogeable(e.getKey(), mouvement));
        if (!conforme) {
            mouvement.setStatut(StatutMouvementCarriere.A_COMPLETER);
            mouvements.save(mouvement);
            throw new BusinessException("MOUVEMENT_NON_ELIGIBLE",
                    "Les controles automatiques ne sont pas satisfaits : " + controles);
        }
        mouvement.setStatut(StatutMouvementCarriere.EN_VALIDATION);
        MouvementCarriere enregistre = mouvements.save(mouvement);
        audit.enregistrer("MOUVEMENT_SOUMIS", "mouvement_carriere", id, null, enregistre.getStatut().name());
        return enDto(enregistre);
    }

    @Transactional
    public CarriereMobiliteDtos.MouvementDto valider(Long id, CarriereMobiliteDtos.DecisionRequest requete) {
        MouvementCarriere mouvement = mouvementEntite(id);
        exigerStatut(mouvement, StatutMouvementCarriere.EN_VALIDATION);
        var acteur = utilisateurCourant.exiger();
        verifierSeparationDesTaches(mouvement, acteur);
        Map<String, Object> conditions = lireMap(mouvement.getConditionsVerifiees());
        List<Map<String, Object>> circuit = circuitLisible(conditions, mouvement);
        int etape = etapeCourante(conditions);
        if (etape >= circuit.size()) {
            throw new BusinessException("CIRCUIT_TERMINE", "Le circuit de validation est deja termine.");
        }
        Map<String, Object> courante = circuit.get(etape);
        String roleAttendu = String.valueOf(courante.get("role"));
        if (!roleAttendu.equals(acteur.getRoleNom())) {
            throw BusinessException.forbidden("L'etape " + (etape + 1) + " requiert le role " + roleAttendu + ".");
        }
        if (requete.derogationDirection() != null && requete.derogationDirection()) {
            utilisateurCourant.exigerRole(Roles.DIRECTION);
            mouvement.setDerogationDirection(true);
            mouvement.setMotifDerogation(requete.motifDerogation());
        }
        courante.put("statut", "APPROUVE");
        courante.put("idUtilisateur", acteur.getId());
        courante.put("dateDecision", LocalDateTime.now().toString());
        int suivante = etape + 1;
        conditions.put("etapeCourante", suivante);
        conditions.put("circuitValidation", circuit);
        mouvement.setConditionsVerifiees(json(conditions));
        mouvement.setIdValidateur(acteur.getId());
        mouvement.setDateDecision(LocalDateTime.now());
        if (suivante == circuit.size()) {
            mouvement.setStatut(StatutMouvementCarriere.VALIDE);
        }
        MouvementCarriere enregistre = mouvements.save(mouvement);
        audit.enregistrer("MOUVEMENT_VALIDE", "mouvement_carriere", id, null,
                enregistre.getStatut().name());
        return enDto(enregistre);
    }

    @Transactional
    public CarriereMobiliteDtos.MouvementDto refuser(Long id, String motif) {
        MouvementCarriere mouvement = mouvementEntite(id);
        exigerStatut(mouvement, StatutMouvementCarriere.EN_VALIDATION);
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION, Roles.FINANCE, Roles.MANAGER);
        verifierSeparationDesTaches(mouvement, utilisateurCourant.exiger());
        if (motif == null || motif.isBlank()) {
            throw new BusinessException("MOTIF_REFUS_OBLIGATOIRE", "Le motif du refus est obligatoire.");
        }
        mouvement.setStatut(StatutMouvementCarriere.REFUSE);
        mouvement.setMotifRefus(motif);
        MouvementCarriere enregistre = mouvements.save(mouvement);
        audit.enregistrer("MOUVEMENT_REFUSE", "mouvement_carriere", id, null, motif);
        return enDto(enregistre);
    }

    @Transactional
    public CarriereMobiliteDtos.MouvementDto preparerAvenant(Long id) {
        MouvementCarriere mouvement = mouvementEntite(id);
        exigerStatut(mouvement, StatutMouvementCarriere.VALIDE);
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION);
        if (mouvement.getIdAvenant() == null) {
            throw new BusinessException("AVENANT_OBLIGATOIRE",
                    "Un avenant doit etre rattache au mouvement avant signature.");
        }
        mouvement.setStatut(StatutMouvementCarriere.AVENANT_EN_SIGNATURE);
        MouvementCarriere enregistre = mouvements.save(mouvement);
        audit.enregistrer("AVENANT_EN_SIGNATURE", "mouvement_carriere", id, null,
                String.valueOf(mouvement.getIdAvenant()));
        return enDto(enregistre);
    }

    @Transactional
    public CarriereMobiliteDtos.MouvementDto signerAvenant(Long id) {
        MouvementCarriere mouvement = mouvementEntite(id);
        exigerStatut(mouvement, StatutMouvementCarriere.AVENANT_EN_SIGNATURE);
        mouvement.setStatut(StatutMouvementCarriere.PLANIFIE);
        MouvementCarriere enregistre = mouvements.save(mouvement);
        audit.enregistrer("AVENANT_SIGNE", "mouvement_carriere", id, null, null);
        return enDto(enregistre);
    }

    @Transactional
    public CarriereMobiliteDtos.MouvementDto refuserParSalarie(Long id, String motif) {
        MouvementCarriere mouvement = mouvementEntite(id);
        exigerStatut(mouvement, StatutMouvementCarriere.AVENANT_EN_SIGNATURE);
        if (motif == null || motif.isBlank()) {
            throw new BusinessException("MOTIF_REFUS_OBLIGATOIRE", "Le motif du refus est obligatoire.");
        }
        mouvement.setStatut(StatutMouvementCarriere.REFUSE_PAR_SALARIE);
        mouvement.setMotifRefus(motif);
        MouvementCarriere enregistre = mouvements.save(mouvement);
        audit.enregistrer("AVENANT_REFUSE_SALARIE", "mouvement_carriere", id, null, motif);
        return enDto(enregistre);
    }

    @Transactional
    public CarriereMobiliteDtos.MouvementDto planifier(Long id) {
        MouvementCarriere mouvement = mouvementEntite(id);
        exigerStatut(mouvement, StatutMouvementCarriere.VALIDE);
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION);
        mouvement.setStatut(StatutMouvementCarriere.PLANIFIE);
        MouvementCarriere enregistre = mouvements.save(mouvement);
        audit.enregistrer("MOUVEMENT_PLANIFIE", "mouvement_carriere", id, null,
                String.valueOf(mouvement.getDateEffetSouhaitee()));
        return enDto(enregistre);
    }

    @Transactional
    public CarriereMobiliteDtos.MouvementDto appliquer(Long id) {
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION);
        MouvementCarriere mouvement = mouvementEntite(id);
        exigerStatut(mouvement, StatutMouvementCarriere.VALIDE, StatutMouvementCarriere.PLANIFIE,
                StatutMouvementCarriere.AVENANT_EN_SIGNATURE);
        if (mouvement.getStatut() == StatutMouvementCarriere.AVENANT_EN_SIGNATURE
                && exigeAvenantSigne(mouvement)) {
            throw new BusinessException("AVENANT_SIGNATURE_REQUISE",
                    "Ce mouvement ne peut etre applique avant la signature de l'avenant.");
        }
        Map<String, Object> avant = situationActuelle(mouvement.getIdEmploye());
        Map<String, Object> cible = situationCible(mouvement);
        LocalDate dateEffet = mouvement.getDateEffet() == null
                ? mouvement.getDateEffetSouhaitee() : mouvement.getDateEffet();
        mouvement.setDateEffet(dateEffet);
        Long idUtilisateur = utilisateurCourant.exiger().getId();
        switch (mouvement.getType()) {
            case PROMOTION -> appliquerPromotion(mouvement, dateEffet, idUtilisateur);
            case MUTATION, DETACHEMENT, CHANGEMENT_POSTE -> appliquerMutation(mouvement, dateEffet, idUtilisateur);
            case CHANGEMENT_SALAIRE -> appliquerChangementSalaire(mouvement, dateEffet, idUtilisateur);
        }
        mouvement.setStatut(StatutMouvementCarriere.APPLIQUE);
        mouvement.setDateApplication(LocalDateTime.now());
        MouvementCarriere enregistre = mouvements.save(mouvement);
        audit.enregistrer("MOUVEMENT_APPLIQUE", "mouvement_carriere", id, null, dateEffet.toString());
        evenements.publishEvent(evenementApplication(enregistre, avant, cible, dateEffet, idUtilisateur));
        return enDto(enregistre);
    }

    @Transactional
    public CarriereMobiliteDtos.MouvementDto annuler(Long id, String motif) {
        MouvementCarriere mouvement = mouvementEntite(id);
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION);
        if (!List.of(StatutMouvementCarriere.BROUILLON, StatutMouvementCarriere.A_COMPLETER,
                StatutMouvementCarriere.PLANIFIE, StatutMouvementCarriere.VALIDE)
                .contains(mouvement.getStatut())) {
            throw new BusinessException("MOUVEMENT_ANNULATION_INTERDITE",
                    "Seul un mouvement en cours avant application peut etre annule.");
        }
        mouvement.setStatut(StatutMouvementCarriere.ANNULE);
        mouvement.setMotifRefus(motif);
        MouvementCarriere enregistre = mouvements.save(mouvement);
        audit.enregistrer("MOUVEMENT_ANNULE", "mouvement_carriere", id, null, motif);
        return enDto(enregistre);
    }

    @Transactional(readOnly = true)
    public CarriereMobiliteDtos.CarriereDateDto carriereALaDate(Long idEmploye,
                                                               LocalDate dateValidite,
                                                               LocalDate dateConnaissance) {
        LocalDate connaissance = dateConnaissance == null ? LocalDate.now() : dateConnaissance;
        ContratDtos.AffectationDto affectation = contrats.affectationsDe(idEmploye).stream()
                .filter(a -> visible(a.dateDebut(), a.dateFin(), a.enregistreLe(), a.remplaceLe(),
                        dateValidite, connaissance))
                .filter(ContratDtos.AffectationDto::principale)
                .findFirst()
                .orElse(null);
        com.tovo.erp.contrat.dto.CarriereDtos.SalaireDto salaire = historique.salaires(idEmploye).stream()
                .filter(s -> visible(s.dateEffet(), s.dateFin(), s.enregistreLe(), s.remplaceLe(),
                        dateValidite, connaissance))
                .filter(s -> s.enregistreLe() != null
                        && !s.enregistreLe().toLocalDate().isAfter(connaissance))
                .findFirst()
                .orElse(null);
        String contrat = contrats.contratsDe(idEmploye).stream()
                .filter(c -> !c.dateDebut().isAfter(dateValidite))
                .filter(c -> c.dateFinPrevue() == null || !c.dateFinPrevue().isBefore(dateValidite))
                .map(ContratDtos.ContratDto::numero)
                .findFirst()
                .orElse(null);
        return new CarriereMobiliteDtos.CarriereDateDto(idEmploye, dateValidite, connaissance, contrat,
                affectation == null ? null : affectation.idPoste(),
                affectation == null ? null : affectation.idService(),
                affectation == null ? null : affectation.idDepartement(),
                affectation == null ? null : affectation.lieuTravail(),
                affectation == null ? null : affectation.idManager(),
                affectation == null ? null : affectation.categorie(),
                affectation == null ? null : affectation.echelon(),
                salaire == null ? null : salaire.montantBrut(),
                affectation == null ? null : Objects.toString(affectation.motif(), null));
    }

    @Transactional(readOnly = true)
    public List<CarriereMobiliteDtos.FriseLigneDto> frise(Long idEmploye) {
        List<CarriereMobiliteDtos.FriseLigneDto> lignes = new ArrayList<>();
        mouvements.findByIdEmployeOrderByDateCreationDesc(idEmploye).forEach(m ->
                lignes.add(new CarriereMobiliteDtos.FriseLigneDto("MOUVEMENT", m.getId(),
                        m.getType().name(), m.getDateEffet() == null ? m.getDateEffetSouhaitee() : m.getDateEffet(),
                        m.getReference() + " · " + m.getMotif(), "INTERNE")));
        historique.evenements(idEmploye).forEach(e -> {
            String typeEvenement = Objects.toString(e.type(), null);
            lignes.add(new CarriereMobiliteDtos.FriseLigneDto("EVENEMENT", e.id(), typeEvenement,
                    e.dateEffet(), e.nouvelleValeur(),
                    "SANCTION".equals(typeEvenement) ? "TRES_CONFIDENTIEL" : "INTERNE"));
        });
        if (utilisateurCourant.aLeRole(Roles.RH, Roles.DIRECTION, Roles.FINANCE)) {
            historique.salaires(idEmploye).forEach(s ->
                    lignes.add(new CarriereMobiliteDtos.FriseLigneDto("SALAIRE", s.id(), "SALAIRE",
                            s.dateEffet(), s.montantBrut().toPlainString(), "CONFIDENTIEL")));
        }
        return lignes.stream()
                .sorted(Comparator.comparing(CarriereMobiliteDtos.FriseLigneDto::date,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Transactional(readOnly = true)
    public CarriereMobiliteDtos.RapportIntegriteDto rapportIntegrite() {
        List<String> anomalies = new ArrayList<>();
        int chevauchements = 0;
        int trous = 0;
        int horsContrat = 0;
        int mouvementsSansHistorique = 0;
        List<Long> employesActifs = employes.actifs().stream().map(e -> e.id()).toList();
        for (Long idEmploye : employesActifs) {
            List<ContratDtos.AffectationDto> affectations = contrats.affectationsDe(idEmploye).stream()
                    .sorted(Comparator.comparing(ContratDtos.AffectationDto::dateDebut))
                    .toList();
            for (int i = 0; i < affectations.size(); i++) {
                ContratDtos.AffectationDto courante = affectations.get(i);
                if (courante.idContrat() == null) {
                    horsContrat++;
                    anomalies.add("Affectation " + courante.id() + " sans contrat.");
                }
                if (i + 1 < affectations.size()
                        && affectations.get(i + 1).dateDebut().isAfter(courante.dateFin() == null
                        ? LocalDate.now() : courante.dateFin().plusDays(1))) {
                    trous++;
                    anomalies.add("Trou d'affectation pour le salarie " + idEmploye + ".");
                }
                boolean doublon = affectations.stream().anyMatch(a -> !a.id().equals(courante.id())
                        && !a.dateDebut().isAfter(courante.dateFin() == null ? LocalDate.now() : courante.dateFin())
                        && !(a.dateFin() == null || a.dateFin().isBefore(courante.dateDebut())));
                if (doublon) {
                    chevauchements++;
                }
            }
            List<Long> idsMouvements = mouvements.findByIdEmployeOrderByDateCreationDesc(idEmploye).stream()
                    .filter(m -> m.getStatut() == StatutMouvementCarriere.APPLIQUE)
                    .map(MouvementCarriere::getId)
                    .toList();
            List<Long> idsHistorique = new ArrayList<>();
            affectations.forEach(a -> { if (a.idMouvement() != null) idsHistorique.add(a.idMouvement()); });
            historique.salaires(idEmploye).forEach(s -> { if (s.idMouvement() != null) idsHistorique.add(s.idMouvement()); });
            mouvementsSansHistorique += (int) idsMouvements.stream()
                    .filter(id -> !idsHistorique.contains(id)).count();
        }
        return new CarriereMobiliteDtos.RapportIntegriteDto(chevauchements, trous, horsContrat,
                mouvementsSansHistorique, anomalies);
    }

    @Transactional(readOnly = true)
    public List<CarriereMobiliteDtos.CampagneDto> campagnes() {
        return campagnes.findAll().stream()
                .map(c -> CarriereMobiliteDtos.CampagneDto.de(c, lireMap(c.getSimulation())))
                .toList();
    }

    @Transactional
    public CarriereMobiliteDtos.CampagneDto creerCampagne(CarriereMobiliteDtos.CampagneRequest requete) {
        CampagneAugmentation campagne = new CampagneAugmentation();
        campagne.setLibelle(requete.libelle());
        campagne.setDateEffet(requete.dateEffet());
        campagne.setEnveloppeBudgetaire(requete.enveloppeBudgetaire());
        campagne.setPopulation(requete.population());
        campagne.setRegle(requete.regle());
        CampagneAugmentation enregistree = campagnes.save(campagne);
        return CarriereMobiliteDtos.CampagneDto.de(enregistree, Map.of());
    }

    @Transactional
    public CarriereMobiliteDtos.CampagneDto simulerCampagne(Long id) {
        CampagneAugmentation campagne = campagnes.findById(id)
                .orElseThrow(() -> BusinessException.notFound("La campagne " + id + " n'existe pas."));
        Map<String, Object> simulation = simuler(campagne);
        campagne.setSimulation(json(simulation));
        campagnes.save(campagne);
        audit.enregistrer("CAMPAGNE_SIMULEE", "campagne_augmentation", id, null, campagne.getSimulation());
        return CarriereMobiliteDtos.CampagneDto.de(campagne, simulation);
    }

    @Transactional
    public int appliquerMouvementsPlanifies() {
        LocalDate aujourdHui = LocalDate.now();
        List<MouvementCarriere> planifies = mouvements
                .findByStatutAndDateEffetLessThanEqual(StatutMouvementCarriere.PLANIFIE, aujourdHui);
        for (MouvementCarriere mouvement : planifies) {
            appliquer(mouvement.getId());
        }
        return planifies.size();
    }

    @Transactional
    public int traiterRetoursPlanifies() {
        LocalDate aujourdHui = LocalDate.now();
        List<Mutation> retours = mutations.findByDateRetourPrevueIsNotNull().stream()
                .filter(m -> !m.getDateRetourPrevue().isAfter(aujourdHui))
                .toList();
        int traites = 0;
        for (Mutation mutation : retours) {
            MouvementCarriere mouvement = mouvements.findById(mutation.getIdMouvement()).orElse(null);
            if (mouvement == null || mouvement.getStatut() != StatutMouvementCarriere.APPLIQUE) {
                continue;
            }
            Map<String, Object> contexte = lireMap(mouvement.getConditionsVerifiees());
            Object idPoste = contexte.get("idPosteOrigine");
            if (idPoste == null) {
                continue;
            }
            contrats.appliquerMobilite(mouvement.getIdEmploye(), new ContratDtos.AffectationMobiliteRequest(
                    ((Number) idPoste).longValue(), null, mutation.getIdServiceOrigine(),
                    mutation.getIdManagerOrigine(), mutation.getIdLieuOrigine(), mutation.getDateRetourPrevue(),
                    null, "REINTEGRATION", null, null, null, mouvement.getId()), null);
            historique.enregistrerEvenementSimple(mouvement.getIdEmploye(),
                    new com.tovo.erp.contrat.dto.CarriereDtos.EvenementSimpleRequest("REPRISE",
                            mutation.getDateRetourPrevue(), null, null, "Retour de mutation temporaire."), null);
            traites++;
        }
        return traites;
    }

    private void enregistrerDetails(MouvementCarriere mouvement, CarriereMobiliteDtos.MouvementRequest requete) {
        if (requete.type() == com.tovo.erp.carriere.model.TypeMouvementCarriere.PROMOTION) {
            var p = Objects.requireNonNull(requete.promotion(), "La promotion est obligatoire.");
            Promotion promotion = new Promotion();
            promotion.setIdMouvement(mouvement.getId());
            promotion.setNature(p.nature());
            promotion.setIdAncienPoste(p.idAncienPoste());
            promotion.setIdNouveauPoste(p.idNouveauPoste());
            promotion.setAncienneCategorie(p.ancienneCategorie());
            promotion.setNouvelleCategorie(p.nouvelleCategorie());
            promotion.setAncienEchelon(p.ancienEchelon());
            promotion.setNouveauEchelon(p.nouveauEchelon());
            promotion.setAncienSalaire(p.ancienSalaire());
            promotion.setNouveauSalaire(p.nouveauSalaire());
            promotions.save(promotion);
        }
        if (requete.type() == com.tovo.erp.carriere.model.TypeMouvementCarriere.MUTATION
                || requete.type() == com.tovo.erp.carriere.model.TypeMouvementCarriere.DETACHEMENT) {
            var m = Objects.requireNonNull(requete.mutation(), "La mutation est obligatoire.");
            Mutation mutation = new Mutation();
            mutation.setIdMouvement(mouvement.getId());
            mutation.setType(m.type());
            mutation.setInitiative(m.initiative());
            mutation.setIdPosteOrigine(m.idPosteOrigine());
            mutation.setIdPosteDestination(m.idPosteDestination());
            mutation.setIdServiceOrigine(m.idServiceOrigine());
            mutation.setIdServiceDestination(m.idServiceDestination());
            mutation.setIdLieuOrigine(m.idLieuOrigine());
            mutation.setIdLieuDestination(m.idLieuDestination());
            mutation.setIdManagerOrigine(m.idManagerOrigine());
            mutation.setIdManagerDestination(m.idManagerDestination());
            mutation.setDateRetourPrevue(m.dateRetourPrevue());
            mutation.setClauseMobiliteVerifiee(m.clauseMobiliteVerifiee());
            mutation.setAccordSalarieDocumentId(m.accordSalarieDocumentId());
            mutation.setIndemniteMobilite(m.indemniteMobilite());
            mutations.save(mutation);
        }
    }

    private Map<String, Object> controles(MouvementCarriere mouvement) {
        Map<String, Object> resultat = new LinkedHashMap<>();
        LocalDate dateEffet = mouvement.getDateEffetSouhaitee();
        ContratDtos.AffectationDto affectationCourante = contrats.affectationsDe(mouvement.getIdEmploye()).stream()
                .filter(a -> a.principale())
                .filter(a -> !a.dateDebut().isAfter(dateEffet))
                .filter(a -> a.dateFin() == null || !a.dateFin().isBefore(dateEffet))
                .findFirst()
                .orElse(null);
        resultat.put("employeExiste", employes.existe(mouvement.getIdEmploye()));
        resultat.put("dateEffetCoherente", contrats.contratsDe(mouvement.getIdEmploye()).stream()
                .anyMatch(c -> !c.dateDebut().isAfter(dateEffet)
                        && (c.dateFinPrevue() == null || !c.dateFinPrevue().isBefore(dateEffet))));
        resultat.put("retroactiviteAutorisee",
                ChronoUnit.MONTHS.between(dateEffet, LocalDate.now()) <= RETROACTIVITE_MAX_MOIS);

        Long idPosteCible = idPosteCible(mouvement);
        if (idPosteCible != null) {
            PosteDto.Liste poste = postes.trouver(idPosteCible);
            resultat.put("posteActif", poste.actif());
            long occupants = employes.actifsDuPoste(idPosteCible).size();
            resultat.put("capaciteDisponible", poste.effectifAutorise() == null
                    || occupants < poste.effectifAutorise());
            List<EvaluationDtos.EcartDto> ecarts = evaluations.ecartsEmploye(mouvement.getIdEmploye()).stream()
                    .filter(e -> !"COMBLE".equals(e.statut()) && "HAUTE".equals(e.priorite()))
                    .toList();
            resultat.put("ecartCritique", ecarts.isEmpty());
        } else {
            resultat.put("posteActif", true);
            resultat.put("capaciteDisponible", true);
            resultat.put("ecartCritique", true);
        }

        long mois = affectationCourante == null ? 0
                : ChronoUnit.MONTHS.between(affectationCourante.dateDebut(), dateEffet);
        resultat.put("ancienneteSuffisante", mois >= ANCIENNETE_MINIMALE_MOIS);
        BigDecimal note = evaluations.derniereNoteGlobale(mouvement.getIdEmploye());
        resultat.put("evaluationSuffisante", note != null && note.compareTo(NOTE_MINIMALE) >= 0);
        resultat.put("sanctionActive", !eligibiliteDiscipline.sanctionActive(mouvement.getIdEmploye()));

        var promotion = promotions.findByIdMouvement(mouvement.getId()).orElse(null);
        resultat.put("niveauSuperieur", promotion == null
                || !Objects.equals(promotion.getIdAncienPoste(), promotion.getIdNouveauPoste())
                || !Objects.equals(promotion.getAncienneCategorie(), promotion.getNouvelleCategorie())
                || !Objects.equals(promotion.getAncienEchelon(), promotion.getNouveauEchelon()));
        resultat.put("salaireNonDecroissant", promotion == null
                || promotion.getNouveauSalaire().compareTo(promotion.getAncienSalaire()) >= 0);

        BigDecimal salaire = salaireCible(mouvement);
        if (salaire != null && salaire.signum() > 0) {
            GrilleSalariale grille = trouverGrille(mouvement, salaire);
            resultat.put("salaireDansGrille", grille == null
                    || (salaire.compareTo(grille.getSalaireMin()) >= 0
                    && salaire.compareTo(grille.getSalaireMax()) <= 0));
        } else {
            resultat.put("salaireDansGrille", true);
        }
        if (affectationCourante != null) {
            Map<String, Object> contexte = new LinkedHashMap<>();
            contexte.put("idPosteOrigine", affectationCourante.idPoste());
            contexte.put("idServiceOrigine", affectationCourante.idService());
            contexte.put("idManagerOrigine", affectationCourante.idManager());
            contexte.put("lieuOrigine", affectationCourante.lieuTravail());
            resultat.put("contexte", contexte);
        }
        return resultat;
    }

    private boolean cleDerogeable(String cle, MouvementCarriere mouvement) {
        if (!mouvement.isDerogationDirection()) {
            return false;
        }
        return List.of("ancienneteSuffisante", "evaluationSuffisante", "ecartCritique",
                "sanctionActive", "salaireDansGrille", "capaciteDisponible",
                "retroactiviteAutorisee").contains(cle);
    }

    private MouvementAppliqueEvent evenementApplication(MouvementCarriere mouvement,
                                                        Map<String, Object> avant,
                                                        Map<String, Object> cible,
                                                        LocalDate dateEffet,
                                                        Long idUtilisateur) {
        boolean salaire = mouvement.getType() == com.tovo.erp.carriere.model.TypeMouvementCarriere.PROMOTION
                || mouvement.getType() == com.tovo.erp.carriere.model.TypeMouvementCarriere.CHANGEMENT_SALAIRE;
        return new MouvementAppliqueEvent(
                mouvement.getId(),
                mouvement.getIdEmploye(),
                mouvement.getType().name(),
                extraireLong(avant, "idPoste"),
                extraireLong(cible, "idPoste"),
                extraireLong(avant, "idService"),
                extraireLong(cible, "idService"),
                extraireLong(avant, "idManager"),
                extraireLong(cible, "idManager"),
                extraireTexte(avant, "lieuTravail"),
                extraireTexte(cible, "lieuTravail"),
                dateEffet,
                salaire ? extraireDecimal(avant, "salaireBrut") : null,
                salaire ? extraireDecimal(cible, "salaireBrut") : null,
                idUtilisateur);
    }

    private Long extraireLong(Map<String, Object> source, String cle) {
        Object valeur = source.get(cle);
        return valeur instanceof Number nombre ? nombre.longValue() : null;
    }

    private String extraireTexte(Map<String, Object> source, String cle) {
        Object valeur = source.get(cle);
        return valeur == null ? null : String.valueOf(valeur);
    }

    private BigDecimal extraireDecimal(Map<String, Object> source, String cle) {
        Object valeur = source.get(cle);
        if (valeur == null) {
            return null;
        }
        return valeur instanceof BigDecimal decimal ? decimal : new BigDecimal(valeur.toString());
    }

    private Map<String, Object> situationActuelle(Long idEmploye) {
        Map<String, Object> situation = new LinkedHashMap<>();
        contrats.affectationsDe(idEmploye).stream()
                .filter(ContratDtos.AffectationDto::principale)
                .findFirst()
                .ifPresent(a -> {
                    situation.put("idPoste", a.idPoste());
                    situation.put("idDepartement", a.idDepartement());
                    situation.put("idService", a.idService());
                    situation.put("idManager", a.idManager());
                    situation.put("lieuTravail", a.lieuTravail());
                    situation.put("categorie", a.categorie());
                    situation.put("echelon", a.echelon());
                });
        var salaire = historique.salaireCourant(idEmploye);
        if (salaire != null) {
            situation.put("salaireBrut", salaire.montantBrut());
            situation.put("devise", salaire.devise());
        }
        return situation;
    }

    private Map<String, Object> situationCible(MouvementCarriere mouvement) {
        Map<String, Object> situation = new LinkedHashMap<>();
        situation.put("type", mouvement.getType().name());
        MovementDetails details = resolveTarget(mouvement);
        situation.put("idPoste", details.idPoste());
        situation.put("idService", details.idService());
        situation.put("idManager", details.idManager());
        situation.put("lieuTravail", details.lieu());
        situation.put("salaireBrut", details.salaire());
        return situation;
    }

    private void appliquerPromotion(MouvementCarriere mouvement, LocalDate dateEffet, Long idUtilisateur) {
        Promotion promotion = promotions.findByIdMouvement(mouvement.getId())
                .orElseThrow(() -> BusinessException.notFound("Promotion introuvable."));
        if (promotion.getNouveauSalaire().compareTo(promotion.getAncienSalaire()) < 0) {
            throw new BusinessException("PROMOTION_SALAIRE_INFERIEUR",
                    "Une promotion ne peut pas reduire le salaire.");
        }
        BigDecimal variation = promotion.getAncienSalaire().signum() == 0 ? BigDecimal.ZERO
                : promotion.getNouveauSalaire().subtract(promotion.getAncienSalaire())
                        .multiply(CENT).divide(promotion.getAncienSalaire(), 4, RoundingMode.HALF_UP);
        var courant = historique.salaireCourant(mouvement.getIdEmploye());
        contrats.appliquerMobilite(mouvement.getIdEmploye(), new ContratDtos.AffectationMobiliteRequest(
                promotion.getIdNouveauPoste(), null, null, null, null, dateEffet, null,
                "PROMOTION", promotion.getNouvelleCategorie(), null, promotion.getNouveauEchelon(),
                mouvement.getId()), idUtilisateur);
        historique.enregistrerSalaire(mouvement.getIdEmploye(),
                new com.tovo.erp.contrat.dto.CarriereDtos.SalaireRequest(
                        promotion.getNouveauSalaire(), courant == null ? "MGA" : courant.devise(),
                        dateEffet, "PROMOTION", courant == null ? null : courant.idContrat(),
                        mouvement.getId(), mouvement.getIdAvenant(), promotion.getAncienSalaire(),
                        variation, idUtilisateur), idUtilisateur);
        historique.enregistrerEvenementSimple(mouvement.getIdEmploye(),
                new com.tovo.erp.contrat.dto.CarriereDtos.EvenementSimpleRequest("PROMOTION", dateEffet,
                        promotion.getAncienSalaire().toPlainString(),
                        promotion.getNouveauSalaire().toPlainString(), mouvement.getMotif()), idUtilisateur);
    }

    private void appliquerMutation(MouvementCarriere mouvement, LocalDate dateEffet, Long idUtilisateur) {
        Mutation mutation = mutations.findByIdMouvement(mouvement.getId())
                .orElseThrow(() -> BusinessException.notFound("Mutation introuvable."));
        if (("TEMPORAIRE".equals(mutation.getType()) || "DETACHEMENT".equals(mutation.getType()))
                && mutation.getDateRetourPrevue() == null) {
            throw new BusinessException("DATE_RETOUR_OBLIGATOIRE",
                    "Une mutation temporaire exige une date de retour prevue.");
        }
        MovementDetails details = MovementDetails.of(mutation);
        contrats.appliquerMobilite(mouvement.getIdEmploye(), new ContratDtos.AffectationMobiliteRequest(
                details.idPoste(), null, mutation.getIdServiceDestination(),
                mutation.getIdManagerDestination(), mutation.getIdLieuDestination(), dateEffet,
                null, mouvement.getType() == com.tovo.erp.carriere.model.TypeMouvementCarriere.CHANGEMENT_POSTE
                        ? "CHANGEMENT_POSTE" : "MUTATION",
                null, null, null, mouvement.getId()), idUtilisateur);
        historique.enregistrerEvenementSimple(mouvement.getIdEmploye(),
                new com.tovo.erp.contrat.dto.CarriereDtos.EvenementSimpleRequest("MUTATION", dateEffet,
                        null, null, mouvement.getMotif()), idUtilisateur);
        mouvement.setConditionsVerifiees(json(details.contexte()));
    }

    private void appliquerChangementSalaire(MouvementCarriere mouvement, LocalDate dateEffet,
                                            Long idUtilisateur) {
        BigDecimal nouveau = lireMap(mouvement.getConditionsVerifiees()).containsKey("nouveauSalaire")
                ? new BigDecimal(lireMap(mouvement.getConditionsVerifiees()).get("nouveauSalaire").toString())
                : null;
        if (nouveau == null) {
            throw new BusinessException("SALAIRE_MANQUANT", "Le nouveau salaire est obligatoire.");
        }
        var courant = historique.salaireCourant(mouvement.getIdEmploye());
        BigDecimal ancien = courant == null ? BigDecimal.ZERO : courant.montantBrut();
        BigDecimal variation = ancien.signum() == 0 ? BigDecimal.ZERO
                : nouveau.subtract(ancien).multiply(CENT).divide(ancien, 4, RoundingMode.HALF_UP);
        historique.enregistrerSalaire(mouvement.getIdEmploye(),
                new com.tovo.erp.contrat.dto.CarriereDtos.SalaireRequest(
                        nouveau, courant == null ? "MGA" : courant.devise(), dateEffet,
                        "AUGMENTATION_INDIVIDUELLE", courant == null ? null : courant.idContrat(),
                        mouvement.getId(), mouvement.getIdAvenant(), ancien, variation, idUtilisateur),
                idUtilisateur);
    }

    private MovementDetails resolveTarget(MouvementCarriere mouvement) {
        Promotion promotion = promotions.findByIdMouvement(mouvement.getId()).orElse(null);
        if (promotion != null) {
            return new MovementDetails(promotion.getIdNouveauPoste(), null, null, null,
                    promotion.getNouveauSalaire(), Map.of());
        }
        Mutation mutation = mutations.findByIdMouvement(mouvement.getId()).orElse(null);
        if (mutation != null) {
            return MovementDetails.of(mutation);
        }
        return new MovementDetails(null, null, null, null, salaireCible(mouvement), Map.of());
    }

    private record MovementDetails(Long idPoste, Long idService, Long idManager, String lieu,
                                   BigDecimal salaire, Map<String, Object> contexte) {

        static MovementDetails of(Mutation mutation) {
            Map<String, Object> contexte = new LinkedHashMap<>();
            contexte.put("idPosteOrigine", mutation.getIdPosteOrigine());
            contexte.put("idServiceOrigine", mutation.getIdServiceOrigine());
            contexte.put("idManagerOrigine", mutation.getIdManagerOrigine());
            contexte.put("lieuOrigine", mutation.getIdLieuOrigine());
            return new MovementDetails(mutation.getIdPosteDestination(), mutation.getIdServiceDestination(),
                    mutation.getIdManagerDestination(), mutation.getIdLieuDestination(),
                    mutation.getIndemniteMobilite(), contexte);
        }
    }

    private Long idPosteCible(MouvementCarriere mouvement) {
        Promotion promotion = promotions.findByIdMouvement(mouvement.getId()).orElse(null);
        if (promotion != null) {
            return promotion.getIdNouveauPoste();
        }
        Mutation mutation = mutations.findByIdMouvement(mouvement.getId()).orElse(null);
        return mutation == null ? null : mutation.getIdPosteDestination();
    }

    private BigDecimal salaireCible(MouvementCarriere mouvement) {
        Promotion promotion = promotions.findByIdMouvement(mouvement.getId()).orElse(null);
        if (promotion != null) {
            return promotion.getNouveauSalaire();
        }
        return lireMap(mouvement.getConditionsVerifiees()).containsKey("nouveauSalaire")
                ? new BigDecimal(lireMap(mouvement.getConditionsVerifiees()).get("nouveauSalaire").toString())
                : BigDecimal.ZERO;
    }

    private GrilleSalariale trouverGrille(MouvementCarriere mouvement, BigDecimal salaire) {
        Promotion promotion = promotions.findByIdMouvement(mouvement.getId()).orElse(null);
        if (promotion == null || promotion.getNouvelleCategorie() == null
                || promotion.getNouveauEchelon() == null) {
            return null;
        }
        return grilles.findByCategorieAndEchelonOrderByDateEffetDesc(
                        promotion.getNouvelleCategorie(), promotion.getNouveauEchelon()).stream()
                .filter(g -> !g.getDateEffet().isAfter(mouvement.getDateEffetSouhaitee()))
                .filter(g -> g.getDateFin() == null || !g.getDateFin().isBefore(mouvement.getDateEffetSouhaitee()))
                .findFirst()
                .orElse(null);
    }

    private Map<String, Object> simuler(CampagneAugmentation campagne) {
        Map<String, Object> resultat = new LinkedHashMap<>();
        try {
            JsonNode regle = MAPPER.readTree(campagne.getRegle());
            BigDecimal taux = regle.has("taux")
                    ? new BigDecimal(regle.get("taux").asText()) : BigDecimal.ZERO;
            Map<Long, BigDecimal> salaires = historique.salairesBaseParIds(
                    employes.actifs().stream().map(e -> e.id()).toList());
            List<Map<String, Object>> lignes = new ArrayList<>();
            BigDecimal impactAnnuel = BigDecimal.ZERO;
            for (Map.Entry<Long, BigDecimal> entree : salaires.entrySet()) {
                BigDecimal nouveau = entree.getValue().multiply(BigDecimal.ONE.add(taux))
                        .setScale(2, RoundingMode.HALF_UP);
                BigDecimal delta = nouveau.subtract(entree.getValue());
                impactAnnuel = impactAnnuel.add(delta.multiply(BigDecimal.valueOf(12)));
                Map<String, Object> ligne = new LinkedHashMap<>();
                ligne.put("idEmploye", entree.getKey());
                ligne.put("ancienSalaire", entree.getValue());
                ligne.put("nouveauSalaire", nouveau);
                ligne.put("delta", delta);
                lignes.add(ligne);
            }
            resultat.put("lignes", lignes);
            resultat.put("impactAnnuel", impactAnnuel);
            resultat.put("depasseEnveloppe", impactAnnuel.compareTo(campagne.getEnveloppeBudgetaire()) > 0);
        } catch (Exception e) {
            throw new BusinessException("REGLE_CAMPAGNE_INVALIDE", "La regle de campagne est invalide.");
        }
        return resultat;
    }

    private CarriereMobiliteDtos.MouvementDto enDto(MouvementCarriere mouvement) {
        Map<String, Object> conditions = lireMap(mouvement.getConditionsVerifiees());
        Map<String, Object> details = new LinkedHashMap<>();
        promotions.findByIdMouvement(mouvement.getId()).ifPresent(p -> {
            details.put("id", p.getId());
            details.put("nature", p.getNature());
            details.put("idAncienPoste", p.getIdAncienPoste());
            details.put("idNouveauPoste", p.getIdNouveauPoste());
            details.put("ancienSalaire", p.getAncienSalaire());
            details.put("nouveauSalaire", p.getNouveauSalaire());
        });
        mutations.findByIdMouvement(mouvement.getId()).ifPresent(m -> {
            details.put("id", m.getId());
            details.put("type", m.getType());
            details.put("initiative", m.getInitiative());
            details.put("idServiceDestination", m.getIdServiceDestination());
            details.put("dateRetourPrevue", m.getDateRetourPrevue());
        });
        return CarriereMobiliteDtos.MouvementDto.de(mouvement, conditions, details);
    }

    private MouvementCarriere mouvementEntite(Long id) {
        return mouvements.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Le mouvement " + id + " n'existe pas."));
    }

    private boolean visible(LocalDate debut, LocalDate fin, LocalDateTime enregistreLe,
                            LocalDateTime remplaceLe, LocalDate dateValidite, LocalDate connaissance) {
        boolean validite = !debut.isAfter(dateValidite)
                && (fin == null || !fin.isBefore(dateValidite));
        boolean connaissanceOk = enregistreLe == null || !enregistreLe.toLocalDate().isAfter(connaissance);
        boolean nonRemplacee = remplaceLe == null || remplaceLe.toLocalDate().isAfter(connaissance);
        return validite && connaissanceOk && nonRemplacee;
    }

    private void verifierSeparationDesTaches(MouvementCarriere mouvement,
                                             com.tovo.erp.shared.UtilisateurConnecte acteur) {
        if (mouvement.getIdDemandeur() != null && mouvement.getIdDemandeur().equals(acteur.getId())) {
            throw BusinessException.forbidden("Le demandeur ne peut pas valider sa propre demande.");
        }
        employes.idParUtilisateur(acteur.getId()).filter(mouvement.getIdEmploye()::equals)
                .ifPresent(u -> {
                    throw BusinessException.forbidden("Le beneficiaire ne peut pas valider son mouvement.");
                });
    }

    private List<Map<String, Object>> circuitValidation(MouvementCarriere mouvement) {
        List<String> roles = new ArrayList<>();
        if (mouvement.getIdCampagne() != null) {
            roles.addAll(List.of(Roles.RH, Roles.FINANCE, Roles.DIRECTION));
        } else if (baisseSalaire(mouvement)) {
            roles.addAll(List.of(Roles.RH, Roles.DIRECTION));
        } else {
            switch (mouvement.getType()) {
                case PROMOTION -> roles.addAll(List.of(Roles.MANAGER, Roles.RH, Roles.FINANCE, Roles.DIRECTION));
                case CHANGEMENT_SALAIRE -> {
                    roles.addAll(List.of(Roles.MANAGER, Roles.RH, Roles.FINANCE));
                    if (variationMajeure(mouvement)) {
                        roles.add(Roles.DIRECTION);
                    }
                }
                case MUTATION, DETACHEMENT -> {
                    roles.addAll(List.of(Roles.MANAGER, Roles.RH));
                    if (geographiqueOuTemporaire(mouvement)) {
                        roles.add(Roles.DIRECTION);
                    }
                }
                case CHANGEMENT_POSTE -> roles.addAll(List.of(Roles.MANAGER, Roles.RH));
            }
        }
        List<Map<String, Object>> etapes = new ArrayList<>();
        for (String role : roles) {
            Map<String, Object> etape = new LinkedHashMap<>();
            etape.put("role", role);
            etape.put("statut", "EN_ATTENTE");
            etapes.add(etape);
        }
        return etapes;
    }

    private boolean geographiqueOuTemporaire(MouvementCarriere mouvement) {
        var mutation = mutations.findByIdMouvement(mouvement.getId()).orElse(null);
        if (mutation == null) {
            return false;
        }
        return List.of("GEOGRAPHIQUE", "TEMPORAIRE", "DETACHEMENT").contains(mutation.getType());
    }

    private boolean baisseSalaire(MouvementCarriere mouvement) {
        var courant = historique.salaireCourant(mouvement.getIdEmploye());
        BigDecimal cible = salaireCible(mouvement);
        return courant != null && cible != null && cible.signum() > 0
                && cible.compareTo(courant.montantBrut()) < 0;
    }

    private boolean variationMajeure(MouvementCarriere mouvement) {
        var courant = historique.salaireCourant(mouvement.getIdEmploye());
        BigDecimal cible = salaireCible(mouvement);
        if (courant == null || courant.montantBrut().signum() == 0 || cible == null) {
            return false;
        }
        return cible.subtract(courant.montantBrut())
                .multiply(CENT).divide(courant.montantBrut(), 4, java.math.RoundingMode.HALF_UP)
                .compareTo(SEUIL_VALIDATION_MAJEURE) > 0;
    }

    private boolean exigeAvenantSigne(MouvementCarriere mouvement) {
        if (baisseSalaire(mouvement)) {
            return true;
        }
        var mutation = mutations.findByIdMouvement(mouvement.getId()).orElse(null);
        if (mutation == null || !"EMPLOYEUR".equals(mutation.getInitiative())) {
            return false;
        }
        if (List.of("TEMPORAIRE", "DETACHEMENT").contains(mutation.getType())) {
            return true;
        }
        return !Objects.equals(mutation.getIdPosteOrigine(), mutation.getIdPosteDestination())
                || !Objects.equals(mutation.getIdServiceOrigine(), mutation.getIdServiceDestination())
                || !Objects.equals(mutation.getIdManagerOrigine(), mutation.getIdManagerDestination())
                || !Objects.equals(mutation.getIdLieuOrigine(), mutation.getIdLieuDestination());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> circuitLisible(Map<String, Object> conditions,
                                                     MouvementCarriere mouvement) {
        Object circuit = conditions.get("circuitValidation");
        if (circuit instanceof List<?> liste) {
            return (List<Map<String, Object>>) liste;
        }
        return circuitValidation(mouvement);
    }

    private int etapeCourante(Map<String, Object> conditions) {
        Object valeur = conditions.get("etapeCourante");
        return valeur instanceof Number nombre ? nombre.intValue() : 0;
    }

    private void exigerStatut(MouvementCarriere mouvement, StatutMouvementCarriere... attendus) {
        boolean ok = List.of(attendus).contains(mouvement.getStatut());
        if (!ok) {
            throw new BusinessException("MOUVEMENT_STATUT_INVALIDE",
                    "Transition impossible depuis le statut " + mouvement.getStatut() + ".");
        }
    }

    private StatutMouvementCarriere statut(String valeur) {
        try {
            return StatutMouvementCarriere.valueOf(valeur);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("STATUT_MOUVEMENT_INVALIDE", "Statut de mouvement inconnu : " + valeur);
        }
    }

    private String json(Object valeur) {
        try {
            return MAPPER.writeValueAsString(valeur);
        } catch (Exception e) {
            throw new BusinessException("SERIALISATION_IMPOSSIBLE", "Impossible de serialiser les donnees.");
        }
    }

    private Map<String, Object> lireMap(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return Map.of();
        }
        try {
            return MAPPER.readValue(valeur, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }
}
