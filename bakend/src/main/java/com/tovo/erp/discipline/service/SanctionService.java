package com.tovo.erp.discipline.service;

import com.tovo.erp.contrat.dto.CarriereDtos;
import com.tovo.erp.contrat.service.CarriereService;
import com.tovo.erp.discipline.dto.DisciplineDtos;
import com.tovo.erp.discipline.model.AbsenceDisciplinaire;
import com.tovo.erp.discipline.model.Incident;
import com.tovo.erp.discipline.model.ProcedureDisciplinaire;
import com.tovo.erp.discipline.model.SanctionDisciplinaire;
import com.tovo.erp.discipline.model.StatutSanction;
import com.tovo.erp.discipline.model.TypeSanction;
import com.tovo.erp.discipline.repository.AbsenceDisciplinaireRepository;
import com.tovo.erp.discipline.repository.IncidentRepository;
import com.tovo.erp.discipline.repository.SanctionRepository;
import com.tovo.erp.shared.BusinessException;
import com.tovo.erp.shared.Roles;
import com.tovo.erp.shared.UtilisateurCourant;
import com.tovo.erp.shared.audit.JournalAuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class SanctionService {

    private static final List<StatutSanction> SANCTIONS_ACTIVES = List.of(
            StatutSanction.NOTIFIEE, StatutSanction.APPLIQUEE, StatutSanction.CONTESTEE);
    private static final Map<TypeSanction, Integer> DUREE_EFFACEMENT_MOIS = Map.of(
            TypeSanction.AVERTISSEMENT_VERBAL, 0,
            TypeSanction.AVERTISSEMENT_ECRIT, 6,
            TypeSanction.BLAME, 12,
            TypeSanction.MISE_A_PIED, 24,
            TypeSanction.RETROGRADATION, 36,
            TypeSanction.LICENCIEMENT_DISCIPLINAIRE, 60);

    private final SanctionRepository sanctions;
    private final AbsenceDisciplinaireRepository absences;
    private final IncidentRepository incidents;
    private final CarriereService carriere;
    private final UtilisateurCourant utilisateurCourant;
    private final JournalAuditService audit;
    private final JournalDisciplineService journal;

    public SanctionService(SanctionRepository sanctions,
                           AbsenceDisciplinaireRepository absences,
                           IncidentRepository incidents,
                           CarriereService carriere,
                           UtilisateurCourant utilisateurCourant,
                           JournalAuditService audit,
                           JournalDisciplineService journal) {
        this.sanctions = sanctions;
        this.absences = absences;
        this.incidents = incidents;
        this.carriere = carriere;
        this.utilisateurCourant = utilisateurCourant;
        this.audit = audit;
        this.journal = journal;
    }

    @Transactional(readOnly = true)
    public List<DisciplineDtos.SanctionDto> lister(Long idEmploye) {
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION);
        List<SanctionDisciplinaire> liste = idEmploye == null
                ? sanctions.findAllByOrderByDateNotificationDesc()
                : sanctions.findByIdEmployeOrderByDateNotificationDesc(idEmploye);
        return liste.stream().map(DisciplineDtos.SanctionDto::de).toList();
    }

    @Transactional
    public DisciplineDtos.SanctionDto notifier(ProcedureDisciplinaire procedure,
                                               DisciplineDtos.NotificationRequest requete,
                                               String motif) {
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION);
        journal.exigerMotif(motif);
        Incident incident = incidents.findById(procedure.getIdIncident())
                .orElseThrow(() -> BusinessException.notFound("Incident introuvable."));
        verifierProportionnalite(requete.type(), incident);
        if (sanctions.existsByIdProcedureAndStatutIn(procedure.getId(), SANCTIONS_ACTIVES)) {
            throw new BusinessException("NON_BIS_IN_IDEM",
                    "Une seule sanction active peut etre notifiee pour une meme faute.");
        }
        String decision = procedure.getDecision();
        if (decision == null || !decision.toUpperCase().contains(requete.type().name())) {
            throw new BusinessException("SANCTION_HORS_DECISION",
                    "La sanction notifiee doit correspondre a la decision rendue.");
        }
        if (requete.baseReglement() == null || requete.baseReglement().isBlank()) {
            throw new BusinessException("BASE_REGLEMENT_OBLIGATOIRE",
                    "La base reglementaire de la sanction est obligatoire.");
        }
        if (requete.type() == TypeSanction.MISE_A_PIED
                && (requete.dureeJours() == null || requete.dureeJours() < 1
                || requete.dureeJours() > 30)) {
            throw new BusinessException("DUREE_MISE_A_PIED_INVALIDE",
                    "La duree de la mise a pied doit etre comprise entre 1 et 30 jours.");
        }
        BigDecimal joursImpact = calculerImpact(procedure, requete);

        SanctionDisciplinaire sanction = new SanctionDisciplinaire();
        sanction.setIdProcedure(procedure.getId());
        sanction.setIdEmploye(procedure.getIdEmploye());
        sanction.setType(requete.type());
        sanction.setDateNotification(LocalDateTime.now());
        sanction.setDateEffet(requete.dateEffet());
        sanction.setDureeJours(requete.dureeJours());
        sanction.setMotif(requete.motif());
        sanction.setBaseReglement(requete.baseReglement());
        sanction.setStatut(StatutSanction.NOTIFIEE);
        sanction.setDateEffacement(calculerEffacement(requete.type(), requete.dateEffet()));
        sanction.setImpactPaieJours(joursImpact);
        SanctionDisciplinaire creee = sanctions.save(sanction);

        if (requete.type() == TypeSanction.MISE_A_PIED && requete.dureeJours() != null) {
            enregistrerAbsence(creee, procedure, requete, joursImpact);
        }
        appliquerEffetsCarriere(creee, procedure, motif);
        audit.enregistrer("SANCTION_NOTIFIEE", "sanction", creee.getId(), null,
                creee.getType().name(), motif, null);
        journal.enregistrer(utilisateurCourant.exiger().getId(), "SANCTION_NOTIFIEE", "sanction",
                creee.getId(), motif, creee.getType().name());
        return DisciplineDtos.SanctionDto.de(creee);
    }

    @Transactional
    public void contester(Long idProcedure) {
        sanctions.findByIdProcedure(idProcedure).stream()
                .filter(s -> s.getStatut() == StatutSanction.NOTIFIEE
                        || s.getStatut() == StatutSanction.APPLIQUEE)
                .forEach(s -> {
                    s.setStatut(StatutSanction.CONTESTEE);
                    sanctions.save(s);
                });
    }

    @Transactional
    public void annulerProcedure(Long idProcedure) {
        sanctions.findByIdProcedure(idProcedure).forEach(s -> {
            s.setStatut(StatutSanction.ANNULEE);
            sanctions.save(s);
        });
        absences.findByIdSanctionIn(sanctions.findByIdProcedure(idProcedure).stream()
                .map(SanctionDisciplinaire::getId).toList()).forEach(a -> {
            a.setTraiteePaie(false);
            absences.save(a);
        });
    }

    @Transactional
    public int effacerEchus(LocalDate date) {
        List<SanctionDisciplinaire> aEffacer = sanctions.findByStatutInAndDateEffacementBefore(
                List.of(StatutSanction.NOTIFIEE, StatutSanction.APPLIQUEE, StatutSanction.CONTESTEE), date);
        for (SanctionDisciplinaire sanction : aEffacer) {
            sanction.setStatut(StatutSanction.EFFACEE);
            sanctions.save(sanction);
        }
        return aEffacer.size();
    }

    @Transactional(readOnly = true)
    public List<DisciplineDtos.VariablePaieDto> variables(LocalDate debut, LocalDate fin) {
        return absences.findByTraiteePaieFalseAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
                        fin, debut).stream()
                .map(a -> new DisciplineDtos.VariablePaieDto(a.getId(), a.getIdEmploye(),
                        "ABSENCE_DISCIPLINAIRE", a.getJours(),
                        a.isRemuneree() ? BigDecimal.ZERO : a.getJours(),
                        a.isRemuneree(), a.getMotif()))
                .toList();
    }

    @Transactional
    public DisciplineDtos.AbsenceDto marquerTraitee(Long idAbsence) {
        utilisateurCourant.exigerRole(Roles.FINANCE, Roles.RH);
        AbsenceDisciplinaire absence = absences.findById(idAbsence)
                .orElseThrow(() -> BusinessException.notFound("Absence disciplinaire introuvable."));
        absence.setTraiteePaie(true);
        return DisciplineDtos.AbsenceDto.de(absences.save(absence));
    }

    @Transactional(readOnly = true)
    public List<DisciplineDtos.AbsenceDto> absences(Long idEmploye) {
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION);
        return absences.findAll().stream()
                .filter(a -> idEmploye == null || a.getIdEmploye().equals(idEmploye))
                .map(DisciplineDtos.AbsenceDto::de).toList();
    }

    private void verifierProportionnalite(TypeSanction type, Incident incident) {
        var gravite = incident.getGraviteRetenue();
        if (gravite == null) {
            throw new BusinessException("INCIDENT_NON_QUALIFIE",
                    "Aucune sanction ne peut etre notifiee sur un incident non qualifie.");
        }
        boolean autorise = switch (type) {
            case AVERTISSEMENT_VERBAL, AVERTISSEMENT_ECRIT -> true;
            case BLAME -> gravite.ordinal() >= 1;
            case MISE_A_PIED -> gravite.ordinal() >= 1;
            case RETROGRADATION -> gravite.ordinal() >= 2;
            case LICENCIEMENT_DISCIPLINAIRE -> gravite.ordinal() >= 3;
        };
        if (!autorise) {
            throw new BusinessException("SANCTION_DISPROPORTIONNEE",
                    "La sanction " + type.name() + " est disproportionnee pour la gravite "
                            + gravite.name() + ".");
        }
    }

    private BigDecimal calculerImpact(ProcedureDisciplinaire procedure,
                                      DisciplineDtos.NotificationRequest requete) {
        if (requete.type() != TypeSanction.MISE_A_PIED || requete.dureeJours() == null) {
            return BigDecimal.ZERO;
        }
        if (Boolean.TRUE.equals(procedure.getMiseAPiedRemuneree())) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(requete.dureeJours());
    }

    private LocalDate calculerEffacement(TypeSanction type, LocalDate dateEffet) {
        int mois = DUREE_EFFACEMENT_MOIS.get(type);
        return dateEffet.plusMonths(mois);
    }

    private void enregistrerAbsence(SanctionDisciplinaire sanction, ProcedureDisciplinaire procedure,
                                    DisciplineDtos.NotificationRequest requete, BigDecimal joursImpact) {
        AbsenceDisciplinaire absence = new AbsenceDisciplinaire();
        absence.setIdSanction(sanction.getId());
        absence.setIdEmploye(sanction.getIdEmploye());
        absence.setDateDebut(requete.dateEffet());
        absence.setDateFin(requete.dateEffet().plusDays(requete.dureeJours() - 1L));
        absence.setJours(BigDecimal.valueOf(requete.dureeJours()));
        absence.setRemuneree(Boolean.TRUE.equals(procedure.getMiseAPiedRemuneree()));
        absence.setMotif("Mise a pied disciplinaire - " + sanction.getType().name());
        absences.save(absence);
    }

    private void appliquerEffetsCarriere(SanctionDisciplinaire sanction,
                                         ProcedureDisciplinaire procedure, String motif) {
        String type = sanction.getType() == TypeSanction.MISE_A_PIED ? "SUSPENSION" : "SANCTION";
        carriere.enregistrerEvenementSimple(sanction.getIdEmploye(),
                new CarriereDtos.EvenementSimpleRequest(type, sanction.getDateEffet(),
                        null, sanction.getType().name(), motif),
                utilisateurCourant.exiger().getId());
    }
}
