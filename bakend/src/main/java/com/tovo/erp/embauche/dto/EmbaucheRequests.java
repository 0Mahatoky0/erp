package com.tovo.erp.embauche.dto;

import com.tovo.erp.embauche.model.DecisionProposition;
import com.tovo.erp.embauche.model.DecisionContrat;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class EmbaucheRequests {
    private EmbaucheRequests() {
    }

    public record PropositionRequest(
            @NotNull Long idCandidature,
            @NotBlank @Size(max = 20) String typeContrat,
            @PositiveOrZero BigDecimal salairePropose,
            @Size(max = 255) String lieuTravail,
            @NotNull LocalDateTime dateDebut,
            LocalDateTime dateFin) {
    }

    public record ReponsePropositionRequest(
            @NotNull DecisionProposition decision,
            @PositiveOrZero BigDecimal salaireSouhaite,
            @Size(max = 255) String lieuSouhaite,
            @Size(max = 20) String typeContratSouhaite,
            LocalDateTime dateDebutSouhaitee,
            LocalDateTime dateFinSouhaitee,
            String commentaire) {
    }

    public record ReponseContratRequest(
            @NotNull DecisionContrat decision) {
    }
}
