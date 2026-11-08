package com.tovo.erp.recrutement.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter; import lombok.NoArgsConstructor; import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Table(name = "candidat") @Getter @Setter @NoArgsConstructor
public class Candidat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long idCandidat;
    @NotBlank @Size(max = 100) @Column(nullable = false) private String nom;
    @NotBlank @Size(max = 100) @Column(nullable = false) private String prenom;
    @NotBlank @Email @Size(max = 255) @Column(nullable = false, unique = true) private String email;
    @Size(max = 30) private String telephone;
    @Column(columnDefinition = "TEXT") private String adresse;
    @Size(max = 50) private String cin;
    private LocalDate dateNaissance;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private StatutCandidat statut = StatutCandidat.CANDIDAT;
    @Column(nullable = false, updatable = false) private LocalDateTime dateCreation;
    @Column(nullable = false) private LocalDateTime dateModification;
    @PrePersist void onCreate() { dateCreation = LocalDateTime.now(); dateModification = dateCreation; }
    @PreUpdate void onUpdate() { dateModification = LocalDateTime.now(); }
}
