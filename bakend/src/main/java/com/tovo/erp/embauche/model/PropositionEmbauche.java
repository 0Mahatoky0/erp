package com.tovo.erp.embauche.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "proposition_embauche")
@Getter
@Setter
@NoArgsConstructor
public class PropositionEmbauche {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_proposition_embauche")
    private Long idPropositionEmbauche;

    @Column(name = "id_candidature", nullable = false)
    private Long idCandidature;

    @Column(name = "id_candidat", nullable = false)
    private Long idCandidat;

    @Column(name = "id_offre", nullable = false)
    private Long idOffre;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_proposition", nullable = false)
    private PropositionStatut statut = PropositionStatut.ENVOYEE;

    @NotBlank
    @Size(max = 20)
    @Column(name = "type_contrat")
    private String typeContrat;

    @PositiveOrZero
    @Column(name = "salaire_propose")
    private BigDecimal salairePropose;

    @Size(max = 255)
    @Column(name = "lieu_travail")
    private String lieuTravail;

    @NotNull
    @Column(name = "date_debut")
    private LocalDateTime dateDebut;

    @Column(name = "date_fin")
    private LocalDateTime dateFin;

    @Column(name = "date_proposition", nullable = false)
    private LocalDateTime dateProposition;

    @PrePersist
    void onCreate() {
        if (dateProposition == null) dateProposition = LocalDateTime.now();
    }

    @AssertTrue(message = "La date de fin est obligatoire pour un CDD")
    boolean cddAvecDateFin() {
        return !"CDD".equals(typeContrat) || dateFin != null;
    }

    @AssertTrue(message = "La date de fin doit être postérieure à la date de début")
    boolean datesCoherentes() {
        return dateFin == null || (dateDebut != null && !dateFin.isBefore(dateDebut));
    }
}
