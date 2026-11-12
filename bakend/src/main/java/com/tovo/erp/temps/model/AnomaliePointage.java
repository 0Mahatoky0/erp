package com.tovo.erp.temps.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "anomalie_pointage")
@Getter
@Setter
public class AnomaliePointage {

    public enum Statut {
        OUVERTE, JUSTIFIEE, CORRIGEE, REJETEE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_anomalie")
    private Long id;

    @Column(name = "id_jour_travaille")
    private Long idJourTravaille;

    @Column(name = "id_employe", nullable = false)
    private Long idEmploye;

    @Column(name = "date_anomalie", nullable = false)
    private LocalDate dateAnomalie;

    @Column(nullable = false, length = 40)
    private String type;

    @Column(nullable = false, length = 20)
    private String gravite = "MOYENNE";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Statut statut = Statut.OUVERTE;

    @Column(length = 500)
    private String resolution;

    @Column(name = "id_resolu_par")
    private Long idResoluPar;

    @Column(name = "date_resolution")
    private LocalDateTime dateResolution;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();
}
