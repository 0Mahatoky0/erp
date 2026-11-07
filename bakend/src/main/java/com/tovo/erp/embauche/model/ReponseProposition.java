package com.tovo.erp.embauche.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reponse_proposition")
@Getter
@Setter
@NoArgsConstructor
public class ReponseProposition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reponse")
    private Long idReponse;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_proposition_embauche", nullable = false)
    private PropositionEmbauche proposition;

    @Column(name = "id_candidat", nullable = false)
    private Long idCandidat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DecisionProposition decision;

    @PositiveOrZero
    @Column(name = "salaire_souhaite")
    private BigDecimal salaireSouhaite;

    @Size(max = 255)
    @Column(name = "lieu_souhaite")
    private String lieuSouhaite;

    @Size(max = 20)
    @Column(name = "type_contrat_souhaite")
    private String typeContratSouhaite;

    @Column(name = "date_debut_souhaitee")
    private LocalDateTime dateDebutSouhaitee;

    @Column(name = "date_fin_souhaitee")
    private LocalDateTime dateFinSouhaitee;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dateReponse;

    @PrePersist
    void onCreate() {
        dateReponse = LocalDateTime.now();
    }
}
