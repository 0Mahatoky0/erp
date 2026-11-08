package com.tovo.erp.embauche.dto;

import com.tovo.erp.embauche.model.AuteurProposition;
import com.tovo.erp.embauche.model.HistoriqueProposition;
import com.tovo.erp.embauche.model.PropositionEmbauche;
import com.tovo.erp.embauche.model.PropositionStatut;
import com.tovo.erp.embauche.model.ReponseCandidat;
import com.tovo.erp.embauche.model.TypeContratEmbauche;
import com.tovo.erp.embauche.model.TypeReponseCandidat;
import com.tovo.erp.embauche.model.VersionProposition;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Objets de transfert du module embauche (section 5). */
public final class PropositionDtos {

    private PropositionDtos() {
    }

    /** Creation ou modification d'une proposition BROUILLON. */
    public record Creation(
            @NotNull Long idCandidature,
            Long idPoste,
            Long idDepartement,
            Long idService,
            Long idManager,
            @NotNull TypeContratEmbauche typeContrat,
            Integer dureeMois,
            @NotNull LocalDate dateDebutPrevue,
            @Min(0) Integer periodeEssaiMois,
            @NotNull @DecimalMin("0") BigDecimal salaireBrutMensuel,
            String devise,
            String primesAvantages,
            String horaires,
            String lieuTravail,
            Boolean derogationFinanceDirection) {
    }

    /** Reponse saisie par un RH a la place du candidat (canal papier / email). */
    public record ReponseInterne(
            @NotNull TypeReponseCandidat typeReponse,
            String commentaire,
            String preuve) {
    }

    /** Refus exprime par le candidat. */
    public record Refus(@NotBlank String motif) {
    }

    /** Contre-proposition du candidat. */
    public record ContreProposition(@NotBlank String message) {
    }

    /** Prolongation de la date d'expiration. */
    public record Prolongation(@Min(1) int jours) {
    }

    /** Retrait d'une proposition par l'entreprise. */
    public record Retrait(String motif) {
    }

    /** Commentaire libre envoye avec une acceptation publique. */
    public record AcceptationPublique(String commentaire) {
    }

    /** Ligne de liste des propositions. */
    public record Resume(
            Long id,
            String reference,
            Integer version,
            boolean active,
            PropositionStatut statut,
            Long idCandidature,
            Long idCandidat,
            String candidat,
            String email,
            Long idPoste,
            String poste,
            TypeContratEmbauche typeContrat,
            LocalDate dateDebutPrevue,
            BigDecimal salaireBrutMensuel,
            String devise,
            LocalDateTime dateEnvoi,
            LocalDateTime dateExpiration,
            boolean derogationFinanceDirection,
            Long idEmployeCree,
            Long idContratCree) {
    }

    /** Detail complet d'une proposition, avec son journal et ses versions. */
    public record Detail(
            Long id,
            String reference,
            Integer version,
            boolean active,
            PropositionStatut statut,
            Long idCandidature,
            Long idCandidat,
            String candidatNom,
            String candidatPrenom,
            String candidatEmail,
            String candidatTelephone,
            String candidatAdresse,
            String candidatCin,
            LocalDate candidatDateNaissance,
            Long idOffre,
            String referenceOffre,
            String titreOffre,
            Long idDemandePoste,
            Long idPoste,
            String poste,
            Long idDepartement,
            Long idService,
            String service,
            Long idManager,
            TypeContratEmbauche typeContrat,
            Integer dureeMois,
            LocalDate dateDebutPrevue,
            Integer periodeEssaiMois,
            BigDecimal salaireBrutMensuel,
            String devise,
            String primesAvantages,
            String horaires,
            String lieuTravail,
            LocalDateTime dateEnvoi,
            LocalDateTime dateExpiration,
            String tokenAcceptation,
            LocalDateTime dateReponse,
            String motifRefus,
            String preuveReponse,
            boolean derogationFinanceDirection,
            Long idUtilisateurCreateur,
            Long idUtilisateurValidateur,
            Long idEmployeCree,
            Long idContratCree,
            Long idAffectationCreee,
            Long idOnboardingCree,
            Long idDocumentPdf,
            LocalDateTime dateCreation,
            LocalDateTime dateModification,
            List<HistoriqueDto> historique,
            List<VersionDto> versions,
            List<ReponseDto> reponses) {
    }

    public record HistoriqueDto(
            Long id,
            AuteurProposition auteurType,
            Long idUtilisateur,
            PropositionStatut ancienStatut,
            PropositionStatut nouveauStatut,
            String motif,
            LocalDateTime dateAction) {

        public static HistoriqueDto de(HistoriqueProposition historique) {
            return new HistoriqueDto(
                    historique.getId(),
                    historique.getAuteurType(),
                    historique.getIdUtilisateur(),
                    historique.getAncienStatut(),
                    historique.getNouveauStatut(),
                    historique.getMotif(),
                    historique.getDateAction());
        }
    }

    public record VersionDto(
            Long id,
            Integer numeroVersion,
            String contenu,
            Long idUtilisateur,
            LocalDateTime dateCreation) {

        public static VersionDto de(VersionProposition version) {
            return new VersionDto(
                    version.getId(),
                    version.getNumeroVersion(),
                    version.getContenu(),
                    version.getIdUtilisateur(),
                    version.getDateCreation());
        }
    }

    public record ReponseDto(
            Long id,
            TypeReponseCandidat typeReponse,
            String commentaire,
            String preuve,
            LocalDateTime dateReponse) {

        public static ReponseDto de(ReponseCandidat reponse) {
            return new ReponseDto(
                    reponse.getId(),
                    reponse.getTypeReponse(),
                    reponse.getCommentaire(),
                    reponse.getPreuve(),
                    reponse.getDateReponse());
        }
    }

    /** Vue limitee accessible avec le token envoye au candidat. */
    public record VuePublique(
            String reference,
            PropositionStatut statut,
            String candidatPrenom,
            String candidatNom,
            String poste,
            TypeContratEmbauche typeContrat,
            LocalDate dateDebutPrevue,
            BigDecimal salaireBrutMensuel,
            String devise,
            String lieuTravail,
            String primesAvantages,
            String horaires,
            LocalDateTime dateExpiration,
            boolean reponsePossible) {
    }

    /** Resultat idempotent de l'acceptation. */
    public record ResultatAcceptation(
            Long idProposition,
            String reference,
            PropositionStatut statut,
            Long idEmploye,
            String matricule,
            Long idContrat,
            String numeroContrat,
            TypeContratEmbauche typeContrat,
            LocalDate dateDebut,
            LocalDate dateFinPrevue,
            BigDecimal salaireBase,
            String devise,
            Long idAffectation,
            Long idOnboarding,
            List<Long> idTaches,
            Long idDocumentPdf) {
    }

    public static Resume versResume(PropositionEmbauche proposition,
                                     com.tovo.erp.recrutement.dto.CandidatureEmbaucheDto candidature,
                                     String nomPoste) {
        return new Resume(
                proposition.getId(),
                proposition.getReference(),
                proposition.getVersion(),
                proposition.isActive(),
                proposition.getStatut(),
                proposition.getIdCandidature(),
                candidature == null ? null : candidature.idCandidat(),
                candidature == null ? null : candidature.prenom() + " " + candidature.nom(),
                candidature == null ? null : candidature.email(),
                proposition.getIdPoste(),
                nomPoste,
                proposition.getTypeContrat(),
                proposition.getDateDebutPrevue(),
                proposition.getSalaireBrutMensuel(),
                proposition.getDevise(),
                proposition.getDateEnvoi(),
                proposition.getDateExpiration(),
                proposition.isDerogationFinanceDirection(),
                proposition.getIdEmployeCree(),
                proposition.getIdContratCree());
    }
}
