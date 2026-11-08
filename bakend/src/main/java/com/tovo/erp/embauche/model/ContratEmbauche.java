package com.tovo.erp.embauche.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "contrat_embauche")
@Getter
@Setter
@NoArgsConstructor
public class ContratEmbauche {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_contrat")
    private Long idContrat;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_proposition_embauche", nullable = false, unique = true)
    private PropositionEmbauche proposition;

    @Column(name = "id_candidat", nullable = false)
    private Long idCandidat;

    @Column(name = "id_employe", unique = true)
    private Long idEmploye;

    @Column(name = "type_contrat", nullable = false)
    private String typeContrat;

    @PositiveOrZero
    private BigDecimal salaire;

    @Column(name = "lieu_travail")
    private String lieuTravail;

    @Column(name = "date_debut", nullable = false)
    private LocalDateTime dateDebut;

    @Column(name = "date_fin")
    private LocalDateTime dateFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContratStatut statut = ContratStatut.PROPOSE;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    private LocalDateTime dateReponse;
    private LocalDateTime dateValidation;

    @PrePersist
    void onCreate() {
        if (dateCreation == null) dateCreation = LocalDateTime.now();
    }
}
