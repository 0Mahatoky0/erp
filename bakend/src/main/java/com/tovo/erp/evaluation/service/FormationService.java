package com.tovo.erp.evaluation.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tovo.erp.conges.dto.CongeDtos;
import com.tovo.erp.conges.service.CongeService;
import com.tovo.erp.employe.dto.EmployeEmbaucheDto;
import com.tovo.erp.employe.service.EmployeService;
import com.tovo.erp.evaluation.dto.EvaluationDtos;
import com.tovo.erp.evaluation.model.BesoinFormation;
import com.tovo.erp.evaluation.model.ConventionFormation;
import com.tovo.erp.evaluation.model.EcartCompetence;
import com.tovo.erp.evaluation.model.Evaluation;
import com.tovo.erp.evaluation.model.EvaluationFormation;
import com.tovo.erp.evaluation.model.Formation;
import com.tovo.erp.evaluation.model.InscriptionFormation;
import com.tovo.erp.evaluation.model.PlanFormation;
import com.tovo.erp.evaluation.model.SessionFormation;
import com.tovo.erp.evaluation.repository.BesoinFormationRepository;
import com.tovo.erp.evaluation.repository.ConventionFormationRepository;
import com.tovo.erp.evaluation.repository.EcartCompetenceRepository;
import com.tovo.erp.evaluation.repository.EvaluationFormationRepository;
import com.tovo.erp.evaluation.repository.EvaluationRepository;
import com.tovo.erp.evaluation.repository.FormationRepository;
import com.tovo.erp.evaluation.repository.InscriptionFormationRepository;
import com.tovo.erp.evaluation.repository.PlanFormationRepository;
import com.tovo.erp.evaluation.repository.SessionFormationRepository;
import com.tovo.erp.shared.BusinessException;
import com.tovo.erp.shared.Roles;
import com.tovo.erp.shared.UtilisateurCourant;
import com.tovo.erp.shared.audit.JournalAuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class FormationService {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final BigDecimal PRESENCE_MINIMALE = new BigDecimal("0.80");
    private static final BigDecimal SEUIL_VALIDATION_DIRECTION = new BigDecimal("1000000");
    private static final List<String> PLACES_CONFIRMEES = List.of("CONFIRMEE", "PRESENT");

    private final FormationRepository formations;
    private final SessionFormationRepository sessions;
    private final InscriptionFormationRepository inscriptions;
    private final PlanFormationRepository plans;
    private final EvaluationFormationRepository evaluationsFormation;
    private final EvaluationRepository evaluations;
    private final ConventionFormationRepository conventions;
    private final BesoinFormationRepository besoins;
    private final EcartCompetenceRepository ecarts;
    private final EmployeService employes;
    private final CongeService conges;
    private final ReferentielService referentiel;
    private final UtilisateurCourant utilisateurCourant;
    private final JournalAuditService audit;

    public FormationService(FormationRepository formations, SessionFormationRepository sessions,
                            InscriptionFormationRepository inscriptions, PlanFormationRepository plans,
                            EvaluationFormationRepository evaluationsFormation, EvaluationRepository evaluations,
                            ConventionFormationRepository conventions, BesoinFormationRepository besoins,
                            EcartCompetenceRepository ecarts, EmployeService employes, CongeService conges,
                            ReferentielService referentiel, UtilisateurCourant utilisateurCourant,
                            JournalAuditService audit) {
        this.formations = formations;
        this.sessions = sessions;
        this.inscriptions = inscriptions;
        this.plans = plans;
        this.evaluationsFormation = evaluationsFormation;
        this.evaluations = evaluations;
        this.conventions = conventions;
        this.besoins = besoins;
        this.ecarts = ecarts;
        this.employes = employes;
        this.conges = conges;
        this.referentiel = referentiel;
        this.utilisateurCourant = utilisateurCourant;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<EvaluationDtos.FormationDto> formations() {
        return formations.findAllByOrderByTitreAsc().stream()
                .map(EvaluationDtos.FormationDto::de).toList();
    }

    @Transactional
    public EvaluationDtos.FormationDto creerFormation(EvaluationDtos.FormationRequest request) {
        Formation formation = new Formation();
        formation.setCode(request.code().trim().toUpperCase());
        formation.setTitre(request.titre());
        formation.setType(request.type().trim().toUpperCase());
        formation.setOrganisme(request.organisme());
        formation.setDureeH(request.dureeH());
        formation.setCout(request.cout());
        formation.setCertifiante(request.certifiante());
        formation.setCompetencesVisees(request.competencesVisees());
        formation.setSeuilDedit(request.seuilDedit());
        EvaluationDtos.FormationDto cree = EvaluationDtos.FormationDto.de(formations.save(formation));
        audit.enregistrer("FORMATION_CREE", "formation", cree.id(), null, cree.toString());
        return cree;
    }

    @Transactional(readOnly = true)
    public List<EvaluationDtos.SessionDto> sessions() {
        return sessions.findAllByOrderByDateDebutDesc().stream()
                .map(s -> EvaluationDtos.SessionDto.de(s, nombreConfirme(s.getId()))).toList();
    }

    @Transactional
    public EvaluationDtos.SessionDto creerSession(EvaluationDtos.SessionRequest request) {
        Formation formation = formation(request.idFormation());
        if (request.dateFin().isBefore(request.dateDebut())) {
            throw new BusinessException("SESSION_DATES_INVALIDE", "La date de fin précède la date de début.");
        }
        SessionFormation session = new SessionFormation();
        session.setIdFormation(request.idFormation());
        session.setDateDebut(request.dateDebut());
        session.setDateFin(request.dateFin());
        session.setLieu(request.lieu());
        session.setCapacite(request.capacite());
        session.setFormateur(request.formateur());
        session.setCout(request.cout());
        session.setStatut(SessionFormation.Statut.PLANIFIEE.name());
        SessionFormation enregistree = sessions.save(session);
        audit.enregistrer("SESSION_CREE", "session_formation", enregistree.getId(), null, formation.getTitre());
        return EvaluationDtos.SessionDto.de(enregistree, 0);
    }

    @Transactional
    public EvaluationDtos.SessionDto changerStatutSession(Long idSession, String statut) {
        SessionFormation session = session(idSession);
        session.setStatut(statut.trim().toUpperCase());
        SessionFormation enregistree = sessions.save(session);
        audit.enregistrer("SESSION_STATUT", "session_formation", idSession, null, enregistree.getStatut());
        return EvaluationDtos.SessionDto.de(enregistree, nombreConfirme(idSession));
    }

    @Transactional(readOnly = true)
    public List<EvaluationDtos.PlanFormationDto> plans(int annee) {
        return plans.findByAnneeOrderByIdDepartementAsc(annee).stream()
                .map(EvaluationDtos.PlanFormationDto::de).toList();
    }

    @Transactional
    public EvaluationDtos.PlanFormationDto enregistrerPlan(EvaluationDtos.PlanFormationRequest request) {
        utilisateurCourant.exigerRole(Roles.RH, Roles.FINANCE, Roles.DIRECTION);
        PlanFormation plan = plans.findByAnneeAndIdDepartement(request.annee(), request.idDepartement())
                .orElseGet(PlanFormation::new);
        plan.setAnnee(request.annee());
        plan.setIdDepartement(request.idDepartement());
        plan.setBudget(request.budget());
        EvaluationDtos.PlanFormationDto enregistre = EvaluationDtos.PlanFormationDto.de(plans.save(plan));
        audit.enregistrer("PLAN_FORMATION", "plan_formation", enregistre.id(), null, enregistre.toString());
        return enregistre;
    }

    @Transactional(readOnly = true)
    public List<EvaluationDtos.InscriptionDto> inscriptionsSession(Long idSession) {
        return inscriptions.findByIdSession(idSession).stream()
                .map(EvaluationDtos.InscriptionDto::de).toList();
    }

    @Transactional(readOnly = true)
    public List<EvaluationDtos.InscriptionDto> inscriptionsEmploye(Long idEmploye) {
        verifierAccesEmploye(idEmploye);
        return inscriptions.findByIdEmployeOrderByDateDemandeDesc(idEmploye).stream()
                .map(EvaluationDtos.InscriptionDto::de).toList();
    }

    @Transactional(readOnly = true)
    public List<EvaluationDtos.InscriptionDto> mesInscriptions() {
        var utilisateur = utilisateurCourant.exiger();
        Long idEmploye = employes.idParUtilisateur(utilisateur.getId())
                .orElseThrow(() -> BusinessException.notFound("Profil salarié introuvable."));
        return inscriptions.findByIdEmployeOrderByDateDemandeDesc(idEmploye).stream()
                .map(EvaluationDtos.InscriptionDto::de).toList();
    }

    @Transactional
    public EvaluationDtos.InscriptionDto demanderInscriptionPourMoi(Long idSession, Long idBesoin) {
        var utilisateur = utilisateurCourant.exiger();
        Long idEmploye = employes.idParUtilisateur(utilisateur.getId())
                .orElseThrow(() -> BusinessException.notFound("Profil salarié introuvable."));
        return demanderInscription(new EvaluationDtos.InscriptionRequest(idSession, idEmploye, idBesoin));
    }

    @Transactional
    public EvaluationDtos.InscriptionDto demanderInscription(EvaluationDtos.InscriptionRequest request) {
        SessionFormation session = session(request.idSession());
        Formation formation = formation(session.getIdFormation());
        if (session.getDateDebut().isBefore(LocalDate.now())
                || "ANNULEE".equals(session.getStatut()) || "REALISEE".equals(session.getStatut())) {
            throw new BusinessException("SESSION_NON_OUVERTE", "Cette session n'accepte plus d'inscription.");
        }
        verifierAbsenceConge(request.idEmploye(), session);
        verifierPrerequis(request.idEmploye(), formation);
        InscriptionFormation inscription = new InscriptionFormation();
        inscription.setIdSession(request.idSession());
        inscription.setIdEmploye(request.idEmploye());
        inscription.setIdBesoin(request.idBesoin());
        inscription.setStatut(InscriptionFormation.Statut.DEMANDEE.name());
        EvaluationDtos.InscriptionDto cree = EvaluationDtos.InscriptionDto.de(inscriptions.save(inscription));
        audit.enregistrer("INSCRIPTION_DEMANDÉE", "inscription_formation", cree.id(), null, cree.toString());
        return cree;
    }

    @Transactional
    public EvaluationDtos.InscriptionDto deciderInscriptionManager(Long idInscription,
                                                                   EvaluationDtos.DecisionInscriptionRequest request) {
        utilisateurCourant.exigerRole(Roles.MANAGER, Roles.RH, Roles.DIRECTION);
        InscriptionFormation inscription = inscription(idInscription);
        if (!"DEMANDEE".equals(inscription.getStatut())) {
            throw new BusinessException("INSCRIPTION_STATUT_INVALIDE", "Cette demande n'est plus en attente.");
        }
        inscription.setStatut(request.approuver()
                ? InscriptionFormation.Statut.VALIDEE_MANAGER.name()
                : InscriptionFormation.Statut.ANNULEE.name());
        if (!request.approuver()) {
            inscription.setResultat(request.motif());
        }
        EvaluationDtos.InscriptionDto decidée = EvaluationDtos.InscriptionDto.de(inscriptions.save(inscription));
        audit.enregistrer("INSCRIPTION_MANAGER", "inscription_formation", idInscription, null, decidée.statut());
        return decidée;
    }

    @Transactional
    public EvaluationDtos.InscriptionDto confirmerInscription(Long idInscription) {
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION);
        InscriptionFormation inscription = inscription(idInscription);
        if (!"VALIDEE_MANAGER".equals(inscription.getStatut())
                && !"LISTE_ATTENTE".equals(inscription.getStatut())) {
            throw new BusinessException("INSCRIPTION_NON_VALIDEE",
                    "La demande doit être validée par le manager avant confirmation.");
        }
        SessionFormation session = session(inscription.getIdSession());
        Formation formation = formation(session.getIdFormation());
        verifierAbsenceConge(inscription.getIdEmploye(), session);
        if (nombreConfirme(session.getId()) >= session.getCapacite()) {
            inscription.setStatut(InscriptionFormation.Statut.LISTE_ATTENTE.name());
            return EvaluationDtos.InscriptionDto.de(inscriptions.save(inscription));
        }
        verifierBudget(inscription.getIdEmploye(), session);
        confirmer(inscription);
        EvaluationDtos.InscriptionDto confirmee = EvaluationDtos.InscriptionDto.de(inscriptions.save(inscription));
        audit.enregistrer("INSCRIPTION_CONFIRMEE", "inscription_formation", idInscription, null, confirmee.toString());
        return confirmee;
    }

    @Transactional
    public EvaluationDtos.InscriptionDto annulerInscription(Long idInscription) {
        InscriptionFormation inscription = inscription(idInscription);
        boolean etaitConfirmee = PLACES_CONFIRMEES.contains(inscription.getStatut());
        inscription.setStatut(InscriptionFormation.Statut.ANNULEE.name());
        inscriptions.save(inscription);
        audit.enregistrer("INSCRIPTION_ANNULEE", "inscription_formation", idInscription, null, null);
        if (etaitConfirmee) {
            promouvoirListeAttente(inscription.getIdSession());
        }
        return EvaluationDtos.InscriptionDto.de(inscription);
    }

    @Transactional
    public EvaluationDtos.InscriptionDto enregistrerPresence(Long idInscription,
                                                             EvaluationDtos.PresenceRequest request) {
        utilisateurCourant.exigerRole(Roles.MANAGER, Roles.RH, Roles.DIRECTION);
        InscriptionFormation inscription = inscription(idInscription);
        SessionFormation session = session(inscription.getIdSession());
        Formation formation = formation(session.getIdFormation());
        BigDecimal taux = request.presenceH().divide(formation.getDureeH(), 6, RoundingMode.HALF_UP);
        inscription.setPresenceH(request.presenceH());
        inscription.setResultat(request.resultat());
        inscription.setCertificatDocumentId(request.certificatDocumentId());
        inscription.setStatut(taux.compareTo(PRESENCE_MINIMALE) >= 0
                ? InscriptionFormation.Statut.PRESENT.name() : InscriptionFormation.Statut.ABSENT.name());
        if (inscription.getIdBesoin() != null && "PRESENT".equals(inscription.getStatut())) {
            BesoinFormation besoin = besoin(inscription.getIdBesoin());
            besoin.setStatut(BesoinFormation.Statut.REALISE.name());
            besoins.save(besoin);
        }
        EvaluationDtos.InscriptionDto enregistree = EvaluationDtos.InscriptionDto.de(inscriptions.save(inscription));
        audit.enregistrer("PRESENCE_FORMATION", "inscription_formation", idInscription, null, taux.toPlainString());
        return enregistree;
    }

    @Transactional
    public EvaluationDtos.EvaluationFormationDto evaluerFormation(Long idInscription,
                                                                  EvaluationDtos.EvaluationFormationRequest request) {
        InscriptionFormation inscription = inscription(idInscription);
        if (!"PRESENT".equals(inscription.getStatut()) && !"ABSENT".equals(inscription.getStatut())) {
            throw new BusinessException("INSCRIPTION_NON_TERMINEE", "La formation doit être terminée.");
        }
        EvaluationFormation evaluation = new EvaluationFormation();
        evaluation.setIdInscription(idInscription);
        evaluation.setType(request.type().trim().toUpperCase());
        evaluation.setSatisfaction(request.satisfaction());
        evaluation.setAcquis(request.acquis());
        evaluation.setTransfert(request.transfert());
        evaluation.setCommentaire(request.commentaire());
        EvaluationDtos.EvaluationFormationDto cree =
                EvaluationDtos.EvaluationFormationDto.de(evaluationsFormation.save(evaluation));
        audit.enregistrer("EVALUATION_FORMATION", "evaluation_formation", cree.id(), null, cree.toString());
        return cree;
    }

    @Transactional(readOnly = true)
    public List<EvaluationDtos.EvaluationFormationDto> evaluationsInscription(Long idInscription) {
        return evaluationsFormation.findByIdInscriptionOrderByDateEvaluationDesc(idInscription).stream()
                .map(EvaluationDtos.EvaluationFormationDto::de).toList();
    }

    @Transactional
    public EvaluationDtos.EvaluationFormationDto mesurer90Jours(Long idInscription,
                                                                EvaluationDtos.EmployeCompetenceRequest request) {
        utilisateurCourant.exigerRole(Roles.MANAGER, Roles.RH, Roles.DIRECTION);
        InscriptionFormation inscription = inscription(idInscription);
        EvaluationFormation mesure = new EvaluationFormation();
        mesure.setIdInscription(idInscription);
        mesure.setType(EvaluationFormation.Type.A_FROID_90J.name());
        mesure.setTransfert(request.niveauAcquis() == null ? null : BigDecimal.valueOf(request.niveauAcquis()));
        mesure.setCommentaire(request.justificatif());
        EvaluationDtos.EvaluationFormationDto cree =
                EvaluationDtos.EvaluationFormationDto.de(evaluationsFormation.save(mesure));
        EvaluationDtos.EmployeCompetenceDto niveau = referentiel.enregistrerNiveau(request,
                utilisateurCourant.exiger().getId());
        ecarts.findByIdEmployeOrderByPrioriteScoreDesc(inscription.getIdEmploye()).stream()
                .filter(e -> e.getIdCompetence().equals(niveau.idCompetence())
                        && "OUVERT".equals(e.getStatut()))
                .forEach(e -> {
                    e.setStatut(EcartCompetence.Statut.COMBLE.name());
                    ecarts.save(e);
                });
        audit.enregistrer("MESURE_90J", "evaluation_formation", cree.id(), null, niveau.toString());
        return cree;
    }

    @Transactional
    public void enregistrerConvention(Long idInscription, EvaluationDtos.ConventionFormationRequest request) {
        inscription(idInscription);
        ConventionFormation convention = conventions.findByIdInscription(idInscription).orElseGet(ConventionFormation::new);
        convention.setIdInscription(idInscription);
        convention.setCout(request.cout());
        convention.setDureeEngagementMois(request.dureeEngagementMois());
        convention.setClauseDedit(request.clauseDedit());
        conventions.save(convention);
        audit.enregistrer("CONVENTION_FORMATION", "convention_formation", convention.getId(), null,
                convention.getClauseDedit());
    }

    @Transactional(readOnly = true)
    public EvaluationDtos.RemboursementDto remboursement(Long idInscription, int moisRestants) {
        ConventionFormation convention = conventions.findByIdInscription(idInscription)
                .orElseThrow(() -> BusinessException.notFound("Convention de formation introuvable."));
        BigDecimal reste = BigDecimal.valueOf(Math.min(moisRestants, convention.getDureeEngagementMois()));
        BigDecimal remboursement = convention.getCout().multiply(reste)
                .divide(BigDecimal.valueOf(convention.getDureeEngagementMois()), 2, RoundingMode.HALF_UP);
        return new EvaluationDtos.RemboursementDto(convention.getCout(), moisRestants,
                convention.getDureeEngagementMois(), remboursement);
    }

    @Transactional(readOnly = true)
    public EvaluationDtos.IndicateursDto indicateurs() {
        int annee = Year.now().getValue();
        long cible = employes.actifs().size();
        Map<String, Long> distribution = new HashMap<>();
        long evaluees = evaluations.findAll().stream()
                .filter(e -> !"BROUILLON".equals(e.getStatut()) && !"AUTO_EVALUATION".equals(e.getStatut()))
                .count();
        evaluations.findAll().stream()
                .filter(e -> e.getAppreciation() != null)
                .forEach(e -> distribution.merge(e.getAppreciation(), 1L, Long::sum));
        BigDecimal heures = BigDecimal.ZERO;
        BigDecimal budget = BigDecimal.ZERO;
        long planifiees = 0;
        long realisees = 0;
        for (SessionFormation session : sessions.findAll()) {
            if (session.getDateDebut().getYear() == annee) {
                planifiees++;
                if ("REALISEE".equals(session.getStatut())) {
                    realisees++;
                }
            }
        }
        for (InscriptionFormation inscription : inscriptions.findAll()) {
            if (PLACES_CONFIRMEES.contains(inscription.getStatut())) {
                SessionFormation session = session(inscription.getIdSession());
                Formation formation = formation(session.getIdFormation());
                heures = heures.add(formation.getDureeH());
                if (session.getDateDebut().getYear() == annee) {
                    budget = budget.add(session.getCout());
                }
            }
        }
        long employesActifs = employes.actifs().size();
        if (employesActifs > 0) {
            heures = heures.divide(BigDecimal.valueOf(employesActifs), 2, RoundingMode.HALF_UP);
        }
        List<EcartCompetence> tousEcarts = ecarts.findAll();
        long ecartsIdentifies = tousEcarts.size();
        long ecartsCombles = tousEcarts.stream()
                .filter(e -> "COMBLE".equals(e.getStatut())).count();
        BigDecimal tauxComblement = pourcentage(ecartsCombles, ecartsIdentifies);
        return new EvaluationDtos.IndicateursDto(
                pourcentage(evaluees, cible), distribution, heures, budget,
                pourcentage(realisees, planifiees), tauxComblement);
    }

    private void confirmer(InscriptionFormation inscription) {
        inscription.setStatut(InscriptionFormation.Statut.CONFIRMEE.name());
        if (inscription.getIdBesoin() != null) {
            BesoinFormation besoin = besoin(inscription.getIdBesoin());
            besoin.setStatut(BesoinFormation.Statut.PLANIFIE.name());
            besoins.save(besoin);
            ecarts.findByIdEmployeOrderByPrioriteScoreDesc(inscription.getIdEmploye()).stream()
                    .filter(e -> e.getIdCompetence().equals(besoin.getIdCompetence())
                            && "OUVERT".equals(e.getStatut()))
                    .forEach(e -> {
                        e.setStatut(EcartCompetence.Statut.FORMATION_PLANIFIEE.name());
                        ecarts.save(e);
                    });
        }
    }

    private void promouvoirListeAttente(Long idSession) {
        SessionFormation session = session(idSession);
        if (nombreConfirme(idSession) >= session.getCapacite()) {
            return;
        }
        List<InscriptionFormation> attente = inscriptions
                .findByIdSessionAndStatutOrderByDateDemandeAsc(idSession, InscriptionFormation.Statut.LISTE_ATTENTE.name());
        if (attente.isEmpty()) {
            return;
        }
        InscriptionFormation promue = attente.getFirst();
        verifierAbsenceConge(promue.getIdEmploye(), session);
        confirmer(promue);
        inscriptions.save(promue);
        audit.enregistrer("LISTE_ATTENTE_PROMUE", "inscription_formation", promue.getId(), null, null);
    }

    private void verifierBudget(Long idEmploye, SessionFormation session) {
        if (session.getCout().compareTo(SEUIL_VALIDATION_DIRECTION) > 0
                && !utilisateurCourant.aLeRole(Roles.DIRECTION)) {
            throw new BusinessException("VALIDATION_DIRECTION_REQUISE",
                    "Une formation dont le coût dépasse le seuil exige une validation de la direction.");
        }
        EmployeEmbaucheDto employe = employes.dossier(idEmploye);
        Long idDepartement = employe.idDepartement();
        if (idDepartement == null) {
            throw new BusinessException("DEPARTEMENT_MANQUANT", "Le salarié n'est rattaché à aucun département.");
        }
        PlanFormation plan = plans.findByAnneeAndIdDepartement(session.getDateDebut().getYear(), idDepartement)
                .orElseThrow(() -> BusinessException.notFound("Aucun plan de formation pour ce département."));
        BigDecimal engage = BigDecimal.ZERO;
        for (InscriptionFormation inscription : inscriptions.findByIdSession(session.getId())) {
            if (PLACES_CONFIRMEES.contains(inscription.getStatut())
                    && idDepartement.equals(employes.dossier(inscription.getIdEmploye()).idDepartement())) {
                engage = engage.add(session.getCout());
            }
        }
        if (engage.add(session.getCout()).compareTo(plan.getBudget()) > 0) {
            throw new BusinessException("BUDGET_FORMATION_DEPASSE", "Le budget de formation du département est insuffisant.");
        }
    }

    private void verifierPrerequis(Long idEmploye, Formation formation) {
        if (formation.getCompetencesVisees() == null || formation.getCompetencesVisees().isBlank()) {
            return;
        }
        try {
            List<Map<String, Object>> visees = MAPPER.readValue(formation.getCompetencesVisees(),
                    new TypeReference<List<Map<String, Object>>>() {
                    });
            Map<Long, Integer> acquis = referentiel.niveauxAcquis(idEmploye);
            for (Map<String, Object> visee : visees) {
                Object identifiant = visee.get("competence") == null ? visee.get("idCompetence") : visee.get("competence");
                Object niveau = visee.get("niveau") == null ? visee.get("niveauMinimum") : visee.get("niveau");
                if (identifiant == null || niveau == null) {
                    continue;
                }
                Long idCompetence = Long.valueOf(String.valueOf(identifiant));
                int minimum = Integer.parseInt(String.valueOf(niveau));
                if (acquis.getOrDefault(idCompetence, 0) < minimum) {
                    throw new BusinessException("PREREQUIS_FORMATION_NON_SATISFAIT",
                            "Niveau insuffisant pour une compétence prérequise.");
                }
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("PREREQUIS_FORMATION_INVALIDE", "Prérequis de formation illisible.");
        }
    }

    private void verifierAbsenceConge(Long idEmploye, SessionFormation session) {
        List<CongeDtos.PlanningDto> congesValides = conges.planningConges(idEmploye,
                session.getDateDebut(), session.getDateFin());
        if (!congesValides.isEmpty()) {
            throw new BusinessException("FORMATION_CHEVAUCHE_CONGE",
                    "La formation chevauche un congé approuvé.");
        }
    }

    private void verifierAccesEmploye(Long idEmploye) {
        var utilisateur = utilisateurCourant.optionnel();
        if (utilisateur == null) {
            throw BusinessException.forbidden("Authentification requise.");
        }
        boolean proprietaire = employes.idParUtilisateur(utilisateur.getId())
                .map(idEmploye::equals).orElse(false);
        if (!proprietaire && !utilisateurCourant.aLeRole(Roles.RH, Roles.MANAGER, Roles.DIRECTION, Roles.FINANCE)) {
            throw BusinessException.forbidden("Vous n'avez pas accès à ces inscriptions.");
        }
    }

    private long nombreConfirme(Long idSession) {
        return inscriptions.findByIdSession(idSession).stream()
                .filter(i -> PLACES_CONFIRMEES.contains(i.getStatut())).count();
    }

    private BigDecimal pourcentage(long numerateur, long denominateur) {
        if (denominateur == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerateur).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominateur), 2, RoundingMode.HALF_UP);
    }

    private Formation formation(Long idFormation) {
        return formations.findById(idFormation)
                .orElseThrow(() -> BusinessException.notFound("Formation introuvable."));
    }

    private SessionFormation session(Long idSession) {
        return sessions.findById(idSession)
                .orElseThrow(() -> BusinessException.notFound("Session de formation introuvable."));
    }

    private InscriptionFormation inscription(Long idInscription) {
        return inscriptions.findById(idInscription)
                .orElseThrow(() -> BusinessException.notFound("Inscription introuvable."));
    }

    private BesoinFormation besoin(Long idBesoin) {
        return besoins.findById(idBesoin)
                .orElseThrow(() -> BusinessException.notFound("Besoins de formation introuvable."));
    }
}
