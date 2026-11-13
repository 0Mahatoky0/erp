package com.tovo.erp.recrutement.dto;

import com.tovo.erp.recrutement.model.Candidat;
import com.tovo.erp.recrutement.model.Candidature;
import com.tovo.erp.recrutement.model.EvaluationCandidat;
import com.tovo.erp.recrutement.model.TypeEvaluation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SyntheseEvaluationDtoTest {

    private Candidature candidature() {
        Candidat candidat = new Candidat();
        candidat.setIdCandidat(2L);
        candidat.setNom("Rakoto");
        candidat.setPrenom("Mamy");
        candidat.setEmail("mamy@example.com");
        Candidature candidature = new Candidature();
        candidature.setIdCandidature(10L);
        candidature.setCandidat(candidat);
        return candidature;
    }

    private EvaluationCandidat evaluation(TypeEvaluation type, String note, String noteMax) {
        EvaluationCandidat evaluation = new EvaluationCandidat();
        evaluation.setTypeEvaluation(type);
        evaluation.setNote(note == null ? null : new BigDecimal(note));
        evaluation.setNoteMax(new BigDecimal(noteMax));
        return evaluation;
    }

    @Test
    void signaleLesCategoriesSansEvaluation() {
        SyntheseEvaluationDto synthese = SyntheseEvaluationDto.de(candidature(), List.of());

        assertFalse(synthese.dossier().evaluee());
        assertFalse(synthese.entretien().evaluee());
        assertFalse(synthese.test().evaluee());
        assertNull(synthese.noteGlobale());
        assertEquals(List.of(TypeEvaluation.DOSSIER, TypeEvaluation.ENTRETIEN, TypeEvaluation.TEST),
                synthese.categoriesManquantes());
    }

    @Test
    void ramenePlusieursBaremesSurVingtEtMoyenne() {
        SyntheseEvaluationDto synthese = SyntheseEvaluationDto.de(candidature(), List.of(
                evaluation(TypeEvaluation.DOSSIER, "15", "20"),
                evaluation(TypeEvaluation.ENTRETIEN, "8", "10"),
                evaluation(TypeEvaluation.ENTRETIEN, "12", "20")));

        assertEquals(new BigDecimal("15.00"), synthese.dossier().note());
        // (8/10 -> 16) et (12/20 -> 12) : moyenne 14.
        assertEquals(new BigDecimal("14.00"), synthese.entretien().note());
        assertEquals(2, synthese.entretien().nombreEvaluations());
        assertFalse(synthese.test().evaluee());
        assertEquals(List.of(TypeEvaluation.TEST), synthese.categoriesManquantes());
        // La moyenne generale ne compte que les categories notees : (15 + 14) / 2.
        assertEquals(new BigDecimal("14.50"), synthese.noteGlobale());
    }

    @Test
    void uneEvaluationSansNoteCompteCommeFaiteMaisSansNote() {
        SyntheseEvaluationDto synthese = SyntheseEvaluationDto.de(candidature(), List.of(
                evaluation(TypeEvaluation.TEST, null, "20")));

        assertTrue(synthese.test().evaluee());
        assertNull(synthese.test().note());
        assertNull(synthese.noteGlobale());
        assertFalse(synthese.categoriesManquantes().contains(TypeEvaluation.TEST));
    }
}
