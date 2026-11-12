package com.tovo.erp.temps.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "heure_supplementaire")
@Getter
@Setter
public class HeureSupplementaire {

    public enum Statut {
        DETECTEE, DEMANDEE, VALIDEE, REFUSEE, PAYEE, RECUPEREE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_heure_sup")
    private Long id;

    @Column(name = "id_employe", nullable = false)
    private Long idEmploye;

    @Column(name = "semaine_debut", nullable = false)
    private LocalDate semaineDebut;

    @Column(name = "minutes_total", nullable = false)
    private int minutesTotal;

    @Column(name = "minutes_hs_30", nullable = false)
    private int minutesHs30;

    @Column(name = "minutes_hs_50", nullable = false)
    private int minutesHs50;

    @Column(name = "minutes_nuit", nullable = false)
    private int minutesNuit;

    @Column(name = "minutes_dimanche", nullable = false)
    private int minutesDimanche;

    @Column(name = "minutes_ferie", nullable = false)
    private int minutesFerie;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Statut statut = Statut.DETECTEE;

    @Column(name = "alerte_plafond", nullable = false)
    private boolean alertePlafond;

    @Column(name = "date_calcul", nullable = false)
    private LocalDateTime dateCalcul = LocalDateTime.now();
}
