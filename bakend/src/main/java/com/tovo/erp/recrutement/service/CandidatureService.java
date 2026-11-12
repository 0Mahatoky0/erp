package com.tovo.erp.recrutement.service;

import com.tovo.erp.recrutement.dto.CandidatureDto;
import com.tovo.erp.recrutement.dto.CandidatureEmbaucheDto;
import com.tovo.erp.recrutement.dto.HistoriqueCandidatureDto;
import com.tovo.erp.recrutement.model.*;
import com.tovo.erp.recrutement.repository.*;
import com.tovo.erp.shared.Roles;
import com.tovo.erp.shared.UtilisateurConnecte;
import com.tovo.erp.shared.BusinessException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CandidatureService {
    private static final Map<CandidatureStatut, Set<CandidatureStatut>> TRANSITIONS_AUTORISEES =
            Map.ofEntries(
                    Map.entry(CandidatureStatut.RECUE, Set.of(
                            CandidatureStatut.EN_ETUDE, CandidatureStatut.RETIREE)),
                    Map.entry(CandidatureStatut.EN_ETUDE, Set.of(
                            CandidatureStatut.PRESELECTIONNEE,
                            CandidatureStatut.REFUSEE,
                            CandidatureStatut.RETIREE)),
                    Map.entry(CandidatureStatut.PRESELECTIONNEE, Set.of(
                            CandidatureStatut.EN_EVALUATION,
                            CandidatureStatut.REFUSEE,
                            CandidatureStatut.RETIREE)),
                    Map.entry(CandidatureStatut.EN_EVALUATION, Set.of(
                            CandidatureStatut.RETENUE,
                            CandidatureStatut.LISTE_ATTENTE,
                            CandidatureStatut.REFUSEE,
                            CandidatureStatut.RETIREE)),
                    Map.entry(CandidatureStatut.LISTE_ATTENTE, Set.of(
                            CandidatureStatut.RETENUE, CandidatureStatut.REFUSEE)),
                    Map.entry(CandidatureStatut.RETENUE, Set.of(
                            CandidatureStatut.ACCEPTEE,
                            CandidatureStatut.EMBAUCHEE,
                            CandidatureStatut.REFUSEE_PAR_CANDIDAT,
                            CandidatureStatut.EXPIREE,
                            CandidatureStatut.RETIREE)),
                    Map.entry(CandidatureStatut.ACCEPTEE, Set.of(
                            CandidatureStatut.EMBAUCHEE,
                            CandidatureStatut.REFUSEE_PAR_CANDIDAT,
                            CandidatureStatut.EXPIREE)),
                    Map.entry(CandidatureStatut.EMBAUCHEE, Set.of()),
                    Map.entry(CandidatureStatut.REFUSEE, Set.of()),
                    Map.entry(CandidatureStatut.REFUSEE_PAR_CANDIDAT, Set.of()),
                    Map.entry(CandidatureStatut.EXPIREE, Set.of()),
                    Map.entry(CandidatureStatut.RETIREE, Set.of())
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
    public Candidature postuler(Long idOffre, UtilisateurConnecte utilisateur, String message, String source) {
        if (!Roles.CANDIDAT.equals(utilisateur.getRoleNom())) {
            throw new IllegalStateException("Seul un compte candidat peut postuler");
        }
        OffreEmploi offre = offres.findById(idOffre)
                .orElseThrow(() -> new IllegalArgumentException("Offre introuvable"));
        if (offre.getStatut() != OffreStatut.PUBLIEE)
            throw new IllegalStateException("L'offre n'est pas ouverte aux candidatures");
        Candidat existant = candidats.findByIdUtilisateur(utilisateur.getId())
                .orElseThrow(() -> new IllegalStateException("Le profil candidat est introuvable"));
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

    /** Variante HTTP : la candidature creee est exposee sous forme de DTO. */
    @Transactional
    public CandidatureDto postulerDto(Long idOffre, UtilisateurConnecte utilisateur,
                                      String message, String source) {
        return CandidatureDto.de(postuler(idOffre, utilisateur, message, source));
    }

    /** Compatibilite des tests legacy ; les endpoints utilisent le compte authentifie. */
    @Deprecated
    @Transactional
    public Candidature postuler(Long idOffre, Candidat candidat, String message, String source) {
        OffreEmploi offre = offres.findById(idOffre)
                .orElseThrow(() -> new IllegalArgumentException("Offre introuvable"));
        if (offre.getStatut() != OffreStatut.PUBLIEE)
            throw new IllegalStateException("L'offre n'est pas ouverte aux candidatures");
        Candidat existant = candidats.findByEmailIgnoreCase(candidat.getEmail())
                .orElseGet(() -> candidats.save(candidat));
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
    public CandidatureDto changerStatut(Long id, CandidatureStatut statut, Long idUtilisateur, String commentaire) {
        Candidature candidature = charger(id);
        CandidatureStatut ancien = candidature.getStatut();
        if (ancien == statut) throw new IllegalStateException("Le statut est déjà " + statut);
        verifierTransition(ancien, statut);
        candidature.setStatut(statut);
        Candidature saved = candidatures.save(candidature);
        historiser(saved, ancien, statut, AuteurType.UTILISATEUR, idUtilisateur, commentaire);
        return CandidatureDto.de(saved);
    }

    /** Candidatures filtrees par statut texte, pour les clients HTTP. */
    @Transactional
    public List<CandidatureDto> listerParStatut(String statut) {
        if (statut == null || statut.isBlank()) {
            return listerParStatut((CandidatureStatut) null);
        }
        try {
            return listerParStatut(CandidatureStatut.valueOf(statut.trim().toUpperCase(java.util.Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("STATUT_INVALIDE", "Statut de candidature inconnu : " + statut);
        }
    }

    /** Candidatures filtrees par statut, pour la liste de selection d'embauche. */
    @Transactional
    public List<CandidatureDto> listerParStatut(CandidatureStatut statut) {
        List<Candidature> resultat = statut == null
                ? candidatures.findAllByOrderByDateCandidatureDesc()
                : candidatures.findAllByStatutOrderByDateCandidatureDesc(statut);
        return resultat.stream().map(CandidatureDto::de).toList();
    }

    /** Detail d'une candidature avec ses relations, pour les fiches de suivi. */
    @Transactional
    public CandidatureDto detail(Long id) {
        return CandidatureDto.de(charger(id));
    }

    /** Vue partagee avec le module embauche pour une candidature. */
    @Transactional
    public CandidatureDto retirerDto(Long id, Long idUtilisateur) {
        return changerStatut(id, CandidatureStatut.RETIREE, idUtilisateur, "Retrait de candidature");
    }

    @Transactional
    public CandidatureEmbaucheDto detailsPourEmbauche(Long id) {
        return CandidatureEmbaucheDto.de(charger(id));
    }

    /** Charge en une requete les candidatures referencees par des propositions. */
    @Transactional
    public Map<Long, CandidatureEmbaucheDto> resumesParIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return candidatures.findAllByIdCandidatureIn(ids).stream()
                .map(CandidatureEmbaucheDto::de)
                .collect(Collectors.toMap(CandidatureEmbaucheDto::idCandidature, Function.identity()));
    }

    /** Le candidat a accepte : la candidature devient EMBAUCHEE (section 5.4, etape 8). */
    @Transactional
    public CandidatureDto marquerEmbaucheePar(Long id, String auteur, Long idUtilisateur, String commentaire) {
        AuteurType type;
        try {
            type = AuteurType.valueOf(auteur.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new BusinessException("AUTEUR_INVALIDE", "Auteur inconnu : " + auteur);
        }
        return CandidatureDto.de(marquerEmbauchee(id, type, idUtilisateur, commentaire));
    }

    @Transactional
    public Candidature marquerEmbauchee(Long id, Long idUtilisateur, String commentaire) {
        return marquerEmbauchee(id, AuteurType.UTILISATEUR, idUtilisateur, commentaire);
    }

    @Transactional
    public Candidature marquerEmbauchee(Long id, AuteurType auteurType, Long idUtilisateur,
                                        String commentaire) {
        Candidature candidature = charger(id);
        if (candidature.getStatut() == CandidatureStatut.EMBAUCHEE) {
            return candidature;
        }
        CandidatureStatut ancien = candidature.getStatut();
        if (ancien != CandidatureStatut.RETENUE && ancien != CandidatureStatut.ACCEPTEE) {
            throw new BusinessException("CANDIDATURE_NON_RETENUE",
                    "Seule une candidature retenue peut etre embauchee (statut actuel : " + ancien + ").");
        }
        candidature.setStatut(CandidatureStatut.EMBAUCHEE);
        Candidature saved = candidatures.save(candidature);
        historiser(saved, ancien, CandidatureStatut.EMBAUCHEE, auteurType,
                idUtilisateur, commentaire);
        Candidat candidat = saved.getCandidat();
        candidat.setStatut(StatutCandidat.EMPLOYE);
        candidats.save(candidat);
        cloreOffreSiComplete(saved.getOffre());
        return saved;
    }

    /** Le candidat a refuse la proposition : la candidature est close. */
    @Transactional
    public Candidature marquerRefuseeParCandidat(Long id, Long idUtilisateur, String motif) {
        Candidature candidature = charger(id);
        if (candidature.getStatut() == CandidatureStatut.REFUSEE_PAR_CANDIDAT) {
            return candidature;
        }
        CandidatureStatut ancien = candidature.getStatut();
        if (ancien != CandidatureStatut.RETENUE && ancien != CandidatureStatut.ACCEPTEE) {
            throw new BusinessException("CANDIDATURE_NON_RETENUE",
                    "La candidature n'est pas dans un etat permettant un refus (statut actuel : " + ancien + ").");
        }
        candidature.setStatut(CandidatureStatut.REFUSEE_PAR_CANDIDAT);
        Candidature saved = candidatures.save(candidature);
        historiser(saved, ancien, CandidatureStatut.REFUSEE_PAR_CANDIDAT, AuteurType.CANDIDAT,
                idUtilisateur, motif);
        return saved;
    }

    /** La proposition a expire sans reponse. */
    @Transactional
    public Candidature marquerExpiree(Long id) {
        Candidature candidature = charger(id);
        if (candidature.getStatut() == CandidatureStatut.EXPIREE) {
            return candidature;
        }
        CandidatureStatut ancien = candidature.getStatut();
        if (ancien != CandidatureStatut.RETENUE && ancien != CandidatureStatut.ACCEPTEE) {
            return candidature;
        }
        candidature.setStatut(CandidatureStatut.EXPIREE);
        Candidature saved = candidatures.save(candidature);
        historiser(saved, ancien, CandidatureStatut.EXPIREE, AuteurType.SYSTEME, null,
                "Proposition d'embauche expiree.");
        return saved;
    }

    @Transactional
    public List<CandidatureDto> parOffreDto(Long idOffre, String statut) {
        CandidatureStatut filtre = null;
        if (statut != null && !statut.isBlank()) {
            try {
                filtre = CandidatureStatut.valueOf(statut.trim().toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new BusinessException("STATUT_INVALIDE", "Statut de candidature inconnu : " + statut);
            }
        }
        return parOffre(idOffre, filtre).stream().map(CandidatureDto::de).toList();
    }

    public List<Candidature> parOffre(Long idOffre, CandidatureStatut statut) {
        return statut == null ? candidatures.findByOffreIdOffre(idOffre)
                : candidatures.findByOffreIdOffreAndStatut(idOffre, statut);
    }

    public List<Candidature> parCandidat(Long idCandidat) { return candidatures.findByCandidatIdCandidat(idCandidat); }

    @Transactional
    public List<CandidatureDto> parCandidatDto(Long idCandidat) {
        return parCandidat(idCandidat).stream().map(CandidatureDto::de).toList();
    }

    @Transactional
    public List<HistoriqueCandidatureDto> historique(Long idCandidature) {
        return historiques.findByCandidatureIdCandidatureOrderByDateActionAsc(idCandidature).stream()
                .map(HistoriqueCandidatureDto::de)
                .toList();
    }

    @Transactional
    public void enregistrerProfilCandidat(Long idUtilisateur, String nom, String prenom,
                                          String email, String telephone, String adresse) {
        Candidat candidat = candidats.findByIdUtilisateur(idUtilisateur).orElseGet(Candidat::new);
        candidat.setIdUtilisateur(idUtilisateur);
        candidat.setNom(nom == null ? null : nom.trim());
        candidat.setPrenom(prenom == null ? null : prenom.trim());
        candidat.setEmail(email == null ? null : email.trim());
        candidat.setTelephone(telephone);
        candidat.setAdresse(adresse);
        candidats.save(candidat);
    }

    @Transactional
    public List<CandidatureDto> listerParEmailPublic(String email) {
        if (email == null || email.isBlank()) {
            return List.of();
        }
        Candidat candidat = candidats.findByEmailIgnoreCase(email.trim()).orElse(null);
        if (candidat == null) {
            return List.of();
        }
        return candidatures.findByCandidatIdCandidatOrderByDateCandidatureDesc(candidat.getIdCandidat())
                .stream()
                .map(CandidatureDto::de)
                .toList();
    }

    /** Identifiant du candidat rattache au compte, sans exposer l'entite. */
    @Transactional
    public Long idCandidatParUtilisateur(Long idUtilisateur) {
        if (idUtilisateur == null) {
            return null;
        }
        return candidats.findByIdUtilisateur(idUtilisateur)
                .map(Candidat::getIdCandidat)
                .orElse(null);
    }

    @Transactional
    public List<CandidatureDto> listerParUtilisateurConnecte(Long idUtilisateur) {
        return listerParUtilisateurConnecte(idUtilisateur, null);
    }

    @Transactional
    public List<CandidatureDto> listerParUtilisateurConnecte(Long idUtilisateur, String email) {
        if (idUtilisateur == null && (email == null || email.isBlank())) {
            return List.of();
        }

        Candidat candidat = null;
        if (idUtilisateur != null) {
            candidat = candidats.findByIdUtilisateur(idUtilisateur).orElse(null);
        }
        if (candidat == null && email != null && !email.isBlank()) {
            candidat = candidats.findByEmailIgnoreCase(email.trim()).orElse(null);
        }
        if (candidat == null) {
            return List.of();
        }
        return candidatures.findByCandidatIdCandidatOrderByDateCandidatureDesc(candidat.getIdCandidat())
                .stream()
                .map(CandidatureDto::de)
                .toList();
    }

    private Candidature charger(Long id) {
        return candidatures.findById(id).orElseThrow(() -> new IllegalArgumentException("Candidature introuvable"));
    }

    private void verifierTransition(CandidatureStatut ancien, CandidatureStatut nouveau) {
        if (nouveau == null || !TRANSITIONS_AUTORISEES.getOrDefault(ancien, Set.of()).contains(nouveau)) {
            throw new IllegalStateException(
                    "Transition de candidature interdite : " + ancien + " -> " + nouveau);
        }
    }

    private void cloreOffreSiComplete(OffreEmploi offre) {
        if (offre.getStatut() != OffreStatut.PUBLIEE) {
            return;
        }
        long embauches = candidatures.countByOffreIdOffreAndStatut(
                offre.getIdOffre(), CandidatureStatut.EMBAUCHEE);
        if (offre.getEffectifRecherche() != null && embauches >= offre.getEffectifRecherche()) {
            offre.setStatut(OffreStatut.CLOTUREE);
            offre.setDateCloture(LocalDateTime.now());
            offres.save(offre);
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
