package com.tovo.erp.recrutement.domain;

import jakarta.persistence.*; import jakarta.validation.constraints.*;
import lombok.Getter; import lombok.NoArgsConstructor; import lombok.Setter;
import java.math.BigDecimal; import java.time.LocalDateTime;

@Entity @Table(name = "evaluation_candidat") @Getter @Setter @NoArgsConstructor
public class EvaluationCandidat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long idEvaluation;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "id_candidature", nullable = false) private Candidature candidature;
    @NotNull @Column(name = "id_utilisateur_evaluateur", nullable = false) private Long idUtilisateurEvaluateur;
    @Enumerated(EnumType.STRING) @Column(name = "type_evaluation", nullable = false) private TypeEvaluation typeEvaluation;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "id_entretien") private Entretien entretien;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "id_passage") private PassageTest passage;
    @DecimalMin("0.0") private BigDecimal note;
    @Positive @Column(name = "note_max", nullable = false) private BigDecimal noteMax = BigDecimal.valueOf(20);
    @NotBlank @Column(nullable = false) private String appreciation;
    @NotBlank @Column(nullable = false, columnDefinition = "TEXT") private String commentaire;
    @Column(nullable = false, updatable = false) private LocalDateTime dateEvaluation;
    @PrePersist void onCreate() { dateEvaluation = LocalDateTime.now(); }

    @AssertTrue(message = "Le support de l'évaluation ne correspond pas à son type")
    boolean supportCoherent() {
        if (typeEvaluation == null) return true;
        return switch (typeEvaluation) {
            case DOSSIER -> entretien == null && passage == null;
            case ENTRETIEN -> entretien != null && passage == null;
            case TEST -> entretien == null && passage != null;
        };
    }

    @AssertTrue(message = "La note doit être comprise entre zéro et le barème")
    boolean noteCoherente() {
        return note == null || (noteMax != null && note.signum() >= 0 && note.compareTo(noteMax) <= 0);
    }
}
