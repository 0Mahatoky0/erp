package com.tovo.erp.evaluation.service;

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
import com.tovo.erp.employe.service.EmployeService;
import com.tovo.erp.shared.Roles;
import com.tovo.erp.shared.UtilisateurConnecte;
import com.tovo.erp.shared.UtilisateurCourant;
import com.tovo.erp.shared.audit.JournalAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceTest {

    @Mock EvaluationRepository evaluations;
    @Mock ObjectifRepository objectifs;
    @Mock EcartCompetenceRepository ecarts;
    @Mock BesoinFormationRepository besoins;
    @Mock CampagneService campagnes;
    @Mock ReferentielService referentiel;
    @Mock EmployeService employes;
    @Mock ContratService contrats;
    @Mock UtilisateurCourant utilisateurCourant;
    @Mock JournalAuditService audit;

    EvaluationService service;

    @BeforeEach
    void setUp() {
        service = new EvaluationService(evaluations, objectifs, ecarts, besoins, campagnes,
                referentiel, employes, contrats, utilisateurCourant, audit);
    }

    @Test
    void clotureGenereEcartsPriorisesEtBesoins() {
        Evaluation evaluation = evaluation(1L, 10L, Evaluation.Statut.SIGNEE);
        CampagneEvaluation campagne = new CampagneEvaluation();
        campagne.setDateCloture(LocalDate.of(2026, 12, 31));
        when(evaluations.findById(1L)).thenReturn(Optional.of(evaluation));
        when(campagnes.campagne(100L)).thenReturn(campagne);
        when(employes.posteDe(10L)).thenReturn(Optional.of(20L));
        when(referentiel.posteCompetences(20L)).thenReturn(List.of(
                new EvaluationDtos.PosteCompetenceDto(1L, 20L, 30L, 5, 2),
                new EvaluationDtos.PosteCompetenceDto(2L, 20L, 31L, 4, 2)));
        when(referentiel.niveauxAcquis(10L)).thenReturn(Map.of(30L, 2, 31L, 4));
        when(ecarts.save(any(EcartCompetence.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(besoins.save(any(BesoinFormation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EvaluationDtos.EvaluationDto resultat = service.clore(1L);

        assertEquals("CLOTUREE", resultat.statut());
        ArgumentCaptor<EcartCompetence> ecartCaptor = ArgumentCaptor.forClass(EcartCompetence.class);
        verify(ecarts).save(ecartCaptor.capture());
        EcartCompetence ecart = ecartCaptor.getValue();
        assertEquals(3, ecart.getEcart());
        assertEquals(new BigDecimal("6"), ecart.getPrioriteScore());
        assertEquals("HAUTE", ecart.getPriorite());

        ArgumentCaptor<BesoinFormation> besoinCaptor = ArgumentCaptor.forClass(BesoinFormation.class);
        verify(besoins).save(besoinCaptor.capture());
        BesoinFormation besoin = besoinCaptor.getValue();
        assertEquals(BesoinFormation.Origine.ECART.name(), besoin.getOrigine());
        assertEquals("HAUTE", besoin.getPriorite());
        assertEquals(LocalDate.of(2026, 12, 31), besoin.getDateCible());
    }

    @Test
    void evaluationManagerCalculeLesNotesPonderees() {
        Evaluation evaluation = evaluation(1L, 10L, Evaluation.Statut.AUTO_EVALUATION);
        evaluation.setIdEvaluateur(99L);
        CampagneEvaluation campagne = new CampagneEvaluation();
        campagne.setGrilleJson("{\"ponctualite\":1,\"qualite\":3,\"collaboration\":3}");
        Objectif objectif = new Objectif();
        objectif.setIdEmploye(10L);
        objectif.setIdCampagne(100L);
        objectif.setValeurCible(BigDecimal.valueOf(100));
        objectif.setValeurRealisee(BigDecimal.valueOf(85));
        objectif.setSens("PLUS_EST_MIEUX");
        objectif.setPoids(BigDecimal.valueOf(100));

        when(evaluations.findById(1L)).thenReturn(Optional.of(evaluation));
        when(utilisateurCourant.exiger()).thenReturn(
                new UtilisateurConnecte(99L, "Manager", "manager@tovo.mg", Roles.MANAGER, 1L, "RH"));
        when(campagnes.campagne(100L)).thenReturn(campagne);
        when(campagnes.objectifsCourants(10L, 100L)).thenReturn(List.of(objectif));
        when(employes.posteDe(10L)).thenReturn(Optional.of(20L));
        when(referentiel.posteCompetences(20L)).thenReturn(
                List.of(
                        new EvaluationDtos.PosteCompetenceDto(1L, 20L, 30L, 2, 2),
                        new EvaluationDtos.PosteCompetenceDto(2L, 20L, 31L, 3, 3),
                        new EvaluationDtos.PosteCompetenceDto(3L, 20L, 32L, 3, 3),
                        new EvaluationDtos.PosteCompetenceDto(4L, 20L, 33L, 2, 2),
                        new EvaluationDtos.PosteCompetenceDto(5L, 20L, 34L, 3, 3),
                        new EvaluationDtos.PosteCompetenceDto(6L, 20L, 35L, 1, 1),
                        new EvaluationDtos.PosteCompetenceDto(7L, 20L, 36L, 3, 1),
                        new EvaluationDtos.PosteCompetenceDto(8L, 20L, 37L, 4, 3),
                        new EvaluationDtos.PosteCompetenceDto(9L, 20L, 38L, 3, 1)));
        when(referentiel.niveauxAcquis(10L)).thenReturn(Map.of(
                30L, 4, 31L, 3, 32L, 2, 33L, 2, 34L, 1,
                35L, 3, 36L, 1, 37L, 5, 38L, 4));
        when(evaluations.save(any(Evaluation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EvaluationDtos.EvaluationDto resultat = service.evaluerManager(1L,
                new EvaluationDtos.EvaluationManagerRequest(Map.of(
                        "ponctualite", 2, "qualite", 4, "collaboration", 5),
                        "Fiable", "Aucun", "Conforme"));

        assertEquals(new BigDecimal("4.25"), resultat.noteObjectifs());
        assertEquals(new BigDecimal("4.04"), resultat.noteCompetences());
        assertEquals(new BigDecimal("4.14"), resultat.noteComportement());
        assertEquals(new BigDecimal("4.17"), resultat.noteGlobale());
        assertEquals("Supérieur", resultat.appreciation());
        verify(objectifs, times(1)).saveAll(any());
    }

    private Evaluation evaluation(Long id, Long idEmploye, Evaluation.Statut statut) {
        Evaluation evaluation = new Evaluation();
        evaluation.setId(id);
        evaluation.setIdEmploye(idEmploye);
        evaluation.setIdCampagne(100L);
        evaluation.setStatut(statut.name());
        return evaluation;
    }
}
