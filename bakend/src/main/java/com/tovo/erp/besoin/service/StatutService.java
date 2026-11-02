package com.tovo.erp.besoin.service;

import com.tovo.erp.besoin.model.DemandeRecrutement;
import com.tovo.erp.besoin.model.DemandeRecrutementPoste;
import com.tovo.erp.besoin.model.HistoriqueStatut;
import com.tovo.erp.iam.model.Utilisateur;
import com.tovo.erp.besoin.model.StatutDemande;
import com.tovo.erp.besoin.model.StatutLigne;
import com.tovo.erp.besoin.repository.HistoriqueStatutRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Calcul du statut global d'une demande (RM-041 a RM-047).
 *
 * <p>Le statut global n'est jamais saisi par un utilisateur : il est recalcule par le
 * backend a chaque transition de statut d'une ligne.
 */
@Service
public class StatutService {

    private final HistoriqueStatutRepository historiqueStatutRepository;

    public StatutService(HistoriqueStatutRepository historiqueStatutRepository) {
        this.historiqueStatutRepository = historiqueStatutRepository;
    }

    /**
     * Applique la regle de calcul :
     * <pre>
     * si la demande n'est pas soumise          -> BROUILLON
     * sinon si une ligne est A_CORRIGER         -> A_CORRIGER
     * sinon si une ligne attend une validation  -> EN_VALIDATION
     * sinon (toutes les lignes sont terminees) :
     *      toutes annulees                      -> ANNULEE
     *      aucune acceptee                       -> REFUSEE
     *      toutes acceptees                      -> FINALISEE
     *      sinon                                 -> FINALISEE_PARTIELLE
     * </pre>
     */
    public StatutDemande calculerStatutGlobal(DemandeRecrutement demande, List<DemandeRecrutementPoste> lignes) {
        if (demande.getDateSoumission() == null || lignes.isEmpty()) {
            return StatutDemande.BROUILLON;
        }
        if (lignes.stream().anyMatch(ligne -> ligne.getStatut() == StatutLigne.A_CORRIGER)) {
            return StatutDemande.A_CORRIGER;
        }
        boolean enAttente = lignes.stream().anyMatch(ligne ->
                ligne.getStatut() == StatutLigne.BROUILLON || ligne.getStatut().estEnValidation());
        if (enAttente) {
            return StatutDemande.EN_VALIDATION;
        }
        boolean toutesAnnulees = lignes.stream().allMatch(ligne -> ligne.getStatut() == StatutLigne.ANNULEE);
        if (toutesAnnulees) {
            return StatutDemande.ANNULEE;
        }
        boolean aucuneAcceptee = lignes.stream().noneMatch(ligne -> ligne.getStatut() == StatutLigne.ACCEPTEE);
        if (aucuneAcceptee) {
            return StatutDemande.REFUSEE;
        }
        boolean toutesAcceptees = lignes.stream().allMatch(ligne -> ligne.getStatut() == StatutLigne.ACCEPTEE);
        if (toutesAcceptees) {
            return StatutDemande.FINALISEE;
        }
        return StatutDemande.FINALISEE_PARTIELLE;
    }

    /**
     * Recalcule le statut global de la demande et journalise le changement (RM-047, RM-065).
     *
     * @return le nouveau statut global de la demande
     */
    @Transactional
    public StatutDemande recalculer(DemandeRecrutement demande, List<DemandeRecrutementPoste> lignes,
                                    Utilisateur auteur, String commentaire) {
        StatutDemande nouveauStatut = calculerStatutGlobal(demande, lignes);
        StatutDemande ancienStatut = demande.getStatut();
        if (ancienStatut == nouveauStatut) {
            return nouveauStatut;
        }
        demande.setStatut(nouveauStatut);
        demande.setDateModification(java.time.LocalDateTime.now());

        HistoriqueStatut journal = new HistoriqueStatut();
        journal.setDemande(demande);
        journal.setAncienStatut(ancienStatut.name());
        journal.setNouveauStatut(nouveauStatut.name());
        journal.setUtilisateur(auteur);
        journal.setCommentaire(commentaire);
        historiqueStatutRepository.save(journal);
        return nouveauStatut;
    }
}
