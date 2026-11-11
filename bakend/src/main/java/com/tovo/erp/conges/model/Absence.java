package com.tovo.erp.conges.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "absence")
@Getter
@Setter
public class Absence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_absence")
    private Long id;
    @Column(name = "id_employe", nullable = false)
    private Long idEmploye;
    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;
    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;
    @Column(nullable = false, length = 100)
    private String motif;
    @Column(columnDefinition = "TEXT")
    private String justificatif;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutAbsence statut = StatutAbsence.A_JUSTIFIER;
    @Column(name = "id_validateur")
    private Long idValidateur;
    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();
}
