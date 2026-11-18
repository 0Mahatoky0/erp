package com.tovo.erp.carriere.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "campagne_augmentation")
@Getter
@Setter
@NoArgsConstructor
public class CampagneAugmentation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_campagne_augmentation")
    private Long id;

    @Column(name = "libelle", nullable = false, length = 150)
    private String libelle;

    @Column(name = "date_effet", nullable = false)
    private LocalDate dateEffet;

    @Column(name = "enveloppe_budgetaire", nullable = false, precision = 15, scale = 2)
    private BigDecimal enveloppeBudgetaire;

    @Column(name = "population", nullable = false, columnDefinition = "TEXT")
    private String population;

    @Column(name = "regle", nullable = false, columnDefinition = "TEXT")
    private String regle;

    @Column(name = "statut", nullable = false, length = 20)
    private String statut = "SIMULEE";

    @Column(name = "simulation", columnDefinition = "TEXT")
    private String simulation;
}
