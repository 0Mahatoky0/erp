package com.tovo.erp.contrat.dto;

import com.tovo.erp.contrat.model.Affectation;
import com.tovo.erp.contrat.model.AffectationStatut;
import com.tovo.erp.contrat.model.Avenant;
import com.tovo.erp.contrat.model.AvenantEtat;
import com.tovo.erp.contrat.model.CategorieTache;
import com.tovo.erp.contrat.model.Contrat;
import com.tovo.erp.contrat.model.ContratEtat;
import com.tovo.erp.contrat.model.MotifAffectation;
import com.tovo.erp.contrat.model.Onboarding;
import com.tovo.erp.contrat.model.OnboardingStatut;
import com.tovo.erp.contrat.model.OnboardingTache;
import com.tovo.erp.contrat.model.StatutExecutionContrat;
import com.tovo.erp.contrat.model.TacheStatut;
import com.tovo.erp.contrat.model.TypeAvenant;
import com.tovo.erp.contrat.model.TypeContrat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Objets de transfert du module contrat / affectation / onboarding (bloc 4). */
public final class ContratDtos {

    private ContratDtos() {
    }

    /** Demande interne de creation du contrat a l'acceptation. */
    public record CreationContratRequest(
            Long idEmploye,
            Long idProposition,
            TypeContrat typeContrat,
            LocalDate dateDebut,
            LocalDate dateFinPrevue,
            BigDecimal salaireBase,
            String devise,
            String primesAvantages,
            String horaires,
            String lieuTravail,
            String motifCdd,
            Long idPoste,
            Long idDepartement,
            Long idService,
            Long idManager,
            Integer periodeEssaiMois,
            Long idResponsableRh) {
    }

    /** Resultat de la creation automatique : contrat et affectation. */
    public record ContratEmbaucheDto(
            Long idContrat,
            String numero,
            Long idEmploye,
            Long idProposition,
            TypeContrat typeContrat,
            LocalDate dateDebut,
            LocalDate dateFinPrevue,
            LocalDate finPeriodeEssai,
            BigDecimal salaireBase,
            String devise,
            ContratEtat etat,
            StatutExecutionContrat statutExecution,
            Long documentPdfId,
            Long idAffectation,
            Long idOnboarding,
            List<Long> idTaches) {
    }

    /** Contrat expose en lecture. */
    public record ContratDto(
            Long id,
            String numero,
            Long idEmploye,
            Long idProposition,
            Long idContratPrecedent,
            Integer rang,
            TypeContrat typeContrat,
            LocalDate dateDebut,
            LocalDate dateFinPrevue,
            LocalDate dateFinReelle,
            LocalDate finPeriodeEssai,
            BigDecimal salaireBase,
            String devise,
            String primesAvantages,
            String horaires,
            String lieuTravail,
            String motifCdd,
            ContratEtat etat,
            StatutExecutionContrat statutExecution,
            LocalDateTime dateSignatureSalarie,
            LocalDateTime dateSignatureEmployeur,
            Long signataireEmployeurId,
            Long documentPdfId,
            String gabaritVersion,
            String motifAnnulation,
            LocalDateTime dateAnnulation,
            LocalDateTime dateCloture,
            LocalDateTime dateCreation,
            LocalDateTime dateModification) {

        public static ContratDto de(Contrat contrat) {
            return new ContratDto(
                    contrat.getId(),
                    contrat.getNumero(),
                    contrat.getIdEmploye(),
                    contrat.getIdProposition(),
                    contrat.getIdContratPrecedent(),
                    contrat.getRang(),
                    contrat.getTypeContrat(),
                    contrat.getDateDebut(),
                    contrat.getDateFinPrevue(),
                    contrat.getDateFinReelle(),
                    contrat.getFinPeriodeEssai(),
                    contrat.getSalaireBase(),
                    contrat.getDevise(),
                    contrat.getPrimesAvantages(),
                    contrat.getHoraires(),
                    contrat.getLieuTravail(),
                    contrat.getMotifCdd(),
                    contrat.getEtat(),
                    contrat.getStatutExecution(),
                    contrat.getDateSignatureSalarie(),
                    contrat.getDateSignatureEmployeur(),
                    contrat.getSignataireEmployeurId(),
                    contrat.getDocumentPdfId(),
                    contrat.getGabaritVersion(),
                    contrat.getMotifAnnulation(),
                    contrat.getDateAnnulation(),
                    contrat.getDateCloture(),
                    contrat.getDateCreation(),
                    contrat.getDateModification());
        }
    }

    /** Affectation exposee en lecture. */
    public record AffectationDto(
            Long id,
            Long idEmploye,
            Long idContrat,
            Long idPoste,
            Long idDepartement,
            Long idService,
            Long idManager,
            String lieuTravail,
            LocalDate dateDebut,
            LocalDate dateFin,
            Integer tauxActivite,
            boolean principale,
            MotifAffectation motif,
            AffectationStatut statut) {

        public static AffectationDto de(Affectation affectation) {
            return new AffectationDto(
                    affectation.getId(),
                    affectation.getIdEmploye(),
                    affectation.getIdContrat(),
                    affectation.getIdPoste(),
                    affectation.getIdDepartement(),
                    affectation.getIdService(),
                    affectation.getIdManager(),
                    affectation.getLieuTravail(),
                    affectation.getDateDebut(),
                    affectation.getDateFin(),
                    affectation.getTauxActivite(),
                    affectation.isPrincipale(),
                    affectation.getMotif(),
                    affectation.getStatut());
        }
    }

    /** Tache d'integration exposee en lecture. */
    public record TacheDto(
            Long id,
            String code,
            String libelle,
            CategorieTache categorie,
            String responsable,
            LocalDate echeance,
            boolean obligatoire,
            boolean bloquante,
            Long idDependDe,
            TacheStatut statut,
            LocalDate dateFait,
            Long idPreuveDocument) {

        public static TacheDto de(OnboardingTache tache) {
            return new TacheDto(
                    tache.getId(),
                    tache.getCode(),
                    tache.getLibelle(),
                    tache.getCategorie(),
                    tache.getResponsable(),
                    tache.getEcheance(),
                    tache.isObligatoire(),
                    tache.isBloquante(),
                    tache.getIdDependDe(),
                    tache.getStatut(),
                    tache.getDateFait(),
                    tache.getIdPreuveDocument());
        }
    }

    /** Onboarding expose en lecture avec ses taches. */
    public record OnboardingDto(
            Long id,
            Long idEmploye,
            Long idContrat,
            LocalDate dateDebut,
            LocalDate dateCible,
            Long idResponsableRh,
            OnboardingStatut statut,
            Integer progression,
            LocalDateTime dateCloture,
            List<TacheDto> taches) {

        public static OnboardingDto de(Onboarding onboarding, List<OnboardingTache> taches) {
            return new OnboardingDto(
                    onboarding.getId(),
                    onboarding.getIdEmploye(),
                    onboarding.getIdContrat(),
                    onboarding.getDateDebut(),
                    onboarding.getDateCible(),
                    onboarding.getIdResponsableRh(),
                    onboarding.getStatut(),
                    onboarding.getProgression(),
                    onboarding.getDateCloture(),
                    taches.stream().map(TacheDto::de).toList());
        }
    }

    /** Signature du contrat par le salarie puis l'employeur (RG-B4-05). */
    public record SignatureRequest(Long signataireEmployeurId) {
    }

    /** Annulation d'un contrat CREE ou retractation avant la date de debut. */
    public record AnnulationRequest(@NotBlank String motif) {
    }

    /** Renouvellement d'un CDD dans la limite legale (RG-B4-12). */
    public record RenouvellementRequest(
            @NotNull LocalDate dateFinPrevue,
            BigDecimal salaireBase,
            String primesAvantages,
            String horaires,
            String lieuTravail,
            @NotBlank String motifCdd,
            Long signataireEmployeurId) {
    }

    /** Transformation d'un CDD en CDI. */
    public record TransformationCdiRequest(
            LocalDate dateDebut,
            BigDecimal salaireBase,
            String primesAvantages,
            String horaires,
            String lieuTravail,
            Long signataireEmployeurId) {
    }

    /** Creation d'une affectation supplementaire (mutation, promotion...). */
    public record AffectationRequest(
            Long idPoste,
            Long idDepartement,
            Long idService,
            Long idManager,
            String lieuTravail,
            String horaires,
            @NotNull LocalDate dateDebut,
            LocalDate dateFin,
            @Min(1) Integer tauxActivite,
            boolean principale,
            @NotNull MotifAffectation motif) {
    }

    /** Modification d'un contrat signe (RG-B4-05). */
    public record AvenantRequest(
            @NotNull TypeAvenant type,
            @NotNull LocalDate dateEffet,
            String anciennesValeurs,
            String nouvellesValeurs,
            @DecimalMin("0") BigDecimal nouveauSalaire,
            LocalDate nouvelleDateFin,
            String nouveauxHoraires,
            String nouveauLieu,
            Long idDocument) {
    }

    /** Avenant expose en lecture. */
    public record AvenantDto(
            Long id,
            Long idContrat,
            String numero,
            TypeAvenant type,
            LocalDate dateEffet,
            String anciennesValeurs,
            String nouvellesValeurs,
            BigDecimal nouveauSalaire,
            LocalDate nouvelleDateFin,
            String nouveauxHoraires,
            String nouveauLieu,
            Long idDocument,
            AvenantEtat etat,
            LocalDateTime dateSignature,
            Long signataireId,
            LocalDateTime dateCreation) {

        public static AvenantDto de(Avenant avenant) {
            return new AvenantDto(
                    avenant.getId(),
                    avenant.getIdContrat(),
                    avenant.getNumero(),
                    avenant.getType(),
                    avenant.getDateEffet(),
                    avenant.getAnciennesValeurs(),
                    avenant.getNouvellesValeurs(),
                    avenant.getNouveauSalaire(),
                    avenant.getNouvelleDateFin(),
                    avenant.getNouveauxHoraires(),
                    avenant.getNouveauLieu(),
                    avenant.getIdDocument(),
                    avenant.getEtat(),
                    avenant.getDateSignature(),
                    avenant.getSignataireId(),
                    avenant.getDateCreation());
        }
    }

    /** Mise a jour d'une tache d'integration. */
    public record TacheMiseAJourRequest(
            @NotNull TacheStatut statut,
            LocalDate dateFait,
            Long idPreuveDocument) {
    }

    /** Alerte calculee a la demande (aucune notification poussee entre modules). */
    public record AlerteDto(
            String categorie,
            String niveau,
            String message,
            Long idContrat,
            String numeroContrat,
            Long idEmploye,
            LocalDate echeance) {
    }
}
