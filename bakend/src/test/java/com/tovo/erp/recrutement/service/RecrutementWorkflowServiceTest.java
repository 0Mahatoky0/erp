package com.tovo.erp.recrutement.service;

import com.tovo.erp.recrutement.domain.*;
import com.tovo.erp.recrutement.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecrutementWorkflowServiceTest {
    @Mock DocumentCandidatureRepository documents;
    @Mock CandidatureRepository candidatures;
    @Mock EntretienRepository entretiens;
    @Mock TestRecrutementRepository tests;
    @Mock PassageTestRepository passages;
    @Mock OffreEmploiRepository offres;
    @Mock EvaluationCandidatRepository evaluations;
    @Mock DecisionCandidatureRepository decisions;
    @Mock CandidatureService candidatureService;

    @Test
    void valideLesDocumentsEtRefuseUnMimeDangereux() throws Exception {
        DocumentCandidatureService service = new DocumentCandidatureService(documents, candidatures);
        Candidature candidature = new Candidature();
        candidature.setIdCandidature(3L);
        when(candidatures.findById(3L)).thenReturn(Optional.of(candidature));
        when(documents.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        MockMultipartFile fichier = new MockMultipartFile("fichier", "cv.pdf",
                "application/pdf", "contenu".getBytes());

        DocumentCandidature saved = service.deposer(3L, TypeDocument.CV, fichier);

        assertEquals("application/pdf", saved.getTypeMime());
        assertTrue(saved.getCleStockage().startsWith("candidatures/3/"));

        MockMultipartFile executable = new MockMultipartFile("fichier", "script.exe",
                "application/x-msdownload", "x".getBytes());
        assertThrows(IllegalArgumentException.class, () -> service.deposer(3L, TypeDocument.CV, executable));
    }

    @Test
    void planifieUnEntretienAvecDesDatesCoherentes() {
        EntretienTestService service = new EntretienTestService(
                entretiens, candidatures, tests, passages, offres);
        Candidature candidature = new Candidature();
        candidature.setIdCandidature(3L);
        Entretien entretien = new Entretien();
        entretien.setCandidature(candidature);
        entretien.setDateDebut(LocalDateTime.now().plusDays(1));
        entretien.setDateFin(LocalDateTime.now().plusDays(1).plusHours(1));
        when(candidatures.existsById(3L)).thenReturn(true);
        when(entretiens.save(entretien)).thenReturn(entretien);

        Entretien saved = service.planifierEntretien(entretien);

        assertEquals(EntretienStatut.PLANIFIE, saved.getStatut());
    }

    @Test
    void refuseUnEntretienAvecUneFinInvalide() {
        EntretienTestService service = new EntretienTestService(
                entretiens, candidatures, tests, passages, offres);
        Entretien entretien = new Entretien();
        entretien.setDateDebut(LocalDateTime.now());
        entretien.setDateFin(LocalDateTime.now().minusMinutes(1));

        assertThrows(IllegalArgumentException.class, () -> service.planifierEntretien(entretien));
        verify(entretiens, never()).save(any());
    }

    @Test
    void valideLesNotesEtExigeUnMotifDeDecision() {
        EvaluationDecisionService service = new EvaluationDecisionService(
                evaluations, decisions, candidatures, candidatureService);
        Candidature candidature = new Candidature();
        candidature.setIdCandidature(3L);
        EvaluationCandidat evaluation = new EvaluationCandidat();
        evaluation.setCandidature(candidature);
        evaluation.setNote(BigDecimal.valueOf(21));
        evaluation.setNoteMax(BigDecimal.valueOf(20));
        when(candidatures.existsById(3L)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.evaluer(evaluation));

        DecisionCandidature decision = new DecisionCandidature();
        decision.setCandidature(candidature);
        decision.setDecision(DecisionType.RETENUE);
        assertThrows(IllegalArgumentException.class, () -> service.decider(decision));
        verify(decisions, never()).save(any());
    }
}
