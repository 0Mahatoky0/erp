package com.tovo.erp.temps.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "periode_temps_close")
@Getter
@Setter
public class PeriodeTempsClose {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_periode_close")
    private Long id;
    @Column(nullable = false)
    private int annee;
    @Column(nullable = false)
    private int mois;
    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;
    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;
    @Column(name = "id_utilisateur")
    private Long idUtilisateur;
    @Column(name = "date_cloture", nullable = false)
    private LocalDateTime dateCloture = LocalDateTime.now();
}
