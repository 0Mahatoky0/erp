package com.tovo.erp.evaluation.service;

import com.tovo.erp.conges.dto.CongeDtos;
import com.tovo.erp.conges.service.CongeService;
import com.tovo.erp.employe.service.EmployeService;
import com.tovo.erp.evaluation.dto.EvaluationDtos;
import com.tovo.erp.evaluation.model.Formation;
import com.tovo.erp.evaluation.model.InscriptionFormation;
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
import com.tovo.erp.shared.UtilisateurCourant;
import com.tovo.erp.shared.audit.JournalAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FormationServiceTest {

    @Mock FormationRepository formations;
    @Mock SessionFormationRepository sessions;
    @Mock InscriptionFormationRepository inscriptions;
    @Mock PlanFormationRepository plans;
    @Mock EvaluationFormationRepository evaluationsFormation;
    @Mock EvaluationRepository evaluations;
    @Mock ConventionFormationRepository conventions;
    @Mock BesoinFormationRepository besoins;
    @Mock EcartCompetenceRepository ecarts;
    @Mock EmployeService employes;
    @Mock CongeService conges;
    @Mock ReferentielService referentiel;
    @Mock UtilisateurCourant utilisateurCourant;
    @Mock JournalAuditService audit;

    FormationService service;

    @BeforeEach
    void setUp() {
        service = new FormationService(formations, sessions, inscriptions, plans, evaluationsFormation,
                evaluations, conventions, besoins, ecarts, employes, conges, referentiel,
                utilisateurCourant, audit);
    }

    @Test
    void demandeInscriptionRefuseChevauchementConge() {
        SessionFormation session = session(1L, 1);
        Formation formation = formation(1L);
        when(sessions.findById(1L)).thenReturn(Optional.of(session));
        when(formations.findById(1L)).thenReturn(Optional.of(formation));
        when(conges.planningConges(10L, session.getDateDebut(), session.getDateFin())).thenReturn(List.of(
                new CongeDtos.PlanningDto(10L, session.getDateDebut(), session.getDateFin(),
                        BigDecimal.valueOf(5), "CONGE_PAYE", "Vacances")));

        BusinessException erreur = assertThrows(BusinessException.class,
                () -> service.demanderInscription(new EvaluationDtos.InscriptionRequest(1L, 10L, null)));

        assertEquals("FORMATION_CHEVAUCHE_CONGE", erreur.getCode());
    }

    @Test
    void confirmationPlaceEnListeAttenteQuandCapaciteAtteinte() {
        SessionFormation session = session(1L, 1);
        Formation formation = formation(1L);
        InscriptionFormation cible = inscription(2L, "VALIDEE_MANAGER");
        InscriptionFormation occupante = inscription(3L, "CONFIRMEE");
        when(inscriptions.findById(2L)).thenReturn(Optional.of(cible));
        when(sessions.findById(1L)).thenReturn(Optional.of(session));
        when(formations.findById(1L)).thenReturn(Optional.of(formation));
        when(conges.planningConges(10L, session.getDateDebut(), session.getDateFin())).thenReturn(List.of());
        when(inscriptions.findByIdSession(1L)).thenReturn(List.of(occupante));
        when(inscriptions.save(any(InscriptionFormation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EvaluationDtos.InscriptionDto resultat = service.confirmerInscription(2L);

        assertEquals("LISTE_ATTENTE", resultat.statut());
    }

    private SessionFormation session(Long id, int capacite) {
        SessionFormation session = new SessionFormation();
        session.setId(id);
        session.setIdFormation(1L);
        session.setDateDebut(LocalDate.now().plusMonths(1));
        session.setDateFin(LocalDate.now().plusMonths(1).plusDays(2));
        session.setCapacite(capacite);
        session.setCout(BigDecimal.valueOf(500000));
        session.setStatut("PREVUE");
        return session;
    }

    private Formation formation(Long id) {
        Formation formation = new Formation();
        formation.setId(id);
        formation.setTitre("Gestion avancée");
        return formation;
    }

    private InscriptionFormation inscription(Long id, String statut) {
        InscriptionFormation inscription = new InscriptionFormation();
        inscription.setId(id);
        inscription.setIdSession(1L);
        inscription.setIdEmploye(10L);
        inscription.setStatut(statut);
        return inscription;
    }
}
