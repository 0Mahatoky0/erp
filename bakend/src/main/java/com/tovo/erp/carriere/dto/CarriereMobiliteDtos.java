package com.tovo.erp.carriere.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tovo.erp.carriere.model.CampagneAugmentation;
import com.tovo.erp.carriere.model.MouvementCarriere;
import com.tovo.erp.carriere.model.StatutMouvementCarriere;
import com.tovo.erp.carriere.model.TypeMouvementCarriere;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

public final class CarriereMobiliteDtos {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private CarriereMobiliteDtos() {
    }

    public record MouvementDto(
            Long id,
            String reference,
            Long idEmploye,
            TypeMouvementCarriere type,
            String initiateur,
            String motif,
            String justification,
            LocalDate dateEffetSouhaitee,
            LocalDate dateEffet,
            StatutMouvementCarriere statut,
            Long idDemandeur,
            Long idValidateur,
            boolean derogationDirection,
            String motifDerogation,
            Map<String, Object> conditions,
            Map<String, Object> details,
            Map<String, Object> ancienneSituation,
            Map<String, Object> nouvelleSituation,
            LocalDateTime dateCreation,
            LocalDateTime dateApplication) {

        public static MouvementDto de(MouvementCarriere mouvement, Map<String, Object> conditions,
                                      Map<String, Object> details) {
            return new MouvementDto(
                    mouvement.getId(), mouvement.getReference(), mouvement.getIdEmploye(),
                    mouvement.getType(), mouvement.getInitiateur(), mouvement.getMotif(),
                    mouvement.getJustification(), mouvement.getDateEffetSouhaitee(),
                    mouvement.getDateEffet(), mouvement.getStatut(), mouvement.getIdDemandeur(),
                    mouvement.getIdValidateur(), mouvement.isDerogationDirection(),
                    mouvement.getMotifDerogation(), conditions, details,
                    lireMap(mouvement.getAncienneSituation()), lireMap(mouvement.getNouvelleSituation()),
                    mouvement.getDateCreation(), mouvement.getDateApplication());
        }

        private static Map<String, Object> lireMap(String valeur) {
            if (valeur == null || valeur.isBlank()) {
                return Map.of();
            }
            try {
                return MAPPER.readValue(valeur, Map.class);
            } catch (Exception e) {
                return Map.of();
            }
        }
    }

    public record PromotionRequest(
            String nature,
            @NotNull Long idAncienPoste,
            @NotNull Long idNouveauPoste,
            String ancienneCategorie,
            String nouvelleCategorie,
            String ancienEchelon,
            String nouveauEchelon,
            @NotNull @DecimalMin("0.0") BigDecimal ancienSalaire,
            @NotNull @DecimalMin("0.0") BigDecimal nouveauSalaire) {
    }

    public record MutationRequest(
            @NotBlank String type,
            @NotBlank String initiative,
            Long idPosteOrigine,
            Long idPosteDestination,
            Long idServiceOrigine,
            Long idServiceDestination,
            String idLieuOrigine,
            String idLieuDestination,
            Long idManagerOrigine,
            Long idManagerDestination,
            LocalDate dateRetourPrevue,
            boolean clauseMobiliteVerifiee,
            Long accordSalarieDocumentId,
            BigDecimal indemniteMobilite) {
    }

    public record MouvementRequest(
            @NotNull Long idEmploye,
            @NotNull TypeMouvementCarriere type,
            @NotBlank String initiateur,
            @NotBlank String motif,
            @NotBlank String justification,
            @NotNull LocalDate dateEffetSouhaitee,
            Long idEvaluation,
            Long idAvenant,
            String documents,
            Boolean derogationDirection,
            String motifDerogation,
            PromotionRequest promotion,
            MutationRequest mutation,
            @DecimalMin("0.0") BigDecimal nouveauSalaire) {
    }

    public record DecisionRequest(String motif, Boolean derogationDirection, String motifDerogation) {
    }

    public record CarriereDateDto(
            Long idEmploye,
            LocalDate dateValidite,
            LocalDate dateConnaissance,
            String contrat,
            Long idPoste,
            Long idService,
            Long idDepartement,
            String lieuTravail,
            Long idManager,
            String categorie,
            String echelon,
            Integer tauxActivite,
            String regimeTravail,
            String statutAffectation,
            BigDecimal salaireBrut,
            String motifAffectation) {
    }

    public record FriseLigneDto(
            String source,
            Long id,
            String type,
            LocalDate date,
            String libelle,
            String confidentialite) {
    }

    public record RapportIntegriteDto(
            int chevauchements,
            int trous,
            int lignesHorsContrat,
            int mouvementsSansHistorique,
            java.util.List<String> anomalies) {
    }

    public record CampagneDto(
            Long id,
            String libelle,
            LocalDate dateEffet,
            BigDecimal enveloppeBudgetaire,
            String population,
            String regle,
            String statut,
            Map<String, Object> simulation) {

        public static CampagneDto de(CampagneAugmentation campagne, Map<String, Object> simulation) {
            return new CampagneDto(campagne.getId(), campagne.getLibelle(), campagne.getDateEffet(),
                    campagne.getEnveloppeBudgetaire(), campagne.getPopulation(), campagne.getRegle(),
                    campagne.getStatut(), simulation);
        }
    }

    public record CampagneRequest(
            @NotBlank String libelle,
            @NotNull LocalDate dateEffet,
            @NotNull @DecimalMin("0.0") BigDecimal enveloppeBudgetaire,
            @NotBlank String population,
            @NotBlank String regle) {
    }
}
