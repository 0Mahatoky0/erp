package com.tovo.erp.employe.dto;

import com.tovo.erp.employe.model.DemandeModification;
import com.tovo.erp.employe.model.StatutDemandeModification;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

/** Demandes de correction du dossier salarie (bloc 5). */
public final class DemandeModificationDtos {

    private DemandeModificationDtos() {
    }

    public record CreationDemandeRequest(
            @NotBlank String champ,
            @NotBlank String nouvelleValeur,
            String motif) {
    }

    public record DecisionDemandeRequest(
            boolean approuver,
            String motifDecision) {
    }

    public record DemandeModificationDto(
            Long id,
            Long idEmploye,
            String champ,
            String ancienneValeur,
            String nouvelleValeur,
            String motif,
            StatutDemandeModification statut,
            Long idDemandeur,
            Long idValidateur,
            String motifDecision,
            LocalDateTime dateDecision,
            LocalDateTime dateCreation) {

        public static DemandeModificationDto de(DemandeModification demande) {
            return new DemandeModificationDto(demande.getId(), demande.getIdEmploye(),
                    demande.getChamp(), demande.getAncienneValeur(),
                    demande.getNouvelleValeur(), demande.getMotif(), demande.getStatut(),
                    demande.getIdDemandeur(), demande.getIdValidateur(),
                    demande.getMotifDecision(), demande.getDateDecision(),
                    demande.getDateCreation());
        }
    }
}
