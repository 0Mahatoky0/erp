package com.tovo.erp.discipline.service;

import com.tovo.erp.discipline.dto.DisciplineDtos;
import com.tovo.erp.discipline.model.CategorieIncident;
import com.tovo.erp.discipline.model.GraviteIncident;
import com.tovo.erp.discipline.model.Incident;
import com.tovo.erp.discipline.model.IncidentEmploye;
import com.tovo.erp.discipline.model.IncidentTemoin;
import com.tovo.erp.discipline.model.StatutIncident;
import com.tovo.erp.discipline.repository.IncidentEmployeRepository;
import com.tovo.erp.discipline.repository.IncidentRepository;
import com.tovo.erp.discipline.repository.IncidentTemoinRepository;
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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class IncidentService {

    private static final Pattern APPRECIATION = Pattern.compile(
            "(?i)\\b(je pense|je crois|probablement|visiblement|semble|il me semble|apparemment)\\b");
    private static final int DELAI_PRESCRIPTION_MOIS = 2;

    private final IncidentRepository incidents;
    private final IncidentEmployeRepository incidentEmployes;
    private final IncidentTemoinRepository temoins;
    private final EmployeService employes;
    private final ReferenceService references;
    private final UtilisateurCourant utilisateurCourant;
    private final JournalAuditService audit;
    private final JournalDisciplineService journal;
    private final AccesDisciplinaireService acces;
    private final ProcedureService procedures;

    public IncidentService(IncidentRepository incidents,
                           IncidentEmployeRepository incidentEmployes,
                           IncidentTemoinRepository temoins,
                           EmployeService employes,
                           ReferenceService references,
                           UtilisateurCourant utilisateurCourant,
                           JournalAuditService audit,
                           JournalDisciplineService journal,
                           AccesDisciplinaireService acces,
                           ProcedureService procedures) {
        this.incidents = incidents;
        this.incidentEmployes = incidentEmployes;
        this.temoins = temoins;
        this.employes = employes;
        this.references = references;
        this.utilisateurCourant = utilisateurCourant;
        this.audit = audit;
        this.journal = journal;
        this.acces = acces;
        this.procedures = procedures;
    }

    @Transactional(readOnly = true)
    public List<DisciplineDtos.IncidentDto> lister(String motif) {
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION);
        journal.exigerMotif(motif);
        return incidents.findAllByOrderByDateDeclarationDesc().stream().map(this::enDto).toList();
    }

    @Transactional(readOnly = true)
    public DisciplineDtos.IncidentDto incident(Long idIncident, String motif) {
        acces.exigerAccesIncident(idIncident, motif);
        return enDto(incidents.findById(idIncident)
                .orElseThrow(() -> BusinessException.notFound("Incident introuvable.")));
    }

    @Transactional
    public DisciplineDtos.IncidentDto creer(DisciplineDtos.IncidentRequest requete, String motif) {
        utilisateurCourant.exigerRole(Roles.MANAGER, Roles.RH, Roles.DIRECTION);
        journal.exigerMotif(motif);
        verifierDescription(requete.description());
        LocalDate aujourdHui = LocalDate.now();
        if (requete.dateFaits().isAfter(aujourdHui) || requete.dateConnaissance().isAfter(aujourdHui)) {
            throw new BusinessException("INCIDENT_DATE_FUTURE",
                    "Les dates de faits et de connaissance ne peuvent pas etre futures.");
        }
        if (requete.dateFaits().isAfter(requete.dateConnaissance())) {
            throw new BusinessException("INCIDENT_DATE_INCOHERENTE",
                    "La date des faits doit etre anterieure ou egale a la date de connaissance.");
        }
        verifierEmployes(requete.employesConcernes());

        Incident incident = new Incident();
        incident.setReference(references.suivante("INCIDENT", "INC", 4));
        incident.setDeclarantId(utilisateurCourant.exiger().getId());
        incident.setDateDeclaration(LocalDateTime.now());
        incident.setDateFaits(requete.dateFaits());
        incident.setDateConnaissance(requete.dateConnaissance());
        incident.setLieu(requete.lieu());
        incident.setDescription(requete.description().trim());
        incident.setCategorie(requete.categorie());
        incident.setGraviteInitiale(requete.graviteInitiale());
        incident.setStatut(StatutIncident.DECLARE);
        Incident cree = incidents.save(incident);

        enregistrerEmployes(cree.getId(), requete.employesConcernes());
        enregistrerTemoins(cree.getId(), requete.temoins());
        audit.enregistrer("INCIDENT_DECLARE", "incident", cree.getId(), null,
                cree.getReference(), motif, null);
        journal.enregistrer(utilisateurCourant.exiger().getId(), "INCIDENT_DECLARE", "incident",
                cree.getId(), motif, cree.getReference());
        return enDto(cree);
    }

    @Transactional
    public DisciplineDtos.IncidentDto qualifier(Long idIncident,
                                                DisciplineDtos.QualificationRequest requete) {
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION);
        String motif = requete.motif();
        journal.exigerMotif(motif);
        Incident incident = incidents.findById(idIncident)
                .orElseThrow(() -> BusinessException.notFound("Incident introuvable."));
        if (incident.getStatut() != StatutIncident.DECLARE) {
            throw new BusinessException("INCIDENT_DEJA_QUALIFIE",
                    "Seul un incident declare peut etre qualifie.");
        }
        if (requete.graviteRetenue() == null) {
            throw new BusinessException("GRAVITE_OBLIGATOIRE",
                    "La gravite retenue est obligatoire.");
        }
        verifierPrescription(incident, requete.derogationPrescription(), motif);
        if (requete.ouvrirProcedure()) {
            List<Long> employesConcernes = empilerUniques(requete.employesConcernes());
            if (employesConcernes.isEmpty()) {
                throw new BusinessException("EMPLOYE_CONCERNE_OBLIGATOIRE",
                        "Une procedure disciplinaire doit viser au moins un salarie.");
            }
            verifierEmployes(employesConcernes);
            verifierSeparationRoles(incident, requete.idInstructeur(), requete.idDecideur(),
                    employesConcernes);
            if (requete.idInstructeur() == null || requete.idDecideur() == null) {
                throw new BusinessException("ROLES_PROCEDURE_OBLIGATOIRES",
                        "L'instructeur et le decideur doivent etre designes.");
            }
            if (requete.miseAPied()
                    && (requete.miseAPiedDebut() == null || requete.miseAPiedFin() == null
                    || requete.miseAPiedFin().isBefore(requete.miseAPiedDebut()))) {
                throw new BusinessException("MISE_A_PIED_DATES_INVALIDE",
                        "Les dates de mise a pied sont invalides.");
            }
            enregistrerEmployes(idIncident, employesConcernes);
        }

        incident.setGraviteRetenue(requete.graviteRetenue());
        incident.setDateQualification(LocalDateTime.now());
        incident.setQualifiePar(utilisateurCourant.exiger().getId());
        if (requete.ouvrirProcedure()) {
            incident.setStatut(StatutIncident.PROCEDURE_OUVERTE);
            for (Long idEmploye : empilerUniques(requete.employesConcernes())) {
                procedures.ouvrir(incident, idEmploye, requete.idInstructeur(),
                        requete.idDecideur(), requete.miseAPied(), requete.miseAPiedDebut(),
                        requete.miseAPiedFin(), motif);
            }
        } else {
            if (motif.isBlank()) {
                throw new BusinessException("MOTIF_CLASSEMENT_OBLIGATOIRE",
                        "Le motif de classement sans suite est obligatoire.");
            }
            incident.setStatut(StatutIncident.CLASSE_SANS_SUITE);
            incident.setMotifClassement(motif);
        }
        incidents.save(incident);
        audit.enregistrer("INCIDENT_QUALIFIE", "incident", idIncident, StatutIncident.DECLARE.name(),
                incident.getStatut().name(), motif, null);
        journal.enregistrer(utilisateurCourant.exiger().getId(), "INCIDENT_QUALIFIE", "incident",
                idIncident, motif, requete.graviteRetenue().name());
        return enDto(incident);
    }

    private void verifierPrescription(Incident incident, boolean derogation, String motif) {
        LocalDate limite = incident.getDateConnaissance().plusMonths(DELAI_PRESCRIPTION_MOIS);
        if (LocalDate.now().isAfter(limite) && !derogation) {
            throw new BusinessException("PRESCRIPTION_ACQUISE",
                    "Le delai de prescription de " + DELAI_PRESCRIPTION_MOIS
                            + " mois est depasse ; une derogation motivee est requise.");
        }
        if (derogation && motif.isBlank()) {
            throw new BusinessException("DEROGATION_MOTIVEE_OBLIGATOIRE",
                    "Toute derogation a la prescription doit etre motivee.");
        }
    }

    private void verifierSeparationRoles(Incident incident, Long instructeurId, Long decideurId,
                                         List<Long> employesConcernes) {
        if (instructeurId.equals(decideurId)) {
            throw new BusinessException("SEPARATION_ROLES",
                    "L'instructeur et le decideur doivent etre deux personnes distinctes.");
        }
        if (instructeurId.equals(incident.getDeclarantId())
                || decideurId.equals(incident.getDeclarantId())) {
            throw new BusinessException("SEPARATION_ROLES",
                    "Le declarant ne peut etre ni instructeur ni decideur.");
        }
        if (employesConcernes.contains(instructeurId) || employesConcernes.contains(decideurId)) {
            throw new BusinessException("SEPARATION_ROLES",
                    "Un salarie concerne ne peut etre instructeur ni decideur.");
        }
    }

    private void verifierDescription(String description) {
        if (description == null || description.trim().length() < 20) {
            throw new BusinessException("DESCRIPTION_INSUFFISANTE",
                    "La description doit contenir au moins 20 caracteres.");
        }
        if (APPRECIATION.matcher(description).find()) {
            throw new BusinessException("DESCRIPTION_APPRECIATIVE",
                    "La description doit relater des faits objectifs, sans appreciation personnelle.");
        }
        for (CategorieIncident categorie : CategorieIncident.values()) {
            if (description.toUpperCase().contains("CATEGORY_" + categorie.name())) {
                throw new BusinessException("DESCRIPTION_CATEGORIE",
                        "La categorie doit etre selectionnee dans le champ dedie.");
            }
        }
    }

    private void verifierEmployes(List<Long> ids) {
        if (ids == null) {
            return;
        }
        for (Long id : ids) {
            if (!employes.existe(id)) {
                throw BusinessException.notFound("Le salarie " + id + " n'existe pas.");
            }
        }
    }

    private void enregistrerEmployes(Long idIncident, List<Long> ids) {
        List<Long> uniques = empilerUniques(ids);
        incidentEmployes.deleteAll(incidentEmployes.findByIdIdIncident(idIncident));
        for (Long idEmploye : uniques) {
            incidentEmployes.save(new IncidentEmploye(idIncident, idEmploye));
        }
    }

    private void enregistrerTemoins(Long idIncident, List<DisciplineDtos.TemoinDto> liste) {
        temoins.deleteAll(temoins.findByIdIncident(idIncident));
        if (liste == null) {
            return;
        }
        for (DisciplineDtos.TemoinDto temoin : liste) {
            verifierEmployes(List.of(temoin.idEmploye()));
            IncidentTemoin entite = new IncidentTemoin();
            entite.setIdIncident(idIncident);
            entite.setIdEmploye(temoin.idEmploye());
            entite.setProtege(temoin.protege());
            temoins.save(entite);
        }
    }

    private List<Long> empilerUniques(List<Long> ids) {
        if (ids == null) {
            return List.of();
        }
        Set<Long> uniques = new LinkedHashSet<>(ids);
        return new ArrayList<>(uniques);
    }

    private DisciplineDtos.IncidentDto enDto(Incident incident) {
        List<Long> employesConcernes = incidentEmployes.findByIdIdIncident(incident.getId()).stream()
                .map(l -> l.getId().getIdEmploye()).toList();
        List<DisciplineDtos.TemoinDto> listeTemoins = temoins.findByIdIncident(incident.getId()).stream()
                .map(t -> new DisciplineDtos.TemoinDto(t.getIdEmploye(), t.isProtege())).toList();
        return new DisciplineDtos.IncidentDto(incident.getId(), incident.getReference(),
                incident.getDeclarantId(), incident.getDateDeclaration(), incident.getDateFaits(),
                incident.getDateConnaissance(), incident.getLieu(), incident.getDescription(),
                incident.getCategorie(), incident.getGraviteInitiale(), incident.getGraviteRetenue(),
                incident.getStatut(), incident.getMotifClassement(), employesConcernes, listeTemoins);
    }
}
