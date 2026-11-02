package com.tovo.erp.recrutement.model;

import jakarta.persistence.*; import jakarta.validation.constraints.*;
import lombok.Getter; import lombok.NoArgsConstructor; import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Table(name = "entretien") @Getter @Setter @NoArgsConstructor
public class Entretien {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long idEntretien;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "id_candidature", nullable = false) private Candidature candidature;
    @Column(name = "id_utilisateur_organisateur", nullable = false) @NotNull private Long idUtilisateurOrganisateur;
    @Enumerated(EnumType.STRING) @Column(name = "type_entretien", nullable = false) @NotNull private TypeEntretien typeEntretien;
    @NotNull @Column(name = "date_debut", nullable = false) private LocalDateTime dateDebut;
    @NotNull @Column(name = "date_fin", nullable = false) private LocalDateTime dateFin;
    @Size(max = 255) private String lieu;
    private String lienVisio;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private EntretienStatut statut = EntretienStatut.PLANIFIE;
    @Column(columnDefinition = "TEXT") private String commentaire;
    @Column(nullable = false, updatable = false) private LocalDateTime dateCreation;
    @Column(nullable = false) private LocalDateTime dateModification;
    @PrePersist void onCreate() { dateCreation = LocalDateTime.now(); dateModification = dateCreation; }
    @PreUpdate void onUpdate() { dateModification = LocalDateTime.now(); }

    @AssertTrue(message = "La fin de l'entretien doit être postérieure à son début")
    boolean datesCoherentes() {
        return dateDebut == null || dateFin == null || dateFin.isAfter(dateDebut);
    }
}
