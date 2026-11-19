package com.tovo.erp.discipline.service;

import com.tovo.erp.discipline.dto.DisciplineDtos;
import com.tovo.erp.discipline.model.EtapeProcedure;
import com.tovo.erp.discipline.model.GraviteIncident;
import com.tovo.erp.discipline.model.Incident;
import com.tovo.erp.discipline.model.NiveauAccesDisciplinaire;
import com.tovo.erp.discipline.model.ProcedureDisciplinaire;
import com.tovo.erp.discipline.model.StatutProcedure;
import com.tovo.erp.discipline.model.TypeEtapeProcedure;
import com.tovo.erp.discipline.model.TypeSanction;
import com.tovo.erp.discipline.repository.EtapeRepository;
import com.tovo.erp.discipline.repository.IncidentRepository;
import com.tovo.erp.discipline.repository.PieceRepository;
import com.tovo.erp.discipline.repository.ProcedureRepository;
import com.tovo.erp.discipline.repository.SanctionRepository;
import com.tovo.erp.employe.service.EmployeService;
import com.tovo.erp.shared.BusinessException;
import com.tovo.erp.shared.Roles;
import com.tovo.erp.shared.UtilisateurCourant;
import com.tovo.erp.shared.audit.JournalAuditService;
import com.tovo.erp.shared.reference.ReferenceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProcedureService {

    private static final int DELAI_PRESCRIPTION_MOIS = 2;
    private static final int DELAI_EXPLICATION_MIN = 3;

    private final ProcedureRepository procedures;
    private final EtapeRepository etapes;
    private final IncidentRepository incidents;
    private final PieceRepository pieces;
    private final SanctionRepository sanctions;
    private final EmployeService employes;
    private final ReferenceService references;
    private final UtilisateurCourant utilisateurCourant;
    private final JournalAuditService audit;
    private final JournalDisciplineService journal;
    private final AccesDisciplinaireService acces;
    private final SanctionService sanctionService;

    public ProcedureService(ProcedureRepository procedures, EtapeRepository etapes,
                            IncidentRepository incidents, PieceRepository pieces,
                            SanctionRepository sanctions, EmployeService employes,
                            ReferenceService references, UtilisateurCourant utilisateurCourant,
                            JournalAuditService audit, JournalDisciplineService journal,
                            AccesDisciplinaireService acces, SanctionService sanctionService) {
        this.procedures = procedures;
        this.etapes = etapes;
        this.incidents = incidents;
        this.pieces = pieces;
        this.sanctions = sanctions;
        this.employes = employes;
        this.references = references;
        this.utilisateurCourant = utilisateurCourant;
        this.audit = audit;
        this.journal = journal;
        this.acces = acces;
        this.sanctionService = sanctionService;
    }

    @Transactional(readOnly = true)
    public List<DisciplineDtos.ProcedureDto> lister(Long idEmploye) {
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION);
        List<ProcedureDisciplinaire> liste = idEmploye == null
                ? procedures.findAllByOrderByDateOuvertureDesc()
                : procedures.findByIdEmployeOrderByDateOuvertureDesc(idEmploye);
        return liste.stream().map(this::enDto).toList();
    }

    @Transactional(readOnly = true)
    public DisciplineDtos.ProcedureDto procedure(Long idProcedure, String motif) {
        acces.exigerAccesProcedure(idProcedure, NiveauAccesDisciplinaire.LECTURE_RESUMEE, motif);
        return enDto(procedures.findById(idProcedure)
                .orElseThrow(() -> BusinessException.notFound("Procedure introuvable.")));
    }

    @Transactional
    public DisciplineDtos.ProcedureDto ouvrir(Incident incident, Long idEmploye,
                                              Long instructeurId, Long decideurId,
                                              boolean miseAPied, LocalDate miseAPiedDebut,
                                              LocalDate miseAPiedFin, String motif) {
        if (!employes.existe(idEmploye)) {
            throw BusinessException.notFound("Le salarie " + idEmploye + " n'existe pas.");
        }
        if (procedures.existsByIdIncidentAndIdEmploye(incident.getId(), idEmploye)) {
            throw new BusinessException("PROCEDURE_DEJA_OUVERTE",
                    "Une procedure est deja ouverte pour ce salarie sur cet incident.");
        }
        if (incident.getDateConnaissance().plusMonths(DELAI_PRESCRIPTION_MOIS).isBefore(LocalDate.now())) {
            throw new BusinessException("PRESCRIPTION_ACQUISE",
                    "Le delai de prescription est depasse.");
        }
        ProcedureDisciplinaire procedure = new ProcedureDisciplinaire();
        procedure.setReference(references.suivante("PROCEDURE", "PROC", 4));
        procedure.setIdIncident(incident.getId());
        procedure.setIdEmploye(idEmploye);
        procedure.setInstructeurId(instructeurId);
        procedure.setDecideurId(decideurId);
        procedure.setDateOuverture(LocalDateTime.now());
        procedure.setDateLimitePrescription(incident.getDateConnaissance().plusMonths(DELAI_PRESCRIPTION_MOIS));
        procedure.setMiseAPied(miseAPied);
        procedure.setMiseAPiedDebut(miseAPiedDebut);
        procedure.setMiseAPiedFin(miseAPiedFin);
        procedure.setMiseAPiedRemuneree(miseAPied
                && (incident.getGraviteRetenue() == GraviteIncident.LEGERE
                || incident.getGraviteRetenue() == GraviteIncident.MOYENNE));
        procedure.setStatut(StatutProcedure.OUVERTE);
        ProcedureDisciplinaire cree = procedures.save(procedure);
        acces.accorderParDefaut(incident.getId(), cree.getId(), instructeurId, decideurId);
        audit.enregistrer("PROCEDURE_OUVERTE", "procedure", cree.getId(), null,
                cree.getReference(), motif, null);
        journal.enregistrer(utilisateurCourant.exiger().getId(), "PROCEDURE_OUVERTE", "procedure",
                cree.getId(), motif, cree.getReference());
        return enDto(cree);
    }

    @Transactional
    public DisciplineDtos.ProcedureDto demanderExplication(
            Long idProcedure, DisciplineDtos.EtapeRequest requete, String motif) {
        ProcedureDisciplinaire procedure = procedureModifiable(idProcedure, motif);
        if (etapes.existsByIdProcedureAndType(idProcedure, TypeEtapeProcedure.DEMANDE_EXPLICATION)) {
            throw new BusinessException("EXPLICATION_DEJA_DEMANDER",
                    "Une demande d'explication a deja ete notifiee.");
        }
        if (requete.delaiReponseJours() == null
                || requete.delaiReponseJours() < DELAI_EXPLICATION_MIN) {
            throw new BusinessException("DELAI_EXPLICATION_INVALIDE",
                    "Le delai de reponse doit etre d'au moins " + DELAI_EXPLICATION_MIN + " jours.");
        }
        exigerRemise(requete.modeRemise(), requete.preuve());
        EtapeProcedure etape = nouvelleEtape(procedure, TypeEtapeProcedure.DEMANDE_EXPLICATION, requete);
        etape.setDateLimite(LocalDateTime.now().plusDays(requete.delaiReponseJours()));
        etapes.save(etape);
        procedure.setStatut(StatutProcedure.EXPLICATION_DEMANDEE);
        procedures.save(procedure);
        audit.enregistrer("EXPLICATION_DEMANDEE", "procedure", idProcedure,
                StatutProcedure.OUVERTE.name(), procedure.getStatut().name(), motif, null);
        return enDto(procedure);
    }

    @Transactional
    public DisciplineDtos.ProcedureDto enregistrerReponse(
            Long idProcedure, DisciplineDtos.EtapeRequest requete, String motif) {
        ProcedureDisciplinaire procedure = procedures.findById(idProcedure)
                .orElseThrow(() -> BusinessException.notFound("Procedure introuvable."));
        boolean salarieConcerne = employes.idParUtilisateur(utilisateurCourant.exiger().getId())
                .filter(procedure.getIdEmploye()::equals).isPresent();
        if (salarieConcerne) {
            journal.exigerMotif(motif);
        } else {
            acces.exigerAccesProcedure(idProcedure, NiveauAccesDisciplinaire.ECRITURE, motif);
        }
        if (procedure.getStatut() != StatutProcedure.EXPLICATION_DEMANDEE
                && procedure.getStatut() != StatutProcedure.SANS_REPONSE) {
            throw new BusinessException("REPONSE_HORS_SEQUENCE",
                    "Aucune demande d'explication n'est en attente.");
        }
        if (etapes.existsByIdProcedureAndType(idProcedure, TypeEtapeProcedure.REPONSE)) {
            throw new BusinessException("REPONSE_DEJA_ENREGISTREE",
                    "Une reponse a deja ete enregistree.");
        }
        EtapeProcedure etape = nouvelleEtape(procedure, TypeEtapeProcedure.REPONSE, requete);
        if (salarieConcerne) {
            etape.setActeurId(procedure.getIdEmploye());
        }
        etapes.save(etape);
        procedure.setStatut(StatutProcedure.EXPLICATION_RECUE);
        procedures.save(procedure);
        audit.enregistrer("EXPLICATION_RECUE", "procedure", idProcedure,
                StatutProcedure.EXPLICATION_DEMANDEE.name(), procedure.getStatut().name(), motif, null);
        return enDto(procedure);
    }

    @Transactional
    public DisciplineDtos.ProcedureDto constaterSansReponse(Long idProcedure, String motif) {
        ProcedureDisciplinaire procedure = procedureModifiable(idProcedure, motif);
        if (procedure.getStatut() != StatutProcedure.EXPLICATION_DEMANDEE) {
            throw new BusinessException("CONSTAT_HORS_SEQUENCE",
                    "Aucune demande d'explication n'est en attente.");
        }
        boolean echeanceAtteinte = etapes.findByIdProcedureOrderByDateEtapeAsc(idProcedure).stream()
                .filter(e -> e.getType() == TypeEtapeProcedure.DEMANDE_EXPLICATION)
                .filter(e -> e.getDateLimite() != null)
                .anyMatch(e -> !e.getDateLimite().isAfter(LocalDateTime.now()));
        if (!echeanceAtteinte) {
            throw new BusinessException("DELAI_EXPLICATION_EN_COURS",
                    "Le delai de reponse n'est pas encore ecoule.");
        }
        procedure.setStatut(StatutProcedure.SANS_REPONSE);
        procedures.save(procedure);
        audit.enregistrer("SANS_REPONSE_CONSTATEE", "procedure", idProcedure,
                StatutProcedure.EXPLICATION_DEMANDEE.name(), procedure.getStatut().name(), motif, null);
        return enDto(procedure);
    }

    @Transactional
    public DisciplineDtos.ProcedureDto convoquer(
            Long idProcedure, DisciplineDtos.EtapeRequest requete, String motif) {
        ProcedureDisciplinaire procedure = procedureModifiable(idProcedure, motif);
        verifierEtapePrealable(idProcedure, TypeEtapeProcedure.REPONSE);
        exigerRemise(requete.modeRemise(), requete.preuve());
        etapes.save(nouvelleEtape(procedure, TypeEtapeProcedure.CONVOCATION, requete));
        procedure.setStatut(StatutProcedure.ENTRETIEN);
        procedures.save(procedure);
        return enDto(procedure);
    }

    @Transactional
    public DisciplineDtos.ProcedureDto enregistrerEntretien(
            Long idProcedure, DisciplineDtos.EtapeRequest requete, String motif) {
        ProcedureDisciplinaire procedure = procedureModifiable(idProcedure, motif);
        if (requete.commentaire() == null || requete.commentaire().isBlank()) {
            throw new BusinessException("COMPTE_RENDU_OBLIGATOIRE",
                    "Le compte rendu d'entretien est obligatoire.");
        }
        etapes.save(nouvelleEtape(procedure, TypeEtapeProcedure.ENTRETIEN, requete));
        procedure.setStatut(StatutProcedure.EN_DELIBERATION);
        procedures.save(procedure);
        audit.enregistrer("ENTRETIEN_TENU", "procedure", idProcedure,
                StatutProcedure.ENTRETIEN.name(), procedure.getStatut().name(), motif, null);
        return enDto(procedure);
    }

    @Transactional
    public DisciplineDtos.ProcedureDto deliberer(Long idProcedure, String motif) {
        ProcedureDisciplinaire procedure = procedureModifiable(idProcedure, motif);
        verifierEtapePrealable(idProcedure, TypeEtapeProcedure.REPONSE);
        GraviteIncident gravite = incidents.findById(procedure.getIdIncident())
                .orElseThrow(() -> BusinessException.notFound("Incident introuvable."))
                .getGraviteRetenue();
        if (gravite == GraviteIncident.GRAVE || gravite == GraviteIncident.TRES_GRAVE) {
            verifierEtapePrealable(idProcedure, TypeEtapeProcedure.ENTRETIEN);
        }
        EtapeProcedure etape = new EtapeProcedure();
        etape.setIdProcedure(idProcedure);
        etape.setType(TypeEtapeProcedure.DELIBERATION);
        etape.setDateEtape(LocalDateTime.now());
        etape.setActeurId(utilisateurCourant.exiger().getId());
        etapes.save(etape);
        procedure.setStatut(StatutProcedure.EN_DELIBERATION);
        procedures.save(procedure);
        return enDto(procedure);
    }

    @Transactional
    public DisciplineDtos.ProcedureDto decider(
            Long idProcedure, DisciplineDtos.DecisionRequest requete, String motif) {
        ProcedureDisciplinaire procedure = procedureModifiable(idProcedure, motif);
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION);
        if (!utilisateurCourant.exiger().getId().equals(procedure.getDecideurId())
                && !utilisateurCourant.aLeRole(Roles.DIRECTION)) {
            throw BusinessException.forbidden("Seul le decideur designe ou la direction peut rendre la decision.");
        }
        if (procedure.getStatut() != StatutProcedure.EN_DELIBERATION
                && procedure.getStatut() != StatutProcedure.ENTRETIEN) {
            throw new BusinessException("DECISION_HORS_SEQUENCE",
                    "La decision doit intervenir apres deliberation.");
        }
        verifierEtapePrealable(idProcedure, TypeEtapeProcedure.DEMANDE_EXPLICATION);
        verifierEtapePrealable(idProcedure, TypeEtapeProcedure.DELIBERATION);
        GraviteIncident gravite = incidents.findById(procedure.getIdIncident())
                .orElseThrow(() -> BusinessException.notFound("Incident introuvable."))
                .getGraviteRetenue();
        if (gravite == GraviteIncident.GRAVE || gravite == GraviteIncident.TRES_GRAVE) {
            verifierEtapePrealable(idProcedure, TypeEtapeProcedure.ENTRETIEN);
        }
        if (requete.piecesReferencees() == null || requete.piecesReferencees().isEmpty()) {
            throw new BusinessException("PIECES_DECISION_OBLIGATOIRES",
                    "La decision ecrite doit referencer au moins une piece du dossier.");
        }
        for (Long idPiece : requete.piecesReferencees()) {
            if (pieces.findById(idPiece).isEmpty()) {
                throw BusinessException.notFound("Piece " + idPiece + " introuvable.");
            }
        }
        EtapeProcedure etape = new EtapeProcedure();
        etape.setIdProcedure(idProcedure);
        etape.setType(TypeEtapeProcedure.DECISION);
        etape.setDateEtape(LocalDateTime.now());
        etape.setActeurId(utilisateurCourant.exiger().getId());
        etape.setCommentaire(requete.motivation());
        etapes.save(etape);
        procedure.setDecision(requete.decision());
        procedure.setMotivation(requete.motivation());
        procedure.setPiecesReferencees(requete.piecesReferencees().stream()
                .map(String::valueOf).reduce((a, b) -> a + "," + b).orElse(""));
        procedure.setDateDecision(LocalDateTime.now());
        procedure.setStatut(StatutProcedure.DECIDEE);
        procedures.save(procedure);
        audit.enregistrer("DECISION_RENDUE", "procedure", idProcedure,
                StatutProcedure.EN_DELIBERATION.name(), procedure.getStatut().name(), motif, null);
        return enDto(procedure);
    }

    @Transactional
    public DisciplineDtos.ProcedureDto notifier(
            Long idProcedure, DisciplineDtos.NotificationRequest requete, String motif) {
        ProcedureDisciplinaire procedure = procedureModifiable(idProcedure, motif);
        if (procedure.getStatut() != StatutProcedure.DECIDEE
                && procedure.getStatut() != StatutProcedure.CONTESTEE) {
            throw new BusinessException("NOTIFICATION_HORS_SEQUENCE",
                    "La sanction ne peut etre notifiee qu'apres decision.");
        }
        exigerRemise(requete.modeRemise(), requete.preuve());
        DisciplineDtos.SanctionDto sanction = sanctionService.notifier(procedure, requete, motif);
        EtapeProcedure etape = new EtapeProcedure();
        etape.setIdProcedure(idProcedure);
        etape.setType(TypeEtapeProcedure.NOTIFICATION);
        etape.setDateEtape(LocalDateTime.now());
        etape.setActeurId(utilisateurCourant.exiger().getId());
        etape.setModeRemise(requete.modeRemise());
        etape.setPreuve(requete.preuve());
        etape.setCommentaire(sanction.type().name());
        etapes.save(etape);
        procedure.setStatut(StatutProcedure.NOTIFIEE);
        procedures.save(procedure);
        audit.enregistrer("SANCTION_NOTIFIEE", "procedure", idProcedure,
                StatutProcedure.DECIDEE.name(), procedure.getStatut().name(), motif, null);
        return enDto(procedure);
    }

    @Transactional
    public DisciplineDtos.ProcedureDto contester(
            Long idProcedure, DisciplineDtos.RecoursRequest requete, String motif) {
        ProcedureDisciplinaire procedure = procedureModifiable(idProcedure, motif);
        if (procedure.getStatut() != StatutProcedure.NOTIFIEE) {
            throw new BusinessException("RECOURS_HORS_SEQUENCE",
                    "Le recours n'est possible qu'apres notification.");
        }
        EtapeProcedure etape = new EtapeProcedure();
        etape.setIdProcedure(idProcedure);
        etape.setType(TypeEtapeProcedure.RECOURS);
        etape.setDateEtape(LocalDateTime.now());
        etape.setActeurId(procedure.getIdEmploye());
        etape.setCommentaire(requete.motif());
        etapes.save(etape);
        procedure.setStatut(StatutProcedure.CONTESTEE);
        procedures.save(procedure);
        sanctionService.contester(idProcedure);
        audit.enregistrer("SANCTION_CONTESTEE", "procedure", idProcedure,
                StatutProcedure.NOTIFIEE.name(), procedure.getStatut().name(), motif, null);
        return enDto(procedure);
    }

    @Transactional
    public DisciplineDtos.ProcedureDto cloturer(Long idProcedure, String motif) {
        ProcedureDisciplinaire procedure = procedureModifiable(idProcedure, motif);
        if (procedure.getStatut() != StatutProcedure.NOTIFIEE
                && procedure.getStatut() != StatutProcedure.CONTESTEE) {
            throw new BusinessException("CLOTURE_HORS_SEQUENCE",
                    "La procedure doit etre notifiee avant cloture.");
        }
        procedure.setStatut(StatutProcedure.CLOTUREE);
        procedure.setDateCloture(LocalDateTime.now());
        procedures.save(procedure);
        audit.enregistrer("PROCEDURE_CLOTUREE", "procedure", idProcedure,
                StatutProcedure.NOTIFIEE.name(), procedure.getStatut().name(), motif, null);
        return enDto(procedure);
    }

    @Transactional
    public DisciplineDtos.ProcedureDto annuler(Long idProcedure, String motif) {
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION);
        journal.exigerMotif(motif);
        ProcedureDisciplinaire procedure = procedures.findById(idProcedure)
                .orElseThrow(() -> BusinessException.notFound("Procedure introuvable."));
        procedure.setStatut(StatutProcedure.ANNULEE);
        procedures.save(procedure);
        sanctionService.annulerProcedure(idProcedure);
        audit.enregistrer("PROCEDURE_ANNULEE", "procedure", idProcedure,
                null, procedure.getStatut().name(), motif, null);
        return enDto(procedure);
    }

    @Transactional(readOnly = true)
    public boolean licenciementDecide(Long idProcedure, Long idEmploye) {
        if (idProcedure == null || idEmploye == null) {
            return false;
        }
        return procedures.findById(idProcedure)
                .filter(procedure -> idEmploye.equals(procedure.getIdEmploye()))
                .filter(procedure -> procedure.getStatut() == StatutProcedure.DECIDEE)
                .map(ProcedureDisciplinaire::getDecision)
                .filter(TypeSanction.LICENCIEMENT_DISCIPLINAIRE.name()::equals)
                .isPresent();
    }

    private ProcedureDisciplinaire procedureModifiable(Long idProcedure, String motif) {
        acces.exigerAccesProcedure(idProcedure, NiveauAccesDisciplinaire.ECRITURE, motif);
        ProcedureDisciplinaire procedure = procedures.findById(idProcedure)
                .orElseThrow(() -> BusinessException.notFound("Procedure introuvable."));
        if (procedure.getStatut() == StatutProcedure.CLOTUREE
                || procedure.getStatut() == StatutProcedure.ANNULEE
                || procedure.getStatut() == StatutProcedure.CLASSEE) {
            throw new BusinessException("PROCEDURE_TERMINEE",
                    "Cette procedure est terminee et ne peut plus etre modifiee.");
        }
        return procedure;
    }

    private EtapeProcedure nouvelleEtape(ProcedureDisciplinaire procedure, TypeEtapeProcedure type,
                                         DisciplineDtos.EtapeRequest requete) {
        EtapeProcedure etape = new EtapeProcedure();
        etape.setIdProcedure(procedure.getId());
        etape.setType(type);
        etape.setDateEtape(LocalDateTime.now());
        etape.setActeurId(utilisateurCourant.exiger().getId());
        etape.setModeRemise(requete.modeRemise());
        etape.setPreuve(requete.preuve());
        etape.setDelaiReponseJours(requete.delaiReponseJours());
        etape.setIdDocument(requete.idDocument());
        etape.setCommentaire(requete.commentaire());
        return etape;
    }

    private void verifierEtapePrealable(Long idProcedure, TypeEtapeProcedure type) {
        if (!etapes.existsByIdProcedureAndType(idProcedure, type)) {
            throw new BusinessException("ETAPE_MANQUANTE",
                    "L'etape " + type.name() + " est obligatoire avant cette operation.");
        }
    }

    private void exigerRemise(String modeRemise, String preuve) {
        if (modeRemise == null || modeRemise.isBlank() || preuve == null || preuve.isBlank()) {
            throw new BusinessException("PREUVE_NOTIFICATION_OBLIGATOIRE",
                    "Le mode de remise et la preuve de notification sont obligatoires.");
        }
    }

    private DisciplineDtos.ProcedureDto enDto(ProcedureDisciplinaire procedure) {
        List<DisciplineDtos.EtapeDto> listeEtapes = etapes
                .findByIdProcedureOrderByDateEtapeAsc(procedure.getId()).stream()
                .map(DisciplineDtos.EtapeDto::de).toList();
        List<DisciplineDtos.SanctionDto> listeSanctions = sanctions.findByIdProcedure(procedure.getId())
                .stream().map(DisciplineDtos.SanctionDto::de).toList();
        return DisciplineDtos.ProcedureDto.de(procedure, listeEtapes, listeSanctions);
    }
}
