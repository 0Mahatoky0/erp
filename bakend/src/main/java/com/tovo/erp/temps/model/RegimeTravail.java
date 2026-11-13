package com.tovo.erp.temps.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Modele d'horaire applicable a un groupe de salaries (bloc 6, RG-B6-01).
 * Les plages detaillees d'une journee type sont portees par {@link HoraireTravail}.
 */
@Entity
@Table(name = "regime_travail")
@Getter
@Setter
public class RegimeTravail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_regime")
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 40)
    private String code;

    @Column(name = "libelle", nullable = false, length = 120)
    private String libelle;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private TypeRegimeTravail type = TypeRegimeTravail.HORAIRE_FIXE;

    @Column(name = "heures_hebdo", nullable = false)
    private int heuresHebdo = 40;

    @Column(name = "jours_travailles", nullable = false, length = 100)
    private String joursTravailles = "LUNDI,MARDI,MERCREDI,JEUDI,VENDREDI";

    @Column(name = "tolerance_retard_min", nullable = false)
    private int toleranceRetardMin = 10;

    @Column(name = "tolerance_depart_min", nullable = false)
    private int toleranceDepartMin = 10;

    @Column(name = "pause_minutes", nullable = false)
    private int pauseMinutes = 60;

    @Column(name = "pause_payee", nullable = false)
    private boolean pausePayee;

    @Column(name = "decompte_heures_sup", nullable = false)
    private boolean decompteHeuresSup = true;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();
}
