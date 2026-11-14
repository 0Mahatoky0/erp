package com.tovo.erp.recrutement.dto;

import com.tovo.erp.recrutement.model.Candidature;
import com.tovo.erp.recrutement.model.CandidatureStatut;
import com.tovo.erp.recrutement.model.EvaluationCandidat;
import com.tovo.erp.recrutement.model.TypeEvaluation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Synthese des evaluations d'une candidature : une note par categorie (dossier, entretien, test)
 * et une moyenne generale.
 *
 * <p>Les baremes pouvant differer d'une evaluation a l'autre, chaque note est ramenee sur 20 avant
 * d'etre moyennee. La note d'une categorie est la moyenne de ses evaluations ; la note globale est
 * la moyenne des categories qui ont une note. Une categorie sans evaluation est signalee dans
 * {@code categoriesManquantes} et n'entre pas dans la moyenne generale.
 */
public record SyntheseEvaluationDto(
        Long idCandidature,
        CandidatDto candidat,
        CandidatureStatut statut,
        NoteCategorie dossier,
        NoteCategorie entretien,
        NoteCategorie test,
        BigDecimal noteGlobale,
        List<TypeEvaluation> categoriesManquantes) {

    /** Bareme commun auquel toutes les notes sont ramenees. */
    public static final BigDecimal BAREME = BigDecimal.valueOf(20);

    /**
     * Resultat d'une categorie d'evaluation.
     *
     * @param evaluee           au moins une evaluation de cette categorie existe
     * @param nombreEvaluations nombre d'evaluations enregistrees
     * @param note              moyenne sur 20, null si aucune evaluation ne porte de note
     * @param noteMax           bareme de la note (toujours 20)
     * @param poids             poids de la categorie dans la note globale (grille de l'offre)
     */
    public record NoteCategorie(boolean evaluee, int nombreEvaluations, BigDecimal note,
                                BigDecimal noteMax, BigDecimal poids) {
    }

    public static SyntheseEvaluationDto de(Candidature candidature, List<EvaluationCandidat> evaluations) {
        return de(candidature, evaluations, 1, 1, 1);
    }

    public static SyntheseEvaluationDto de(Candidature candidature, List<EvaluationCandidat> evaluations,
                                           Integer poidsDossier, Integer poidsEntretien, Integer poidsTest) {
        NoteCategorie dossier = categorie(evaluations, TypeEvaluation.DOSSIER, poids(poidsDossier));
        NoteCategorie entretien = categorie(evaluations, TypeEvaluation.ENTRETIEN, poids(poidsEntretien));
        NoteCategorie test = categorie(evaluations, TypeEvaluation.TEST, poids(poidsTest));

        List<TypeEvaluation> manquantes = new ArrayList<>();
        if (!dossier.evaluee()) {
            manquantes.add(TypeEvaluation.DOSSIER);
        }
        if (!entretien.evaluee()) {
            manquantes.add(TypeEvaluation.ENTRETIEN);
        }
        if (!test.evaluee()) {
            manquantes.add(TypeEvaluation.TEST);
        }

        BigDecimal noteGlobale = moyennePonderee(Stream.of(dossier, entretien, test)
                .filter(categorie -> categorie.note() != null)
                .toList());

        return new SyntheseEvaluationDto(
                candidature.getIdCandidature(),
                CandidatDto.de(candidature.getCandidat()),
                candidature.getStatut(),
                dossier, entretien, test,
                noteGlobale,
                List.copyOf(manquantes));
    }

    private static NoteCategorie categorie(List<EvaluationCandidat> evaluations, TypeEvaluation type,
                                           BigDecimal poids) {
        List<EvaluationCandidat> deCeType = evaluations.stream()
                .filter(evaluation -> evaluation.getTypeEvaluation() == type)
                .toList();
        List<BigDecimal> notes = deCeType.stream()
                .map(SyntheseEvaluationDto::surVingt)
                .filter(Objects::nonNull)
                .toList();
        return new NoteCategorie(!deCeType.isEmpty(), deCeType.size(), moyenne(notes), BAREME, poids);
    }

    private static BigDecimal surVingt(EvaluationCandidat evaluation) {
        if (evaluation.getNoteGlobale() != null) {
            return evaluation.getNoteGlobale();
        }
        if (evaluation.getNote() == null || evaluation.getNoteMax() == null
                || evaluation.getNoteMax().signum() <= 0) {
            return null;
        }
        return evaluation.getNote().multiply(BAREME).divide(evaluation.getNoteMax(), 4, RoundingMode.HALF_UP);
    }

    private static BigDecimal poids(Integer poids) {
        return poids == null || poids <= 0 ? BigDecimal.ONE : BigDecimal.valueOf(poids);
    }

    private static BigDecimal moyennePonderee(List<NoteCategorie> categories) {
        BigDecimal poidsTotal = categories.stream()
                .map(NoteCategorie::poids)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (poidsTotal.signum() == 0) {
            return null;
        }
        BigDecimal somme = categories.stream()
                .map(categorie -> categorie.note().multiply(categorie.poids()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return somme.divide(poidsTotal, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal moyenne(List<BigDecimal> notes) {
        if (notes.isEmpty()) {
            return null;
        }
        BigDecimal somme = notes.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return somme.divide(BigDecimal.valueOf(notes.size()), 2, RoundingMode.HALF_UP);
    }
}
