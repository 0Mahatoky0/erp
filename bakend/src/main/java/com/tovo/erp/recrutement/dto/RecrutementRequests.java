package com.tovo.erp.recrutement.dto;

import com.tovo.erp.recrutement.model.*;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class RecrutementRequests {
    private RecrutementRequests() {
    }

    public record CandidatureRequest(@NotNull Long idOffre,
            String messageCandidat, @Size(max = 100) String source) {
    }

    public record CandidatRequest(@NotBlank @Size(max = 100) String nom, @NotBlank @Size(max = 100) String prenom,
            @NotBlank @Email @Size(max = 255) String email, @Size(max = 30) String telephone,
            String adresse) {
        public Candidat toEntity() {
            Candidat c = new Candidat();
            c.setNom(nom);
            c.setPrenom(prenom);
            c.setEmail(email);
            c.setTelephone(telephone);
            c.setAdresse(adresse);
            return c;
        }
    }

    public record StatutRequest(@NotNull CandidatureStatut statut, String commentaire) {
    }

    public record EntretienRequest(@NotNull Long idCandidature,
            @NotNull TypeEntretien typeEntretien, @NotNull LocalDateTime dateDebut,
            @NotNull LocalDateTime dateFin, String lieu, String lienVisio) {
    }

    public record DecisionRequest(@NotNull Long idCandidature,
            @NotNull DecisionType decision, @NotBlank String motif) {
    }

    public record EvaluationRequest(@NotNull Long idCandidature, @NotNull TypeEvaluation typeEvaluation,
            Long idEntretien, Long idPassage, @DecimalMin("0.0") BigDecimal note,
            @Positive BigDecimal noteMax, @NotBlank String appreciation, @NotBlank String commentaire) {
    }
}
