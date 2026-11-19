package com.tovo.erp.discipline.service;

import com.tovo.erp.contrat.service.CarriereService;
import com.tovo.erp.discipline.dto.DisciplineDtos;
import com.tovo.erp.discipline.model.CategorieIncident;
import com.tovo.erp.discipline.model.GraviteIncident;
import com.tovo.erp.discipline.model.Incident;
import com.tovo.erp.discipline.model.ProcedureDisciplinaire;
import com.tovo.erp.discipline.model.StatutIncident;
import com.tovo.erp.discipline.model.StatutProcedure;
import com.tovo.erp.discipline.model.TypeEtapeProcedure;
import com.tovo.erp.discipline.model.TypeSanction;
import com.tovo.erp.discipline.repository.AbsenceDisciplinaireRepository;
import com.tovo.erp.discipline.repository.EtapeRepository;
import com.tovo.erp.discipline.repository.IncidentEmployeRepository;
import com.tovo.erp.discipline.repository.IncidentRepository;
import com.tovo.erp.discipline.repository.IncidentTemoinRepository;
import com.tovo.erp.discipline.repository.PieceRepository;
import com.tovo.erp.discipline.repository.ProcedureRepository;
import com.tovo.erp.discipline.repository.SanctionRepository;
import com.tovo.erp.employe.service.EmployeService;
import com.tovo.erp.shared.BusinessException;
import com.tovo.erp.shared.Roles;
import com.tovo.erp.shared.UtilisateurConnecte;
import com.tovo.erp.shared.UtilisateurCourant;
import com.tovo.erp.shared.audit.JournalAuditService;
import com.tovo.erp.shared.reference.ReferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisciplineServiceTest {

    @Mock private IncidentRepository incidents;
    @Mock private IncidentEmployeRepository incidentEmployes;
    @Mock private IncidentTemoinRepository temoins;
    @Mock private ProcedureRepository procedures;
    @Mock private EtapeRepository etapes;
    @Mock private PieceRepository pieces;
    @Mock private SanctionRepository sanctions;
    @Mock private AbsenceDisciplinaireRepository absences;
    @Mock private EmployeService employes;
    @Mock private ReferenceService references;
    @Mock private UtilisateurCourant utilisateurCourant;
    @Mock private JournalAuditService audit;
    @Mock private JournalDisciplineService journal;
    @Mock private AccesDisciplinaireService acces;
    @Mock private ProcedureService procedureService;
    @Mock private SanctionService sanctionService;
    @Mock private CarriereService carriere;

    private IncidentService incidentService;
    private ProcedureService procedureServiceReel;
    private SanctionService sanctionServiceReel;

    @BeforeEach
    void setUp() {
        incidentService = new IncidentService(incidents, incidentEmployes, temoins, employes,
                references, utilisateurCourant, audit, journal, acces, procedureService);
        procedureServiceReel = new ProcedureService(procedures, etapes, incidents, pieces, sanctions,
                employes, references, utilisateurCourant, audit, journal, acces, sanctionService);
        sanctionServiceReel = new SanctionService(sanctions, absences, incidents, carriere,
                utilisateurCourant, audit, journal);
    }

    @Test
    void refuseUneDescriptionAppreciative() {
        DisciplineDtos.IncidentRequest requete = new DisciplineDtos.IncidentRequest(
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 2), "Atelier",
                "Le salarie semble avoir ete en retard de maniere repetee et il me semble volontaire.",
                CategorieIncident.RETARDS_REPETES, GraviteIncident.LEGERE, List.of(), List.of());

        BusinessException erreur = assertThrows(BusinessException.class,
                () -> incidentService.creer(requete, "Signalement atelier"));

        assertEquals("DESCRIPTION_APPRECIATIVE", erreur.getCode());
    }

    @Test
    void creeUnIncidentAvecSaReference() {
        when(references.suivante("INCIDENT", "INC", 4)).thenReturn("INC-2026-0001");
        when(utilisateurCourant.exiger()).thenReturn(utilisateur());
        when(incidents.save(any())).thenAnswer(invocation -> {
            Incident incident = invocation.getArgument(0);
            incident.setId(1L);
            return incident;
        });
        DisciplineDtos.IncidentRequest requete = new DisciplineDtos.IncidentRequest(
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 2), "Atelier",
                "Le salarie est arrive avec 45 minutes de retard trois fois cette semaine.",
                CategorieIncident.RETARDS_REPETES, GraviteIncident.LEGERE, List.of(), List.of());

        DisciplineDtos.IncidentDto resultat = incidentService.creer(requete, "Signalement atelier");

        assertEquals("INC-2026-0001", resultat.reference());
        assertEquals(StatutIncident.DECLARE, resultat.statut());
    }

    @Test
    void demandeExplicationExigeUnePreuveDeNotification() {
        ProcedureDisciplinaire procedure = procedure(3L, StatutProcedure.OUVERTE);
        when(procedures.findById(3L)).thenReturn(Optional.of(procedure));
        when(etapes.existsByIdProcedureAndType(3L, TypeEtapeProcedure.DEMANDE_EXPLICATION))
                .thenReturn(false);

        BusinessException erreur = assertThrows(BusinessException.class,
                () -> procedureServiceReel.demanderExplication(3L,
                        new DisciplineDtos.EtapeRequest(TypeEtapeProcedure.DEMANDE_EXPLICATION,
                                "EMAIL", null, 5, null, null), "Instruction"));

        assertEquals("PREUVE_NOTIFICATION_OBLIGATOIRE", erreur.getCode());
    }

    @Test
    void refuseUneSanctionDisproportionnee() {
        ProcedureDisciplinaire procedure = procedure(3L, StatutProcedure.DECIDEE);
        procedure.setDecision(TypeSanction.LICENCIEMENT_DISCIPLINAIRE.name());
        when(incidents.findById(1L)).thenReturn(Optional.of(incident(GraviteIncident.GRAVE)));

        BusinessException erreur = assertThrows(BusinessException.class,
                () -> sanctionServiceReel.notifier(procedure,
                        new DisciplineDtos.NotificationRequest(
                                TypeSanction.LICENCIEMENT_DISCIPLINAIRE,
                                LocalDate.of(2026, 5, 1), null, "Faute grave constatee",
                                "Code du travail", "EMAIL", "accuse-reception"),
                        "Notification"));

        assertEquals("SANCTION_DISPROPORTIONNEE", erreur.getCode());
    }

    @Test
    void notifieUneMiseAPiedEtGenereAbsenceEtEvenementCarriere() {
        ProcedureDisciplinaire procedure = procedure(3L, StatutProcedure.DECIDEE);
        procedure.setDecision(TypeSanction.MISE_A_PIED.name());
        procedure.setMiseAPiedRemuneree(false);
        when(incidents.findById(1L)).thenReturn(Optional.of(incident(GraviteIncident.GRAVE)));
        when(sanctions.existsByIdProcedureAndStatutIn(any(), any())).thenReturn(false);
        when(utilisateurCourant.exiger()).thenReturn(utilisateur());
        when(sanctions.save(any())).thenAnswer(invocation -> {
            com.tovo.erp.discipline.model.SanctionDisciplinaire sanction = invocation.getArgument(0);
            sanction.setId(7L);
            return sanction;
        });

        DisciplineDtos.SanctionDto resultat = sanctionServiceReel.notifier(procedure,
                new DisciplineDtos.NotificationRequest(TypeSanction.MISE_A_PIED,
                        LocalDate.of(2026, 5, 1), 3, "Faute moyenne constatee",
                        "Code du travail", "EMAIL", "accuse-reception"),
                "Notification");

        assertEquals(3, resultat.dureeJours());
        assertEquals(java.math.BigDecimal.valueOf(3), resultat.impactPaieJours());
        verify(absences).save(any());
        verify(carriere).enregistrerEvenementSimple(any(), any(), any());
    }

    private UtilisateurConnecte utilisateur() {
        return new UtilisateurConnecte(9L, "RH", "rh@erp.test", Roles.RH, 2L, "RH");
    }

    private ProcedureDisciplinaire procedure(Long id, StatutProcedure statut) {
        ProcedureDisciplinaire procedure = new ProcedureDisciplinaire();
        procedure.setId(id);
        procedure.setIdIncident(1L);
        procedure.setIdEmploye(5L);
        procedure.setStatut(statut);
        return procedure;
    }

    private Incident incident(GraviteIncident gravite) {
        Incident incident = new Incident();
        incident.setId(1L);
        incident.setGraviteRetenue(gravite);
        return incident;
    }
}
