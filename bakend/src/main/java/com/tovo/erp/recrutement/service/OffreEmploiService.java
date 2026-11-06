package com.tovo.erp.recrutement.service;

import com.tovo.erp.recrutement.model.*;
import com.tovo.erp.recrutement.repository.*;
import com.tovo.erp.recrutement.dto.OffreIndicateursDto;
import jakarta.transaction.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class OffreEmploiService {
    private final OffreEmploiRepository offres;
    private final HistoriqueOffreRepository historiques;
    private final CandidatureRepository candidatures;
    private final JdbcTemplate jdbc;

    public OffreEmploiService(OffreEmploiRepository offres,
                               HistoriqueOffreRepository historiques,
                               CandidatureRepository candidatures,
                               JdbcTemplate jdbc) {
        this.offres = offres;
        this.historiques = historiques;
        this.candidatures = candidatures;
        this.jdbc = jdbc;
    }

    @Transactional
    public OffreEmploi creer(OffreEmploi offre) {
        verifierDemandeAcceptee(offre.getIdDemandePoste());
        offre.setStatut(OffreStatut.BROUILLON);
        OffreEmploi sauvegardee = offres.save(offre);
        enregistrerHistorique(sauvegardee, null, OffreStatut.BROUILLON, offre.getIdUtilisateurCreateur());
        return sauvegardee;
    }

    @Transactional
    public OffreEmploi modifier(Long idOffre, OffreEmploi modifications) {
        OffreEmploi offre = charger(idOffre);
        if (offre.getStatut() != OffreStatut.BROUILLON) {
            throw new IllegalStateException("Seule une offre BROUILLON peut être modifiée");
        }
        verifierDemandeAcceptee(modifications.getIdDemandePoste());
        offre.setIdDemandePoste(modifications.getIdDemandePoste());
        offre.setTitre(modifications.getTitre());
        offre.setDescription(modifications.getDescription());
        offre.setProfilRecherche(modifications.getProfilRecherche());
        offre.setLieuTravail(modifications.getLieuTravail());
        offre.setTypeContrat(modifications.getTypeContrat());
        offre.setEffectifRecherche(modifications.getEffectifRecherche());
        offre.setSalaireMin(modifications.getSalaireMin());
        offre.setSalaireMax(modifications.getSalaireMax());
        offre.setModeSelection(modifications.getModeSelection());
        offre.setDateLimiteCandidature(modifications.getDateLimiteCandidature());
        return offres.save(offre);
    }

    @Transactional
    public void supprimer(Long idOffre) {
        OffreEmploi offre = charger(idOffre);
        if (offre.getStatut() != OffreStatut.BROUILLON) {
            throw new IllegalStateException("Seule une offre BROUILLON peut être supprimée");
        }
        if (candidatures.existsByOffreIdOffre(idOffre)) {
            throw new IllegalStateException("Une offre ayant des candidatures ne peut pas être supprimée");
        }
        offres.delete(offre);
    }

    @Transactional
    public OffreEmploi publier(Long idOffre, Long idUtilisateur) {
        OffreEmploi offre = charger(idOffre);
        verifierTransition(offre, OffreStatut.PUBLIEE);
        if (offre.getDatePublication() == null) offre.setDatePublication(LocalDateTime.now());
        OffreStatut ancien = offre.getStatut();
        offre.setStatut(OffreStatut.PUBLIEE);
        OffreEmploi sauvegardee = offres.save(offre);
        enregistrerHistorique(sauvegardee, ancien, OffreStatut.PUBLIEE, idUtilisateur);
        return sauvegardee;
    }

    @Transactional
    public OffreEmploi cloturer(Long idOffre, Long idUtilisateur) {
        OffreEmploi offre = charger(idOffre);
        verifierTransition(offre, OffreStatut.CLOTUREE);
        if (offre.getDateCloture() == null) offre.setDateCloture(LocalDateTime.now());
        OffreStatut ancien = offre.getStatut();
        offre.setStatut(OffreStatut.CLOTUREE);
        OffreEmploi sauvegardee = offres.save(offre);
        enregistrerHistorique(sauvegardee, ancien, OffreStatut.CLOTUREE, idUtilisateur);
        return sauvegardee;
    }

    @Transactional
    public OffreEmploi annuler(Long idOffre, Long idUtilisateur) {
        OffreEmploi offre = charger(idOffre);
        verifierTransition(offre, OffreStatut.ANNULEE);
        OffreStatut ancien = offre.getStatut();
        offre.setStatut(OffreStatut.ANNULEE);
        OffreEmploi sauvegardee = offres.save(offre);
        enregistrerHistorique(sauvegardee, ancien, OffreStatut.ANNULEE, idUtilisateur);
        return sauvegardee;
    }

    public OffreIndicateursDto indicateurs(Long idOffre) {
        if (!offres.existsById(idOffre)) {
            throw new IllegalArgumentException("Offre introuvable: " + idOffre);
        }
        long total = candidatures.countByOffreIdOffre(idOffre);
        long preselectionnees = compter(idOffre, CandidatureStatut.PRESELECTIONNEE);
        long retenues = compter(idOffre, CandidatureStatut.RETENUE);
        return new OffreIndicateursDto(
                idOffre,
                total,
                preselectionnees,
                taux(preselectionnees, total),
                compter(idOffre, CandidatureStatut.EN_EVALUATION),
                retenues,
                taux(retenues, total),
                compter(idOffre, CandidatureStatut.LISTE_ATTENTE),
                compter(idOffre, CandidatureStatut.REFUSEE));
    }

    private long compter(Long idOffre, CandidatureStatut statut) {
        return candidatures.countByOffreIdOffreAndStatut(idOffre, statut);
    }

    private BigDecimal taux(long valeur, long total) {
        if (total == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(valeur * 100.0 / total).setScale(2, RoundingMode.HALF_UP);
    }

    private OffreEmploi charger(Long idOffre) {
        return offres.findById(idOffre)
                .orElseThrow(() -> new IllegalArgumentException("Offre introuvable: " + idOffre));
    }

    private void verifierDemandeAcceptee(Long idDemandePoste) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM demande_recrutement_poste " +
                "WHERE id_demande_poste = ? AND statut = 'ACCEPTEE'",
                Integer.class, idDemandePoste);
        if (count == null || count != 1) {
            throw new IllegalStateException("La demande de recrutement doit être ACCEPTEE");
        }
    }

    private void verifierTransition(OffreEmploi offre, OffreStatut cible) {
        if (offre.getStatut() == OffreStatut.ANNULEE || offre.getStatut() == OffreStatut.CLOTUREE) {
            throw new IllegalStateException("Transition interdite depuis " + offre.getStatut());
        }
        if (cible == OffreStatut.CLOTUREE && offre.getStatut() != OffreStatut.PUBLIEE) {
            throw new IllegalStateException("Une offre doit être publiée avant sa clôture");
        }
    }

    private void enregistrerHistorique(OffreEmploi offre, OffreStatut ancien,
                                       OffreStatut nouveau, Long idUtilisateur) {
        HistoriqueOffre historique = new HistoriqueOffre();
        historique.setOffre(offre);
        historique.setIdUtilisateur(idUtilisateur);
        historique.setAncienStatut(ancien);
        historique.setNouveauStatut(nouveau);
        historiques.save(historique);
    }
}
