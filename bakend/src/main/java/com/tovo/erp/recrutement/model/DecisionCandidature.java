package com.tovo.erp.recrutement.domain;

import jakarta.persistence.*; import jakarta.validation.constraints.*;
import lombok.Getter; import lombok.NoArgsConstructor; import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Table(name = "decision_candidature") @Getter @Setter @NoArgsConstructor
public class DecisionCandidature {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long idDecision;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "id_candidature", nullable = false) private Candidature candidature;
    @NotNull @Column(name = "id_utilisateur", nullable = false) private Long idUtilisateur;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private DecisionType decision;
    @NotBlank @Column(nullable = false, columnDefinition = "TEXT") private String motif;
    @Column(nullable = false, updatable = false) private LocalDateTime dateDecision;
    @PrePersist void onCreate() { dateDecision = LocalDateTime.now(); }
}
