package com.tovo.erp.recrutement.service;

import com.tovo.erp.recrutement.dto.RecrutementRequests;
import com.tovo.erp.recrutement.model.Candidature;
import com.tovo.erp.recrutement.model.DecisionType;
import com.tovo.erp.recrutement.repository.CandidatureRepository;
import com.tovo.erp.recrutement.repository.DecisionCandidatureRepository;
import com.tovo.erp.recrutement.repository.EntretienRepository;
import com.tovo.erp.recrutement.repository.EvaluationCandidatRepository;
import com.tovo.erp.recrutement.repository.PassageTestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvaluationDecisionServiceTest {

    @Mock private EvaluationCandidatRepository evaluations;
    @Mock private DecisionCandidatureRepository decisions;
    @Mock private CandidatureRepository candidatures;
    @Mock private EntretienRepository entretiens;
    @Mock private PassageTestRepository passages;
    @Mock private CandidatureService candidatureService;

    private EvaluationDecisionService service;

    @BeforeEach
    void setUp() {
        service = new EvaluationDecisionService(evaluations, decisions, candidatures,
                entretiens, passages, candidatureService);
    }

    @Test
    void attribueLeRangFifoSuivantDansLaListeDAttente() {
        Candidature candidature = new Candidature();
        candidature.setIdCandidature(4L);
        when(candidatures.getReferenceById(4L)).thenReturn(candidature);
        when(decisions.dernierRangListeAttente(DecisionType.LISTE_ATTENTE)).thenReturn(2);
        when(decisions.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var resultat = service.decider(new RecrutementRequests.DecisionRequest(
                4L, DecisionType.LISTE_ATTENTE, "Profil de reserve"), 7L);

        assertEquals(3, resultat.rangListeAttente());
    }

    @Test
    void nattribuePasDeRangPourUneDecisionFinale() {
        Candidature candidature = new Candidature();
        candidature.setIdCandidature(4L);
        when(candidatures.getReferenceById(4L)).thenReturn(candidature);
        when(decisions.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var resultat = service.decider(new RecrutementRequests.DecisionRequest(
                4L, DecisionType.RETENUE, "Profil retenu"), 7L);

        assertEquals(null, resultat.rangListeAttente());
    }
}
