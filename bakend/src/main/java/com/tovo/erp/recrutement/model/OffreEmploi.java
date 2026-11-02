package com.tovo.erp.recrutement.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "offre_emploi")
@Getter @Setter @NoArgsConstructor
public class OffreEmploi {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idOffre;
    @Column(name = "id_demande_poste", nullable = false) @NotNull
    private Long idDemandePoste;
    @Column(name = "id_utilisateur_createur", nullable = false)
    private Long idUtilisateurCreateur;
    @NotBlank @Size(max = 50) @Column(nullable = false, unique = true)
    private String reference;
    @NotBlank @Size(max = 150) @Column(nullable = false)
    private String titre;
    @NotBlank @Column(nullable = false, columnDefinition = "TEXT")
    private String description;
    @NotBlank @Column(name = "profil_recherche", nullable = false, columnDefinition = "TEXT")
    private String profilRecherche;
    @Size(max = 255) private String lieuTravail;
    @Size(max = 50) private String typeContrat;
    @NotNull @Positive @Column(nullable = false) private Integer effectifRecherche;
    @PositiveOrZero private BigDecimal salaireMin;
    @PositiveOrZero private BigDecimal salaireMax;
    @NotBlank @Column(name = "mode_selection", nullable = false, columnDefinition = "TEXT")
    private String modeSelection;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private OffreStatut statut = OffreStatut.BROUILLON;
    private LocalDateTime datePublication;
    private LocalDateTime dateLimiteCandidature;
    private LocalDateTime dateCloture;
    @Column(nullable = false, updatable = false) private LocalDateTime dateCreation;
    @Column(nullable = false) private LocalDateTime dateModification;

    @PrePersist
    void onCreate() {
        dateCreation = LocalDateTime.now();
        dateModification = dateCreation;
    }
    @PreUpdate
    void onUpdate() { dateModification = LocalDateTime.now(); }

    @AssertTrue(message = "Le salaire maximum doit être supérieur ou égal au salaire minimum")
    boolean salairesCoherents() {
        return salaireMin == null || salaireMax == null || salaireMax.compareTo(salaireMin) >= 0;
    }

    @AssertTrue(message = "La date limite doit être postérieure à la publication")
    boolean dateLimiteCoherente() {
        return dateLimiteCandidature == null || datePublication == null
                || !dateLimiteCandidature.isBefore(datePublication);
    }

    @AssertTrue(message = "La date de clôture doit être postérieure à la publication")
    boolean dateClotureCoherente() {
        return dateCloture == null || (datePublication != null && !dateCloture.isBefore(datePublication));
    }
}
