package com.tovo.erp.contrat.dto;

import java.math.BigDecimal;

import com.tovo.erp.contrat.model.EvenementCarriere;
import com.tovo.erp.contrat.model.SalaireHistorique;
import com.tovo.erp.contrat.model.TypeEvenementCarriere;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** DTO de l'historique de remuneration et de carriere (bloc 5). */
public final class CarriereDtos {

    private CarriereDtos() {
    }

    public record SalaireDto(
            Long id,
            Long idEmploye,
            Long idContrat,
            BigDecimal montantBrut,
            String devise,
            LocalDate dateEffet,
            LocalDate dateFin,
            String motif,
            Long idUtilisateur,
            LocalDateTime dateCreation) {

        public static SalaireDto de(SalaireHistorique salaire) {
            return new SalaireDto(
                    salaire.getId(),
                    salaire.getIdEmploye(),
                    salaire.getIdContrat(),
                    salaire.getMontantBrut(),
                    salaire.getDevise(),
                    salaire.getDateEffet(),
                    salaire.getDateFin(),
                    salaire.getMotif(),
                    salaire.getIdUtilisateur(),
                    salaire.getDateCreation());
        }
    }

    public record EvenementDto(
            Long id,
            Long idEmploye,
            TypeEvenementCarriere type,
            LocalDate dateEffet,
            String ancienneValeur,
            String nouvelleValeur,
            Long idDocument,
            String commentaire,
            Long idUtilisateur,
            LocalDateTime dateCreation) {

        public static EvenementDto de(EvenementCarriere evenement) {
            return new EvenementDto(
                    evenement.getId(),
                    evenement.getIdEmploye(),
                    evenement.getType(),
                    evenement.getDateEffet(),
                    evenement.getAncienneValeur(),
                    evenement.getNouvelleValeur(),
                    evenement.getIdDocument(),
                    evenement.getCommentaire(),
                    evenement.getIdUtilisateur(),
                    evenement.getDateCreation());
        }
    }

    public record SalaireRequest(
            @NotNull @DecimalMin("0.0") BigDecimal montantBrut,
            @NotBlank String devise,
            @NotNull LocalDate dateEffet,
            String motif,
            Long idContrat) {
    }

    public record EvenementRequest(
            @NotNull TypeEvenementCarriere type,
            @NotNull LocalDate dateEffet,
            String ancienneValeur,
            String nouvelleValeur,
            Long idDocument,
            String commentaire) {
    }
}
