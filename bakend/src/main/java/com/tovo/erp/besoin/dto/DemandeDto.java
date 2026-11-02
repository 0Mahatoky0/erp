package com.tovo.erp.demande.dto;

import com.tovo.erp.demande.domain.DemandeRecrutement;
import com.tovo.erp.demande.domain.DemandeRecrutementPoste;
import com.tovo.erp.demande.domain.EtapeValidation;
import com.tovo.erp.demande.domain.StatutDemande;
import com.tovo.erp.demande.domain.StatutLigne;
import com.tovo.erp.demande.domain.TypeBesoin;
import com.tovo.erp.demande.domain.TypeRemplacement;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Objets de transfert des demandes de recrutement et de leurs lignes. */
public final class DemandeDto {

    private DemandeDto() {
    }

    /** Entete d'une demande : motif, date souhaitee et lignes (RM-012 a RM-020). */
    public record Creation(@NotBlank String motif, @NotNull LocalDate dateSouhaitee,
                            @NotNull List<LigneCreation> lignes) {
    }

    /** Modification de l'entete d'une demande encore a l'etat BROUILLON (RM-019). */
    public record Modification(@NotBlank String motif, @NotNull LocalDate dateSouhaitee) {
    }

    /** Contenu d'une ligne de demande (RM-015, RM-022 a RM-030). */
    public record LigneCreation(@NotNull Long idService,
                                 Long idPoste,
                                 @NotNull TypeBesoin typeBesoin,
                                 TypeRemplacement typeRemplacement,
                                 Long idEmployeRemplace,
                                 String motifRemplacement,
                                 LocalDate dateDebutRemplacement,
                                 LocalDate dateFinRemplacement,
                                 @NotBlank String intituleDemande,
                                 String descriptionPoste,
                                 String profilRecherche,
                                 @PositiveOrZero BigDecimal salaireMinEstime,
                                 @PositiveOrZero BigDecimal salaireMaxEstime,
                                 @NotNull @Positive Integer effectif,
                                 @NotNull @PositiveOrZero BigDecimal budgetEstime) {
    }

    /** Modification d'une ligne : uniquement possible tant que la demande est BROUILLON. */
    public record LigneModification(Long idPoste,
                                     String motifRemplacement,
                                     LocalDate dateDebutRemplacement,
                                     LocalDate dateFinRemplacement,
                                     @NotBlank String intituleDemande,
                                     String descriptionPoste,
                                     String profilRecherche,
                                     @PositiveOrZero BigDecimal salaireMinEstime,
                                     @PositiveOrZero BigDecimal salaireMaxEstime,
                                     @NotNull @Positive Integer effectif,
                                     @NotNull @PositiveOrZero BigDecimal budgetEstime) {
    }

    /** Resume d'une demande pour les listes. */
    public record Resume(Long id, String motif, LocalDate dateSouhaitee, StatutDemande statut,
                          LocalDateTime dateCreation, LocalDateTime dateSoumission,
                          String demandeur, Long idDemandeur, int nombreLignes,
                          int lignesAcceptees, int lignesEnValidation, int lignesACorriger,
                          BigDecimal budgetTotal) {
    }

    /** Ligne d'une demande, avec son circuit de validation. */
    public record LigneResume(Long id, Long idDemande, Long idService, String service,
                               Long idPoste, String poste,
                               TypeBesoin typeBesoin, TypeRemplacement typeRemplacement,
                               StatutLigne statut, EtapeValidation etapeCourante,
                               String intituleDemande, Integer effectif, BigDecimal budgetEstime,
                               int episode, LocalDateTime dateModification) {
    }

    /** Detail d'une ligne : contenu complet, historique des decisions et verifications. */
    public record LigneDetail(Long id, Long idDemande, Long idService, String service, String departement,
                               Long idPoste, String poste,
                               TypeBesoin typeBesoin, TypeRemplacement typeRemplacement,
                               Long idEmployeRemplace, String employeRemplace, String matriculeRemplace,
                               String motifRemplacement,
                               LocalDate dateDebutRemplacement, LocalDate dateFinRemplacement,
                               String intituleDemande, String descriptionPoste, String profilRecherche,
                               BigDecimal salaireMinEstime, BigDecimal salaireMaxEstime,
                               Integer effectif, BigDecimal budgetEstime,
                               StatutLigne statut, EtapeValidation etapeCourante,
                               int episode, LocalDateTime dateCreation, LocalDateTime dateModification,
                               LocalDateTime dateSoumission,
                               List<ValidationDto.Historique> validations,
                               List<ValidationDto.VerificationBudgetaire> verificationsBudgetaires) {
    }

    /** Detail complet d'une demande. */
    public record Detail(Long id, String motif, LocalDate dateSouhaitee, StatutDemande statut,
                         LocalDateTime dateCreation, LocalDateTime dateModification,
                         LocalDateTime dateSoumission,
                         String demandeur, Long idDemandeur,
                         List<LigneDetail> lignes,
                         List<ValidationDto.ChangementStatut> historique) {
    }

    /** Transformation d'une demande en resume. */
    public static Resume versResume(DemandeRecrutement demande, List<DemandeRecrutementPoste> lignes) {
        return new Resume(
                demande.getId(),
                demande.getMotif(),
                demande.getDateSouhaitee(),
                demande.getStatut(),
                demande.getDateCreation(),
                demande.getDateSoumission(),
                demande.getUtilisateur().getNom(),
                demande.getUtilisateur().getId(),
                lignes.size(),
                (int) lignes.stream().filter(l -> l.getStatut() == StatutLigne.ACCEPTEE).count(),
                (int) lignes.stream().filter(l -> l.getStatut().estEnValidation()).count(),
                (int) lignes.stream().filter(l -> l.getStatut() == StatutLigne.A_CORRIGER).count(),
                lignes.stream().map(DemandeRecrutementPoste::getBudgetEstime)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
    }
}
