package com.tovo.erp.temps.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "planning_travail")
@Getter
@Setter
public class PlanningTravail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_planning")
    private Long id;

    @Column(name = "id_employe", nullable = false)
    private Long idEmploye;

    @Column(name = "jours_travailles", nullable = false, length = 100)
    private String joursTravailles;

    @Column(name = "heure_debut", nullable = false)
    private LocalTime heureDebut;

    @Column(name = "heure_fin", nullable = false)
    private LocalTime heureFin;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Column(name = "pause_minutes", nullable = false)
    private int pauseMinutes = 60;

    @Column(name = "tolerance_minutes", nullable = false)
    private int toleranceMinutes = 5;

    @Column(name = "origine", nullable = false, length = 20)
    private String origine = "MANUEL";

    @Column(name = "statut", nullable = false, length = 20)
    private String statut = "PLANIFIE";

    @Column(name = "publie_le")
    private LocalDateTime publieLe;

    @Column(name = "motif_modification", length = 500)
    private String motifModification;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();
}
