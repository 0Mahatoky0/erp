package com.tovo.erp.recrutement.service;

import com.tovo.erp.recrutement.dto.SyntheseEvaluationDto;
import com.tovo.erp.recrutement.model.Candidature;
import com.tovo.erp.recrutement.model.CandidatureStatut;
import com.tovo.erp.recrutement.model.EvaluationCandidat;
import com.tovo.erp.recrutement.repository.CandidatureRepository;
import com.tovo.erp.recrutement.repository.EvaluationCandidatRepository;
import com.tovo.erp.shared.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** Calcule les notes par categorie d'evaluation (dossier, entretien, test) des candidatures. */
@Service
public class SyntheseEvaluationService {

    private final EvaluationCandidatRepository evaluations;
    private final CandidatureRepository candidatures;

    public SyntheseEvaluationService(EvaluationCandidatRepository evaluations,
                                     CandidatureRepository candidatures) {
        this.evaluations = evaluations;
        this.candidatures = candidatures;
    }

    /** Synthese de toutes les candidatures d'une offre, filtrees par statut si demande. */
    @Transactional(readOnly = true)
    public List<SyntheseEvaluationDto> parOffre(Long idOffre, String statut) {
        CandidatureStatut filtre = statutDepuis(statut);
        List<Candidature> liste = filtre == null
                ? candidatures.findByOffreIdOffre(idOffre)
                : candidatures.findByOffreIdOffreAndStatut(idOffre, filtre);

        Map<Long, List<EvaluationCandidat>> evaluationsParCandidature = evaluations
                .findByCandidatureOffreIdOffre(idOffre).stream()
                .collect(Collectors.groupingBy(e -> e.getCandidature().getIdCandidature()));

        return liste.stream()
                .map(candidature -> SyntheseEvaluationDto.de(candidature,
                        evaluationsParCandidature.getOrDefault(candidature.getIdCandidature(), List.of())))
                .toList();
    }

    /** Synthese d'une seule candidature, pour sa fiche d'evaluation. */
    @Transactional(readOnly = true)
    public SyntheseEvaluationDto pourCandidature(Long idCandidature) {
        Candidature candidature = candidatures.findById(idCandidature)
                .orElseThrow(() -> BusinessException.notFound(
                        "La candidature " + idCandidature + " n'existe pas."));
        return SyntheseEvaluationDto.de(candidature,
                evaluations.findByCandidatureIdCandidature(idCandidature));
    }

    private CandidatureStatut statutDepuis(String statut) {
        if (statut == null || statut.isBlank()) {
            return null;
        }
        try {
            return CandidatureStatut.valueOf(statut.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("STATUT_INVALIDE", "Statut de candidature inconnu : " + statut);
        }
    }
}
