package com.tovo.erp.recrutement.service;

import com.tovo.erp.recrutement.model.*;
import com.tovo.erp.recrutement.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CandidatureService {
    private static final Map<CandidatureStatut, Set<CandidatureStatut>> TRANSITIONS_AUTORISEES = Map.of(
            CandidatureStatut.RECUE, Set.of(CandidatureStatut.EN_ETUDE, CandidatureStatut.RETIREE),
            CandidatureStatut.EN_ETUDE, Set.of(
                    CandidatureStatut.PRESELECTIONNEE,
                    CandidatureStatut.REFUSEE,
                    CandidatureStatut.RETIREE),
            CandidatureStatut.PRESELECTIONNEE, Set.of(
                    CandidatureStatut.EN_EVALUATION,
                    CandidatureStatut.REFUSEE,
                    CandidatureStatut.RETIREE),
            CandidatureStatut.EN_EVALUATION, Set.of(
                    CandidatureStatut.RETENUE,
                    CandidatureStatut.LISTE_ATTENTE,
                    CandidatureStatut.REFUSEE,
                    CandidatureStatut.RETIREE),
            CandidatureStatut.LISTE_ATTENTE, Set.of(CandidatureStatut.RETENUE, CandidatureStatut.REFUSEE),
            CandidatureStatut.RETENUE, Set.of(),
            CandidatureStatut.REFUSEE, Set.of(),
            CandidatureStatut.RETIREE, Set.of()
    );

    private final CandidatRepository candidats;
    private final CandidatureRepository candidatures;
    private final OffreEmploiRepository offres;
    private final HistoriqueCandidatureRepository historiques;

    public CandidatureService(CandidatRepository candidats, CandidatureRepository candidatures,
                              OffreEmploiRepository offres, HistoriqueCandidatureRepository historiques) {
        this.candidats = candidats;
        this.candidatures = candidatures;
        this.offres = offres;
        this.historiques = historiques;
    }

    @Transactional
    public Candidature postuler(Long idOffre, Candidat candidat, String message, String source) {
        OffreEmploi offre = offres.findById(idOffre)
                .orElseThrow(() -> new IllegalArgumentException("Offre introuvable"));
        if (offre.getStatut() != OffreStatut.PUBLIEE)
            throw new IllegalStateException("L'offre n'est pas ouverte aux candidatures");
        Candidat existant = candidats.findByEmailIgnoreCase(candidat.getEmail()).orElseGet(() -> candidats.save(candidat));
        if (candidatures.existsByOffreIdOffreAndCandidatIdCandidat(idOffre, existant.getIdCandidat()))
            throw new IllegalStateException("Le candidat a déjà postulé à cette offre");
        Candidature candidature = new Candidature();
        candidature.setOffre(offre);
        candidature.setCandidat(existant);
        candidature.setMessageCandidat(message);
        candidature.setSource(source);
        Candidature saved = candidatures.save(candidature);
        historiser(saved, null, CandidatureStatut.RECUE, AuteurType.CANDIDAT, null);
        return saved;
    }

    @Transactional
    public Candidature changerStatut(Long id, CandidatureStatut statut, Long idUtilisateur, String commentaire) {
        Candidature candidature = charger(id);
        CandidatureStatut ancien = candidature.getStatut();
        if (ancien == statut) throw new IllegalStateException("Le statut est déjà " + statut);
        verifierTransition(ancien, statut);
        candidature.setStatut(statut);
        Candidature saved = candidatures.save(candidature);
        historiser(saved, ancien, statut, AuteurType.UTILISATEUR, idUtilisateur, commentaire);
        return saved;
    }

    public List<Candidature> parOffre(Long idOffre, CandidatureStatut statut) {
        return statut == null ? candidatures.findByOffreIdOffre(idOffre)
                : candidatures.findByOffreIdOffreAndStatut(idOffre, statut);
    }

    public List<Candidature> parCandidat(Long idCandidat) { return candidatures.findByCandidatIdCandidat(idCandidat); }

    private Candidature charger(Long id) {
        return candidatures.findById(id).orElseThrow(() -> new IllegalArgumentException("Candidature introuvable"));
    }

    private void verifierTransition(CandidatureStatut ancien, CandidatureStatut nouveau) {
        if (nouveau == null || !TRANSITIONS_AUTORISEES.getOrDefault(ancien, Set.of()).contains(nouveau)) {
            throw new IllegalStateException(
                    "Transition de candidature interdite : " + ancien + " -> " + nouveau);
        }
    }

    private void historiser(Candidature c, CandidatureStatut ancien, CandidatureStatut nouveau,
                            AuteurType auteur, Long idUtilisateur, String... commentaire) {
        HistoriqueCandidature h = new HistoriqueCandidature();
        h.setCandidature(c);
        h.setAuteurType(auteur);
        h.setIdUtilisateur(idUtilisateur);
        h.setAncienStatut(ancien);
        h.setNouveauStatut(nouveau);
        h.setCommentaire(commentaire.length == 0 ? null : commentaire[0]);
        historiques.save(h);
    }
}
