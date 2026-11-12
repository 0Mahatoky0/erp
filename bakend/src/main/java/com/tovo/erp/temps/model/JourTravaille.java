package com.tovo.erp.temps.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "jour_travaille")
@Getter
@Setter
public class JourTravaille {

    public enum Statut {
        CONFORME, ANOMALIE, ABSENT, CONGE, FERIE, A_VALIDER, VALIDE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_jour_travaille")
    private Long id;

    @Column(name = "id_employe", nullable = false)
    private Long idEmploye;

    @Column(name = "date_jour", nullable = false)
    private LocalDate dateJour;

    @Column(name = "id_planning")
    private Long idPlanning;

    @Column(name = "heure_entree")
    private LocalTime heureEntree;

    @Column(name = "heure_sortie")
    private LocalTime heureSortie;

    @Column(name = "minutes_presence", nullable = false)
    private int minutesPresence;

    @Column(name = "minutes_pause", nullable = false)
    private int minutesPause;

    @Column(name = "minutes_travaillees", nullable = false)
    private int minutesTravaillees;

    @Column(name = "minutes_theoriques", nullable = false)
    private int minutesTheoriques;

    @Column(name = "minutes_retard", nullable = false)
    private int minutesRetard;

    @Column(name = "minutes_depart_anticipe", nullable = false)
    private int minutesDepartAnticipe;

    @Column(name = "minutes_hs", nullable = false)
    private int minutesHs;

    @Column(name = "minutes_nuit", nullable = false)
    private int minutesNuit;

    @Column(name = "minutes_dimanche", nullable = false)
    private int minutesDimanche;

    @Column(name = "minutes_ferie", nullable = false)
    private int minutesFerie;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Statut statut = Statut.A_VALIDER;

    @Column(name = "date_calcul", nullable = false)
    private LocalDateTime dateCalcul = LocalDateTime.now();
}
