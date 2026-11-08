package com.tovo.erp.contrat.dto;

import com.tovo.erp.contrat.model.Affectation;
import com.tovo.erp.contrat.model.AffectationStatut;
import com.tovo.erp.contrat.model.CategorieTache;
import com.tovo.erp.contrat.model.Contrat;
import com.tovo.erp.contrat.model.ContratEtat;
import com.tovo.erp.contrat.model.MotifAffectation;
import com.tovo.erp.contrat.model.Onboarding;
import com.tovo.erp.contrat.model.OnboardingStatut;
import com.tovo.erp.contrat.model.OnboardingTache;
import com.tovo.erp.contrat.model.StatutExecutionContrat;
import com.tovo.erp.contrat.model.TacheStatut;
import com.tovo.erp.contrat.model.TypeContrat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Objets de transfert du module contrat / affectation / onboarding. */
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

    /** Resultat de la creation automatique : contrat, affectation et onboarding. */
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
            TypeContrat typeContrat,
            Integer rang,
            LocalDate dateDebut,
            LocalDate dateFinPrevue,
            LocalDate finPeriodeEssai,
            BigDecimal salaireBase,
            String devise,
            ContratEtat etat,
            StatutExecutionContrat statutExecution,
            Long documentPdfId,
            LocalDateTime dateCreation) {

        public static ContratDto de(Contrat contrat) {
            return new ContratDto(
                    contrat.getId(),
                    contrat.getNumero(),
                    contrat.getIdEmploye(),
                    contrat.getIdProposition(),
                    contrat.getTypeContrat(),
                    contrat.getRang(),
                    contrat.getDateDebut(),
                    contrat.getDateFinPrevue(),
                    contrat.getFinPeriodeEssai(),
                    contrat.getSalaireBase(),
                    contrat.getDevise(),
                    contrat.getEtat(),
                    contrat.getStatutExecution(),
                    contrat.getDocumentPdfId(),
                    contrat.getDateCreation());
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
            TacheStatut statut,
            LocalDate dateFait) {

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
                    tache.getStatut(),
                    tache.getDateFait());
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
                    taches.stream().map(TacheDto::de).toList());
        }
    }
}
