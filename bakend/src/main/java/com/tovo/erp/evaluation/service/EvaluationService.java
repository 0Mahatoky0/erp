package com.tovo.erp.evaluation.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tovo.erp.contrat.dto.ContratDtos;
import com.tovo.erp.contrat.service.ContratService;
import com.tovo.erp.evaluation.dto.EvaluationDtos;
import com.tovo.erp.evaluation.model.BesoinFormation;
import com.tovo.erp.evaluation.model.CampagneEvaluation;
import com.tovo.erp.evaluation.model.EcartCompetence;
import com.tovo.erp.evaluation.model.Evaluation;
import com.tovo.erp.evaluation.model.Objectif;
import com.tovo.erp.evaluation.repository.BesoinFormationRepository;
import com.tovo.erp.evaluation.repository.EcartCompetenceRepository;
import com.tovo.erp.evaluation.repository.EvaluationRepository;
import com.tovo.erp.evaluation.repository.ObjectifRepository;
import com.tovo.erp.employe.dto.EmployeEmbaucheDto;
import com.tovo.erp.employe.service.EmployeService;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class EvaluationService {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final BigDecimal CINQ = BigDecimal.valueOf(5);
    private static final BigDecimal CENT = BigDecimal.valueOf(100);
    private static final BigDecimal POIDS_OBJECTIFS = new BigDecimal("0.50");
    private static final BigDecimal POIDS_COMPETENCES = new BigDecimal("0.30");
    private static final BigDecimal POIDS_COMPORTEMENT = new BigDecimal("0.20");

    private final EvaluationRepository evaluations;
    private final ObjectifRepository objectifs;
    private final EcartCompetenceRepository ecarts;
    private final BesoinFormationRepository besoins;
    private final CampagneService campagnes;
    private final ReferentielService referentiel;
    private final EmployeService employes;
    private final ContratService contrats;
    private final UtilisateurCourant utilisateurCourant;
    private final JournalAuditService audit;

    public EvaluationService(EvaluationRepository evaluations, ObjectifRepository objectifs,
                             EcartCompetenceRepository ecarts, BesoinFormationRepository besoins,
                             CampagneService campagnes, ReferentielService referentiel,
                             EmployeService employes, ContratService contrats,
                             UtilisateurCourant utilisateurCourant, JournalAuditService audit) {
        this.evaluations = evaluations;
        this.objectifs = objectifs;
        this.ecarts = ecarts;
        this.besoins = besoins;
        this.campagnes = campagnes;
        this.referentiel = referentiel;
        this.employes = employes;
        this.contrats = contrats;
        this.utilisateurCourant = utilisateurCourant;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<EvaluationDtos.EvaluationDto> evaluations(Long idCampagne) {
        return evaluations.findByIdCampagneOrderByIdEmployeAsc(idCampagne).stream()
                .filter(this::peutLire)
                .map(EvaluationDtos.EvaluationDto::de).toList();
    }

    @Transactional(readOnly = true)
    public List<EvaluationDtos.EvaluationDto> mesEvaluations() {
        var utilisateur = utilisateurCourant.exiger();
        Long idEmploye = employes.idParUtilisateur(utilisateur.getId())
                .orElseThrow(() -> BusinessException.notFound("Profil salarié introuvable."));
        return evaluations.findByIdEmployeOrderByDateCreationDesc(idEmploye).stream()
                .map(EvaluationDtos.EvaluationDto::de).toList();
    }

    @Transactional(readOnly = true)
    public BigDecimal derniereNoteGlobale(Long idEmploye) {
        return evaluations.findByIdEmployeOrderByDateCreationDesc(idEmploye).stream()
                .filter(e -> "CLOTUREE".equals(e.getStatut()) || "SIGNEE".equals(e.getStatut()))
                .map(Evaluation::getNoteGlobale)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public EvaluationDtos.EvaluationDto evaluation(Long idEvaluation) {
        Evaluation evaluation = evaluationEntity(idEvaluation);
        verifierAcces(evaluation);
        return EvaluationDtos.EvaluationDto.de(evaluation);
    }

    @Transactional
    public EvaluationDtos.EvaluationDto demarrer(Long idCampagne, Long idEmploye) {
        CampagneEvaluation campagne = campagnes.campagne(idCampagne);
        if (evaluations.findTopByIdEmployeAndIdCampagneOrderByVersionDesc(idEmploye, idCampagne).isPresent()) {
            throw new BusinessException("EVALUATION_EXISTANTE",
                    "Une évaluation existe déjà pour ce salarié et cette campagne.");
        }
        EmployeEmbaucheDto employe = employes.dossier(idEmploye);
        LocalDate reference = employe.datePremiereEmbauche() == null
                ? employe.dateEmbauche() : employe.datePremiereEmbauche();
        if (reference == null || reference.isAfter(campagne.getDateOuverture().minusMonths(3))) {
            throw new BusinessException("EVALUATION_INELIGIBLE",
                    "Le salarié doit avoir au moins trois mois d'ancienneté à l'ouverture de la campagne.");
        }
        ContratDtos.AffectationDto affectation = contrats.affectationsDe(idEmploye).stream()
                .filter(a -> a.dateFin() == null && a.principale())
                .findFirst().orElseGet(() -> contrats.affectationsDe(idEmploye).stream()
                        .filter(a -> a.dateFin() == null).findFirst().orElse(null));
        Long evaluateur = affectation == null ? null : affectation.idManager();
        if (evaluateur == null) {
            throw new BusinessException("EVALUATEUR_INTROUVABLE",
                    "Aucun manager actif n'est rattaché à l'affectation de ce salarié.");
        }
        Evaluation evaluation = new Evaluation();
        evaluation.setIdEmploye(idEmploye);
        evaluation.setIdCampagne(idCampagne);
        evaluation.setIdEvaluateur(evaluateur);
        evaluation.setStatut(Evaluation.Statut.BROUILLON.name());
        EvaluationDtos.EvaluationDto cree = EvaluationDtos.EvaluationDto.de(evaluations.save(evaluation));
        audit.enregistrer("EVALUATION_CREE", "evaluation", cree.id(), null, cree.toString());
        return cree;
    }

    @Transactional
    public EvaluationDtos.EvaluationDto autoEvaluer(Long idEvaluation, EvaluationDtos.AutoEvaluationRequest request) {
        Evaluation evaluation = evaluationEntity(idEvaluation);
        exigerEmploye(evaluation.getIdEmploye());
        verifierStatut(evaluation, Evaluation.Statut.BROUILLON, Evaluation.Statut.AUTO_EVALUATION);
        Map<String, Object> contenu = new HashMap<>();
        contenu.put("commentaire", request.commentaire());
        contenu.put("pointsForts", request.pointsForts());
        contenu.put("axesProgres", request.axesProgres());
        evaluation.setAutoEvaluation(json(contenu));
        evaluation.setPointsForts(request.pointsForts());
        evaluation.setAxesProgres(request.axesProgres());
        evaluation.setStatut(Evaluation.Statut.AUTO_EVALUATION.name());
        EvaluationDtos.EvaluationDto modifiee = EvaluationDtos.EvaluationDto.de(evaluations.save(evaluation));
        audit.enregistrer("AUTO_EVALUATION", "evaluation", idEvaluation, null, modifiee.toString());
        return modifiee;
    }

    @Transactional
    public EvaluationDtos.EvaluationDto evaluerManager(Long idEvaluation,
                                                        EvaluationDtos.EvaluationManagerRequest request) {
        Evaluation evaluation = evaluationEntity(idEvaluation);
        UtilisateurConnecte utilisateur = utilisateurCourant.exiger();
        if (!utilisateur.getId().equals(evaluation.getIdEvaluateur()) && !estRhOuDirection(utilisateur)) {
            throw BusinessException.forbidden("Seul l'évaluateur désigné peut saisir ces notations.");
        }
        verifierStatut(evaluation, Evaluation.Statut.AUTO_EVALUATION, Evaluation.Statut.EVALUATION_MANAGER,
                Evaluation.Statut.ENTRETIEN);
        if (request.notesComportement() == null || request.notesComportement().isEmpty()) {
            throw new BusinessException("GRILLE_COMPORTEMENT_MANQUANTE",
                    "Au moins un critère comportemental doit être noté.");
        }
        request.notesComportement().forEach((critere, note) -> {
            if (note == null || note < 1 || note > 5) {
                throw new BusinessException("NOTE_INVALIDE", "Les notes doivent être comprises entre 1 et 5.");
            }
            if ((note <= 2 || note >= 5) && estVide(request.pointsForts()) && estVide(request.axesProgres())) {
                throw new BusinessException("NOTE_EXTREME_SANS_COMMENTAIRE",
                        "Une note extrême (≤ 2 ou ≥ 5) exige un commentaire factuel.");
            }
        });
        Map<String, Object> contenu = new HashMap<>();
        contenu.put("notesComportement", request.notesComportement());
        contenu.put("appreciation", request.appreciation());
        evaluation.setEvaluationManager(json(contenu));
        evaluation.setPointsForts(request.pointsForts());
        evaluation.setAxesProgres(request.axesProgres());
        evaluation.setAppreciation(request.appreciation());
        calculerNotes(evaluation, request.notesComportement());
        evaluation.setStatut(Evaluation.Statut.EVALUATION_MANAGER.name());
        EvaluationDtos.EvaluationDto modifiee = EvaluationDtos.EvaluationDto.de(evaluations.save(evaluation));
        audit.enregistrer("EVALUATION_MANAGER", "evaluation", idEvaluation, null, modifiee.toString());
        return modifiee;
    }

    @Transactional
    public EvaluationDtos.EvaluationDto entretien(Long idEvaluation, EvaluationDtos.EntretienRequest request) {
        Evaluation evaluation = evaluationEntity(idEvaluation);
        UtilisateurConnecte utilisateur = utilisateurCourant.exiger();
        if (!utilisateur.getId().equals(evaluation.getIdEvaluateur()) && !estRhOuDirection(utilisateur)) {
            throw BusinessException.forbidden("Seul l'évaluateur désigné peut tenir l'entretien.");
        }
        verifierStatut(evaluation, Evaluation.Statut.EVALUATION_MANAGER, Evaluation.Statut.ENTRETIEN);
        evaluation.setDateEntretien(request.dateEntretien());
        evaluation.setCommentaireSalarie(request.compteRendu());
        evaluation.setStatut(Evaluation.Statut.ENTRETIEN.name());
        EvaluationDtos.EvaluationDto modifiee = EvaluationDtos.EvaluationDto.de(evaluations.save(evaluation));
        audit.enregistrer("ENTRETIEN_EVALUATION", "evaluation", idEvaluation, null, request.compteRendu());
        return modifiee;
    }

    @Transactional
    public EvaluationDtos.EvaluationDto validerN2(Long idEvaluation, EvaluationDtos.ValidationN2Request request) {
        Evaluation evaluation = evaluationEntity(idEvaluation);
        UtilisateurConnecte utilisateur = utilisateurCourant.exiger();
        boolean valideur = utilisateur.getId().equals(request.idValideurN2())
                || utilisateur.getId().equals(evaluation.getIdValideurN2());
        if (!valideur && !estRhOuDirection(utilisateur)) {
            throw BusinessException.forbidden("Seul le N+2 ou la RH peut valider cette évaluation.");
        }
        if (utilisateur.getId().equals(evaluation.getIdEvaluateur())) {
            throw new BusinessException("CONFLIT_INTERET", "Un évaluateur ne peut pas valider sa propre évaluation.");
        }
        verifierStatut(evaluation, Evaluation.Statut.ENTRETIEN, Evaluation.Statut.VALIDEE_N2);
        evaluation.setIdValideurN2(request.idValideurN2());
        evaluation.setStatut(Evaluation.Statut.VALIDEE_N2.name());
        EvaluationDtos.EvaluationDto modifiee = EvaluationDtos.EvaluationDto.de(evaluations.save(evaluation));
        audit.enregistrer("VALIDATION_N2", "evaluation", idEvaluation, null, request.commentaire());
        return modifiee;
    }

    @Transactional
    public EvaluationDtos.EvaluationDto signer(Long idEvaluation, EvaluationDtos.SignatureRequest request) {
        Evaluation evaluation = evaluationEntity(idEvaluation);
        exigerEmploye(evaluation.getIdEmploye());
        verifierStatut(evaluation, Evaluation.Statut.VALIDEE_N2, Evaluation.Statut.SIGNEE);
        evaluation.setCommentaireSalarie(request.commentaire());
        if (request.signer()) {
            evaluation.setDateSignatureSalarie(LocalDateTime.now());
            evaluation.setStatut(Evaluation.Statut.SIGNEE.name());
        } else {
            evaluation.setMotifContestation(request.commentaire());
            evaluation.setStatut(Evaluation.Statut.CONTESTEE.name());
        }
        EvaluationDtos.EvaluationDto modifiee = EvaluationDtos.EvaluationDto.de(evaluations.save(evaluation));
        audit.enregistrer("SIGNATURE_EVALUATION", "evaluation", idEvaluation, null,
                request.signer() ? "SIGNEE" : "CONTESTEE");
        return modifiee;
    }

    @Transactional
    public EvaluationDtos.EvaluationDto contester(Long idEvaluation, EvaluationDtos.ContestationRequest request) {
        Evaluation evaluation = evaluationEntity(idEvaluation);
        exigerEmploye(evaluation.getIdEmploye());
        verifierStatut(evaluation, Evaluation.Statut.SIGNEE);
        if (evaluation.getDateSignatureSalarie() == null
                || evaluation.getDateSignatureSalarie().plusDays(15).isBefore(LocalDateTime.now())) {
            throw new BusinessException("DELAI_CONTESTATION_DEPASSE",
                    "Le délai de 15 jours pour contester est dépassé.");
        }
        evaluation.setStatut(Evaluation.Statut.CONTESTEE.name());
        evaluation.setMotifContestation(request.motif());
        EvaluationDtos.EvaluationDto modifiee = EvaluationDtos.EvaluationDto.de(evaluations.save(evaluation));
        audit.enregistrer("CONTESTATION_EVALUATION", "evaluation", idEvaluation, null, request.motif());
        return modifiee;
    }

    @Transactional
    public EvaluationDtos.EvaluationDto nouvelleVersion(Long idEvaluation, String motif) {
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION);
        Evaluation precedent = evaluationEntity(idEvaluation);
        verifierStatut(precedent, Evaluation.Statut.CONTESTEE);
        Evaluation nouvelle = new Evaluation();
        nouvelle.setIdEmploye(precedent.getIdEmploye());
        nouvelle.setIdCampagne(precedent.getIdCampagne());
        nouvelle.setIdEvaluateur(precedent.getIdEvaluateur());
        nouvelle.setIdValideurN2(precedent.getIdValideurN2());
        nouvelle.setAutoEvaluation(precedent.getAutoEvaluation());
        nouvelle.setEvaluationManager(precedent.getEvaluationManager());
        nouvelle.setNoteObjectifs(precedent.getNoteObjectifs());
        nouvelle.setNoteCompetences(precedent.getNoteCompetences());
        nouvelle.setNoteComportement(precedent.getNoteComportement());
        nouvelle.setNoteGlobale(precedent.getNoteGlobale());
        nouvelle.setAppreciation(precedent.getAppreciation());
        nouvelle.setPointsForts(precedent.getPointsForts());
        nouvelle.setAxesProgres(precedent.getAxesProgres());
        nouvelle.setDateEntretien(precedent.getDateEntretien());
        nouvelle.setVersion(precedent.getVersion() + 1);
        nouvelle.setMotifContestation(motif);
        nouvelle.setStatut(Evaluation.Statut.VALIDEE_N2.name());
        EvaluationDtos.EvaluationDto cree = EvaluationDtos.EvaluationDto.de(evaluations.save(nouvelle));
        audit.enregistrer("EVALUATION_NOUVELLE_VERSION", "evaluation", cree.id(), motif, cree.toString());
        return cree;
    }

    @Transactional
    public EvaluationDtos.EvaluationDto clore(Long idEvaluation) {
        Evaluation evaluation = evaluationEntity(idEvaluation);
        verifierStatut(evaluation, Evaluation.Statut.SIGNEE);
        evaluation.setStatut(Evaluation.Statut.CLOTUREE.name());
        evaluation.setDateCloture(LocalDateTime.now());
        evaluations.save(evaluation);
        genererEcartsEtBesoins(evaluation);
        EvaluationDtos.EvaluationDto closee = EvaluationDtos.EvaluationDto.de(evaluation);
        audit.enregistrer("EVALUATION_CLOTUREE", "evaluation", idEvaluation, null, closee.toString());
        return closee;
    }

    @Transactional(readOnly = true)
    public List<EvaluationDtos.EcartDto> ecartsEmploye(Long idEmploye) {
        return ecarts.findByIdEmployeOrderByPrioriteScoreDesc(idEmploye).stream()
                .map(EvaluationDtos.EcartDto::de).toList();
    }

    @Transactional(readOnly = true)
    public List<EvaluationDtos.BesoinFormationDto> besoinsEmploye(Long idEmploye) {
        return besoins.findByIdEmployeOrderByDateCreationDesc(idEmploye).stream()
                .map(EvaluationDtos.BesoinFormationDto::de).toList();
    }

    @Transactional
    public EvaluationDtos.BesoinFormationDto creerBesoin(EvaluationDtos.BesoinFormationRequest request) {
        try {
            BesoinFormation.Origine.valueOf(request.origine().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("ORIGINE_BESOIN_INVALIDE", "Origine de besoin de formation invalide.");
        }
        BesoinFormation besoin = new BesoinFormation();
        besoin.setIdEmploye(request.idEmploye());
        besoin.setIdCompetence(request.idCompetence());
        besoin.setNiveauCible(request.niveauCible());
        besoin.setOrigine(request.origine().trim().toUpperCase());
        besoin.setPriorite("MOYENNE");
        besoin.setDateCible(request.dateCible());
        besoin.setBudgetEstime(request.budgetEstime());
        besoin.setStatut(BesoinFormation.Statut.IDENTIFIE.name());
        EvaluationDtos.BesoinFormationDto cree = EvaluationDtos.BesoinFormationDto.de(besoins.save(besoin));
        audit.enregistrer("BESOIN_FORMATION_CREE", "besoin_formation", cree.id(), null, cree.toString());
        return cree;
    }

    @Transactional
    public EvaluationDtos.BesoinFormationDto deciderBesoin(Long idBesoin,
                                                            EvaluationDtos.DecisionBesoinRequest request) {
        utilisateurCourant.exigerRole(Roles.MANAGER, Roles.RH, Roles.DIRECTION);
        BesoinFormation besoin = besoins.findById(idBesoin)
                .orElseThrow(() -> BusinessException.notFound("Besoin de formation introuvable."));
        besoin.setStatut(request.approuver()
                ? BesoinFormation.Statut.VALIDE.name() : BesoinFormation.Statut.ABANDONNE.name());
        besoin.setMotifRefus(request.motif());
        EvaluationDtos.BesoinFormationDto decidé = EvaluationDtos.BesoinFormationDto.de(besoins.save(besoin));
        audit.enregistrer("BESOIN_FORMATION_DECISION", "besoin_formation", idBesoin, null, decidé.statut());
        return decidé;
    }

    @Transactional
    public EvaluationDtos.EcartDto comblerEcart(Long idEcart, EvaluationDtos.EmployeCompetenceRequest request) {
        utilisateurCourant.exigerRole(Roles.MANAGER, Roles.RH, Roles.DIRECTION);
        EcartCompetence ecart = ecarts.findById(idEcart)
                .orElseThrow(() -> BusinessException.notFound("Écart de compétence introuvable."));
        referentiel.enregistrerNiveau(request, utilisateurCourant.exiger().getId());
        ecart.setStatut(EcartCompetence.Statut.COMBLE.name());
        audit.enregistrer("ECART_COMBLE", "ecart_competence", idEcart, null, request.toString());
        return EvaluationDtos.EcartDto.de(ecarts.save(ecart));
    }

    private void genererEcartsEtBesoins(Evaluation evaluation) {
        List<EvaluationDtos.PosteCompetenceDto> attendus = referentiel.posteCompetences(
                employes.posteDe(evaluation.getIdEmploye()).orElse(null));
        Map<Long, Integer> acquis = referentiel.niveauxAcquis(evaluation.getIdEmploye());
        for (EvaluationDtos.PosteCompetenceDto attendu : attendus) {
            int niveau = acquis.getOrDefault(attendu.idCompetence(), 1);
            int ecart = attendu.niveauRequis() - niveau;
            if (ecart <= 0) {
                continue;
            }
            BigDecimal score = BigDecimal.valueOf(ecart * attendu.poids());
            String priorite = priorite(score);
            EcartCompetence ligne = new EcartCompetence();
            ligne.setIdEmploye(evaluation.getIdEmploye());
            ligne.setIdCompetence(attendu.idCompetence());
            ligne.setIdEvaluation(evaluation.getId());
            ligne.setNiveauRequis(attendu.niveauRequis());
            ligne.setNiveauAcquis(niveau);
            ligne.setEcart(ecart);
            ligne.setPrioriteScore(score);
            ligne.setPriorite(priorite);
            ligne.setStatut(EcartCompetence.Statut.OUVERT.name());
            EcartCompetence enregistre = ecarts.save(ligne);
            if ("HAUTE".equals(priorite) || "MOYENNE".equals(priorite)) {
                BesoinFormation besoin = new BesoinFormation();
                besoin.setIdEmploye(evaluation.getIdEmploye());
                besoin.setIdCompetence(attendu.idCompetence());
                besoin.setNiveauCible(attendu.niveauRequis());
                besoin.setOrigine(BesoinFormation.Origine.ECART.name());
                besoin.setPriorite(priorite);
                besoin.setDateCible(campagnes.campagne(evaluation.getIdCampagne()).getDateCloture());
                besoin.setSourceEvaluationId(evaluation.getId());
                besoin.setStatut(BesoinFormation.Statut.IDENTIFIE.name());
                besoins.save(besoin);
            }
            audit.enregistrer("ECART_DETECTE", "ecart_competence", enregistre.getId(), null,
                    score.toPlainString());
        }
    }

    private void calculerNotes(Evaluation evaluation, Map<String, Integer> notesComportement) {
        CampagneEvaluation campagne = campagnes.campagne(evaluation.getIdCampagne());
        List<Objectif> objectifsCourants = campagnes.objectifsCourants(
                evaluation.getIdEmploye(), evaluation.getIdCampagne());
        BigDecimal sommePonderee = BigDecimal.ZERO;
        for (Objectif objectif : objectifsCourants) {
            BigDecimal taux = tauxAtteinte(objectif);
            objectif.setTauxAtteinte(taux.setScale(4, RoundingMode.HALF_UP));
            if (taux.compareTo(BigDecimal.ONE) >= 0) {
                objectif.setStatut(Objectif.Statut.ATTEINT.name());
            } else if (taux.signum() > 0) {
                objectif.setStatut(Objectif.Statut.PARTIEL.name());
            } else {
                objectif.setStatut(Objectif.Statut.NON_ATTEINT.name());
            }
            sommePonderee = sommePonderee.add(taux.min(BigDecimal.ONE)
                    .multiply(objectif.getPoids()).divide(CENT, 8, RoundingMode.HALF_UP));
        }
        objectifs.saveAll(objectifsCourants);
        BigDecimal noteObjectifs = sommePonderee.multiply(CINQ).setScale(2, RoundingMode.HALF_UP);

        List<EvaluationDtos.PosteCompetenceDto> attendus = referentiel.posteCompetences(
                employes.posteDe(evaluation.getIdEmploye()).orElse(null));
        Map<Long, Integer> acquis = referentiel.niveauxAcquis(evaluation.getIdEmploye());
        BigDecimal couvertureNumerateur = BigDecimal.ZERO;
        BigDecimal sommePoids = BigDecimal.ZERO;
        for (EvaluationDtos.PosteCompetenceDto attendu : attendus) {
            BigDecimal poids = BigDecimal.valueOf(attendu.poids());
            BigDecimal niveau = BigDecimal.valueOf(acquis.getOrDefault(attendu.idCompetence(), 1));
            BigDecimal requis = BigDecimal.valueOf(attendu.niveauRequis());
            couvertureNumerateur = couvertureNumerateur.add(niveau.divide(requis, 6, RoundingMode.HALF_UP)
                    .min(BigDecimal.ONE).multiply(poids));
            sommePoids = sommePoids.add(poids);
        }
        BigDecimal noteCompetences = sommePoids.signum() == 0 ? BigDecimal.ZERO
                : couvertureNumerateur.divide(sommePoids, 8, RoundingMode.HALF_UP)
                        .multiply(CINQ).setScale(2, RoundingMode.HALF_UP);

        Map<String, BigDecimal> ponderations = ponderations(campagne);
        BigDecimal noteComportement = noteComportement(notesComportement, campagne);
        BigDecimal globale = noteObjectifs.multiply(ponderations.get("objectifs"))
                .add(noteCompetences.multiply(ponderations.get("competences")))
                .add(noteComportement.multiply(ponderations.get("comportement")))
                .setScale(2, RoundingMode.HALF_UP);
        evaluation.setNoteObjectifs(noteObjectifs);
        evaluation.setNoteCompetences(noteCompetences);
        evaluation.setNoteComportement(noteComportement);
        evaluation.setNoteGlobale(globale);
        evaluation.setAppreciation(appreciation(globale));
    }

    private BigDecimal tauxAtteinte(Objectif objectif) {
        BigDecimal cible = objectif.getValeurCible();
        BigDecimal realise = objectif.getValeurRealisee();
        if (cible == null || cible.signum() == 0 || realise == null) {
            return BigDecimal.ZERO;
        }
        if ("MOINS_EST_MIEUX".equals(objectif.getSens())) {
            if (realise.signum() == 0) {
                return BigDecimal.ONE;
            }
            return cible.divide(realise, 6, RoundingMode.HALF_UP);
        }
        return realise.divide(cible, 6, RoundingMode.HALF_UP);
    }

    private BigDecimal noteComportement(Map<String, Integer> notes, CampagneEvaluation campagne) {
        Map<String, BigDecimal> grille = lireMap(campagne.getGrilleJson());
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal poidsTotal = BigDecimal.ZERO;
        for (Map.Entry<String, Integer> entree : notes.entrySet()) {
            BigDecimal poids = grille.getOrDefault(entree.getKey(), BigDecimal.ONE);
            total = total.add(BigDecimal.valueOf(entree.getValue()).multiply(poids));
            poidsTotal = poidsTotal.add(poids);
        }
        return poidsTotal.signum() == 0 ? BigDecimal.ZERO
                : total.divide(poidsTotal, 8, RoundingMode.HALF_UP).setScale(2, RoundingMode.HALF_UP);
    }

    private Map<String, BigDecimal> ponderations(CampagneEvaluation campagne) {
        Map<String, BigDecimal> lues = lireMap(campagne.getPonderationsJson());
        Map<String, BigDecimal> resultat = new HashMap<>();
        resultat.put("objectifs", lues.getOrDefault("objectifs", POIDS_OBJECTIFS));
        resultat.put("competences", lues.getOrDefault("competences", POIDS_COMPETENCES));
        resultat.put("comportement", lues.getOrDefault("comportement", POIDS_COMPORTEMENT));
        return resultat;
    }

    private Map<String, BigDecimal> lireMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<Map<String, BigDecimal>>() {
            });
        } catch (Exception ex) {
            throw new BusinessException("PARAMETRES_EVALUATION_INVALides", "Grille de notation invalide.");
        }
    }

    private String appreciation(BigDecimal note) {
        if (note.compareTo(new BigDecimal("4.5")) >= 0) {
            return "Exceptionnel";
        }
        if (note.compareTo(BigDecimal.valueOf(4)) >= 0) {
            return "Supérieur";
        }
        if (note.compareTo(BigDecimal.valueOf(3)) >= 0) {
            return "Conforme";
        }
        if (note.compareTo(BigDecimal.valueOf(2)) >= 0) {
            return "À améliorer";
        }
        return "Insuffisant";
    }

    private String priorite(BigDecimal score) {
        if (score.compareTo(BigDecimal.valueOf(4)) >= 0) {
            return "HAUTE";
        }
        return score.compareTo(BigDecimal.valueOf(2)) >= 0 ? "MOYENNE" : "BASSE";
    }

    private String json(Object valeur) {
        try {
            return MAPPER.writeValueAsString(valeur);
        } catch (Exception ex) {
            throw new BusinessException("EVALUATION_SERIALISATION", "Impossible d'enregistrer l'évaluation.");
        }
    }

    private boolean estVide(String texte) {
        return texte == null || texte.isBlank();
    }

    private boolean estRhOuDirection(UtilisateurConnecte utilisateur) {
        return Roles.RH.equals(utilisateur.getRoleNom()) || Roles.DIRECTION.equals(utilisateur.getRoleNom());
    }

    private void exigerEmploye(Long idEmploye) {
        UtilisateurConnecte utilisateur = utilisateurCourant.exiger();
        boolean proprietaire = employes.idParUtilisateur(utilisateur.getId())
                .map(idEmploye::equals).orElse(false);
        if (!proprietaire && !estRhOuDirection(utilisateur)) {
            throw BusinessException.forbidden("Vous n'avez pas accès à cette évaluation.");
        }
    }

    private boolean peutLire(Evaluation evaluation) {
        UtilisateurConnecte utilisateur = utilisateurCourant.optionnel();
        if (utilisateur == null) {
            return false;
        }
        if (estRhOuDirection(utilisateur)) {
            return true;
        }
        if (utilisateur.getId().equals(evaluation.getIdEvaluateur())
                || utilisateur.getId().equals(evaluation.getIdValideurN2())) {
            return true;
        }
        return employes.idParUtilisateur(utilisateur.getId()).map(evaluation.getIdEmploye()::equals).orElse(false);
    }

    private void verifierAcces(Evaluation evaluation) {
        if (!peutLire(evaluation)) {
            throw BusinessException.forbidden("Cette évaluation est confidentielle.");
        }
    }

    private void verifierStatut(Evaluation evaluation, Evaluation.Statut... attendus) {
        boolean ok = false;
        for (Evaluation.Statut statut : attendus) {
            if (statut.name().equals(evaluation.getStatut())) {
                ok = true;
                break;
            }
        }
        if (!ok) {
            throw new BusinessException("TRANSITION_EVALUATION_INVALIDE",
                    "Transition non autorisée depuis " + evaluation.getStatut() + ".");
        }
    }

    public Evaluation evaluationEntity(Long idEvaluation) {
        return evaluations.findById(idEvaluation)
                .orElseThrow(() -> BusinessException.notFound("Évaluation introuvable."));
    }
}
