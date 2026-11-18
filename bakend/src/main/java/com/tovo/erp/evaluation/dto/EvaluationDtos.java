package com.tovo.erp.evaluation.dto;

import com.tovo.erp.evaluation.model.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class EvaluationDtos {
    private EvaluationDtos() {
    }

    public record CompetenceDto(Long id, String code, String libelle, String description, String categorie,
                                String echelle, Integer dureeValiditeMois, boolean actif) {
        public static CompetenceDto de(Competence c) {
            return new CompetenceDto(c.getId(), c.getCode(), c.getLibelle(), c.getDescription(), c.getCategorie(),
                    c.getEchelle(), c.getDureeValiditeMois(), c.isActif());
        }
    }

    public record CompetenceRequest(@NotBlank String code, @NotBlank String libelle, String description,
                                    @NotBlank String categorie, String echelle, Integer dureeValiditeMois,
                                    Boolean actif) {
    }

    public record PosteCompetenceDto(Long id, Long idPoste, Long idCompetence, int niveauRequis, int poids) {
        public static PosteCompetenceDto de(PosteCompetence p) {
            return new PosteCompetenceDto(p.getId(), p.getIdPoste(), p.getIdCompetence(), p.getNiveauRequis(), p.getPoids());
        }
    }

    public record PosteCompetenceRequest(@NotNull Long idPoste, @NotNull Long idCompetence,
                                         @NotNull @Min(1) @Max(5) Integer niveauRequis,
                                         @NotNull @Min(1) @Max(3) Integer poids) {
    }

    public record EmployeCompetenceDto(Long id, Long idEmploye, Long idCompetence, int niveauAcquis,
                                       LocalDate dateEvaluation, LocalDate dateValidite, String source,
                                       Long evaluationId, Long formationId, Long valideParId) {
        public static EmployeCompetenceDto de(EmployeCompetence c) {
            return new EmployeCompetenceDto(c.getId(), c.getIdEmploye(), c.getIdCompetence(), c.getNiveauAcquis(),
                    c.getDateEvaluation(), c.getDateValidite(), c.getSource(), c.getEvaluationId(),
                    c.getFormationId(), c.getValideParId());
        }
    }

    public record EmployeCompetenceRequest(@NotNull Long idEmploye, @NotNull Long idCompetence,
                                           @NotNull @Min(1) @Max(5) Integer niveauAcquis,
                                           @NotNull LocalDate dateEvaluation, LocalDate dateValidite,
                                           @NotBlank String source, Long evaluationId, Long formationId,
                                           Long valideParId, @NotBlank String justificatif) {
    }

    public record CampagneDto(Long id, String libelle, String type, LocalDate periodeDebut, LocalDate periodeFin,
                              LocalDate dateOuverture, LocalDate dateCloture, String grilleJson,
                              String ponderationsJson, String population, String statut) {
        public static CampagneDto de(CampagneEvaluation c) {
            return new CampagneDto(c.getId(), c.getLibelle(), c.getType(), c.getPeriodeDebut(), c.getPeriodeFin(),
                    c.getDateOuverture(), c.getDateCloture(), c.getGrilleJson(), c.getPonderationsJson(),
                    c.getPopulation(), c.getStatut());
        }
    }

    public record CampagneRequest(@NotBlank String libelle, @NotBlank String type,
                                  @NotNull LocalDate periodeDebut, @NotNull LocalDate periodeFin,
                                  @NotNull LocalDate dateOuverture, @NotNull LocalDate dateCloture,
                                  String grilleJson, String ponderationsJson, String population) {
    }

    public record ObjectifDto(Long id, Long idEmploye, Long idCampagne, Long parentObjectifId, String libelle,
                              String description, String indicateur, String unite, BigDecimal valeurCible,
                              BigDecimal valeurRealisee, String sens, BigDecimal poids, LocalDate dateEcheance,
                              String statut, BigDecimal tauxAtteinte, int version, String motifAvenant) {
        public static ObjectifDto de(Objectif o) {
            return new ObjectifDto(o.getId(), o.getIdEmploye(), o.getIdCampagne(), o.getParentObjectifId(),
                    o.getLibelle(), o.getDescription(), o.getIndicateur(), o.getUnite(), o.getValeurCible(),
                    o.getValeurRealisee(), o.getSens(), o.getPoids(), o.getDateEcheance(), o.getStatut(),
                    o.getTauxAtteinte(), o.getVersion(), o.getMotifAvenant());
        }
    }

    public record ObjectifRequest(@NotNull Long idEmploye, @NotNull Long idCampagne, Long parentObjectifId,
                                  @NotBlank String libelle, String description, @NotBlank String indicateur,
                                  String unite, @NotNull BigDecimal valeurCible, BigDecimal valeurRealisee,
                                  @NotBlank String sens, @NotNull BigDecimal poids, @NotNull LocalDate dateEcheance) {
    }

    public record ObjectifAvenantRequest(BigDecimal valeurRealisee, BigDecimal poids, LocalDate dateEcheance,
                                         @NotBlank String motif) {
    }

    public record ObjectifValidationDto(Long idEmploye, Long idCampagne, int nombreObjectifs,
                                        BigDecimal sommePoids, boolean valide) {
    }

    public record ObjectifAvenantResult(ObjectifDto avenant, ObjectifDto precedent) {
    }

    public record EvaluationDto(Long id, Long idEmploye, Long idCampagne, Long idEvaluateur, Long idValideurN2,
                                String autoEvaluation, String evaluationManager, BigDecimal noteObjectifs,
                                BigDecimal noteCompetences, BigDecimal noteComportement, BigDecimal noteGlobale,
                                String appreciation, String pointsForts, String axesProgres,
                                LocalDateTime dateEntretien, String commentaireSalarie,
                                LocalDateTime dateSignatureSalarie, String statut, int version,
                                String motifContestation) {
        public static EvaluationDto de(Evaluation e) {
            return new EvaluationDto(e.getId(), e.getIdEmploye(), e.getIdCampagne(), e.getIdEvaluateur(),
                    e.getIdValideurN2(), e.getAutoEvaluation(), e.getEvaluationManager(), e.getNoteObjectifs(),
                    e.getNoteCompetences(), e.getNoteComportement(), e.getNoteGlobale(), e.getAppreciation(),
                    e.getPointsForts(), e.getAxesProgres(), e.getDateEntretien(), e.getCommentaireSalarie(),
                    e.getDateSignatureSalarie(), e.getStatut(), e.getVersion(), e.getMotifContestation());
        }
    }

    public record AutoEvaluationRequest(String pointsForts, String axesProgres, String commentaire) {
    }

    public record EvaluationManagerRequest(Map<String, Integer> notesComportement,
                                           String pointsForts, String axesProgres, String appreciation) {
    }

    public record EntretienRequest(@NotNull LocalDateTime dateEntretien, String compteRendu) {
    }

    public record ValidationN2Request(@NotNull Long idValideurN2, String commentaire) {
    }

    public record SignatureRequest(String commentaire, boolean signer) {
    }

    public record ContestationRequest(@NotBlank String motif) {
    }

    public record EcartDto(Long id, Long idEmploye, Long idCompetence, Long idEvaluation, int niveauRequis,
                           int niveauAcquis, int ecart, BigDecimal prioriteScore, String priorite, String statut) {
        public static EcartDto de(EcartCompetence e) {
            return new EcartDto(e.getId(), e.getIdEmploye(), e.getIdCompetence(), e.getIdEvaluation(),
                    e.getNiveauRequis(), e.getNiveauAcquis(), e.getEcart(), e.getPrioriteScore(),
                    e.getPriorite(), e.getStatut());
        }
    }

    public record BesoinFormationDto(Long id, Long idEmploye, Long idCompetence, int niveauCible, String origine,
                                     String priorite, LocalDate dateCible, BigDecimal budgetEstime, String statut,
                                     String motifRefus, Long sourceEvaluationId, LocalDateTime dateCreation) {
        public static BesoinFormationDto de(BesoinFormation b) {
            return new BesoinFormationDto(b.getId(), b.getIdEmploye(), b.getIdCompetence(), b.getNiveauCible(),
                    b.getOrigine(), b.getPriorite(), b.getDateCible(), b.getBudgetEstime(), b.getStatut(),
                    b.getMotifRefus(), b.getSourceEvaluationId(), b.getDateCreation());
        }
    }

    public record BesoinFormationRequest(@NotNull Long idEmploye, @NotNull Long idCompetence,
                                         @NotNull @Min(1) @Max(5) Integer niveauCible,
                                         @NotBlank String origine, LocalDate dateCible, BigDecimal budgetEstime) {
    }

    public record DecisionBesoinRequest(boolean approuver, String motif) {
    }

    public record FormationDto(Long id, String code, String titre, String type, String organisme,
                               BigDecimal dureeH, BigDecimal cout, boolean certifiante,
                               String competencesVisees, BigDecimal seuilDedit) {
        public static FormationDto de(Formation f) {
            return new FormationDto(f.getId(), f.getCode(), f.getTitre(), f.getType(), f.getOrganisme(),
                    f.getDureeH(), f.getCout(), f.isCertifiante(), f.getCompetencesVisees(), f.getSeuilDedit());
        }
    }

    public record FormationRequest(@NotBlank String code, @NotBlank String titre, @NotBlank String type,
                                   String organisme, @NotNull BigDecimal dureeH, @NotNull BigDecimal cout,
                                   boolean certifiante, String competencesVisees, BigDecimal seuilDedit) {
    }

    public record SessionDto(Long id, Long idFormation, LocalDate dateDebut, LocalDate dateFin, String lieu,
                             int capacite, String formateur, BigDecimal cout, String statut, long confirms) {
        public static SessionDto de(SessionFormation s, long confirms) {
            return new SessionDto(s.getId(), s.getIdFormation(), s.getDateDebut(), s.getDateFin(), s.getLieu(),
                    s.getCapacite(), s.getFormateur(), s.getCout(), s.getStatut(), confirms);
        }
    }

    public record SessionRequest(@NotNull Long idFormation, @NotNull LocalDate dateDebut,
                                 @NotNull LocalDate dateFin, String lieu, @NotNull Integer capacite,
                                 String formateur, @NotNull BigDecimal cout) {
    }

    public record StatutSessionRequest(@NotBlank String statut) {
    }

    public record PlanFormationDto(Long id, int annee, Long idDepartement, BigDecimal budget) {
        public static PlanFormationDto de(PlanFormation p) {
            return new PlanFormationDto(p.getId(), p.getAnnee(), p.getIdDepartement(), p.getBudget());
        }
    }

    public record PlanFormationRequest(@NotNull Integer annee, @NotNull Long idDepartement,
                                       @NotNull BigDecimal budget) {
    }

    public record InscriptionDto(Long id, Long idSession, Long idEmploye, LocalDateTime dateDemande,
                                 String statut, BigDecimal presenceH, String resultat,
                                 Long certificatDocumentId, Long idBesoin) {
        public static InscriptionDto de(InscriptionFormation i) {
            return new InscriptionDto(i.getId(), i.getIdSession(), i.getIdEmploye(), i.getDateDemande(),
                    i.getStatut(), i.getPresenceH(), i.getResultat(), i.getCertificatDocumentId(), i.getIdBesoin());
        }
    }

    public record InscriptionRequest(@NotNull Long idSession, @NotNull Long idEmploye, Long idBesoin) {
    }

    public record DecisionInscriptionRequest(boolean approuver, String motif) {
    }

    public record PresenceRequest(@NotNull BigDecimal presenceH, String resultat, Long certificatDocumentId) {
    }

    public record EvaluationFormationDto(Long id, Long idInscription, String type, Integer satisfaction,
                                         BigDecimal acquis, BigDecimal transfert, String commentaire,
                                         LocalDateTime dateEvaluation) {
        public static EvaluationFormationDto de(EvaluationFormation e) {
            return new EvaluationFormationDto(e.getId(), e.getIdInscription(), e.getType(), e.getSatisfaction(),
                    e.getAcquis(), e.getTransfert(), e.getCommentaire(), e.getDateEvaluation());
        }
    }

    public record EvaluationFormationRequest(@NotBlank String type, @Min(1) @Max(5) Integer satisfaction,
                                             @DecimalMin("0") @DecimalMax("5") BigDecimal acquis,
                                             @DecimalMin("0") @DecimalMax("5") BigDecimal transfert,
                                             String commentaire) {
    }

    public record ConventionFormationRequest(@NotNull BigDecimal cout, @NotNull Integer dureeEngagementMois,
                                             @NotBlank String clauseDedit) {
    }

    public record RemboursementDto(BigDecimal cout, int moisRestants, int dureeEngagementMois,
                                   BigDecimal remboursement) {
    }

    public record IndicateursDto(BigDecimal tauxParticipation, Map<String, Long> distributionNotes,
                                 BigDecimal heuresFormationParSalarie, BigDecimal budgetConsomme,
                                 BigDecimal tauxRealisationPlan, BigDecimal tauxComblement) {
    }
}
