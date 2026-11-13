package com.tovo.erp.besoin.dto;

import com.tovo.erp.besoin.model.DemandeRecrutement;
import com.tovo.erp.besoin.model.DemandeRecrutementPoste;
import com.tovo.erp.besoin.model.EtapeValidation;
import com.tovo.erp.besoin.model.StatutDemande;
import com.tovo.erp.besoin.model.StatutLigne;
import com.tovo.erp.besoin.model.TypeBesoin;
import com.tovo.erp.besoin.model.TypeRemplacement;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

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

    /**
     * Contenu d'une ligne de demande (RM-015, RM-022 a RM-030).
     *
     * <p>Le manager exprime seulement le besoin : la fourchette salariale et le budget
     * sont definis par le RH lors de sa validation (voir {@link ValidationDto.DecisionRequest}).
     */
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
                                 @NotNull @Positive Integer effectif) {
    }

    /** Modification d'une ligne : uniquement possible tant que la demande est BROUILLON. */
    public record LigneModification(Long idPoste,
                                     String motifRemplacement,
                                     LocalDate dateDebutRemplacement,
                                     LocalDate dateFinRemplacement,
                                     @NotBlank String intituleDemande,
                                     String descriptionPoste,
                                     String profilRecherche,
                                     @NotNull @Positive Integer effectif) {
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
                               Integer effectif, Integer nombrePourvus, BigDecimal budgetEstime,
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
    public static Resume versResume(DemandeRecrutement demande, List<DemandeRecrutementPoste> lignes,
                                    String demandeur, Long idDemandeur) {
        return new Resume(
                demande.getId(),
                demande.getMotif(),
                demande.getDateSouhaitee(),
                demande.getStatut(),
                demande.getDateCreation(),
                demande.getDateSoumission(),
                demandeur,
                idDemandeur,
                lignes.size(),
                (int) lignes.stream().filter(l -> l.getStatut() == StatutLigne.ACCEPTEE).count(),
                (int) lignes.stream().filter(l -> l.getStatut().estEnValidation()).count(),
                (int) lignes.stream().filter(l -> l.getStatut() == StatutLigne.A_CORRIGER).count(),
                // Les lignes que le RH n'a pas encore chiffrees n'ont pas de budget.
                lignes.stream().map(DemandeRecrutementPoste::getBudgetEstime)
                        .filter(java.util.Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
    }
}
