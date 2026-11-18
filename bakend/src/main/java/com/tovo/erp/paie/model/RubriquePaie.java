package com.tovo.erp.paie.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "rubrique_paie")
@Getter
@Setter
public class RubriquePaie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rubrique")
    private Long id;
    @Column(nullable = false, unique = true, length = 40)
    private String code;
    @Column(nullable = false, length = 150)
    private String libelle;
    @Column(nullable = false, length = 30)
    private String type;
    @Column(name = "mode_calcul", nullable = false, length = 30)
    private String modeCalcul = "QUANTITE_X_TAUX";
    @Column(length = 255)
    private String formule;
    @Column(length = 40)
    private String base;
    @Column(name = "taux_salarial", nullable = false, precision = 8, scale = 5)
    private BigDecimal tauxSalarial = BigDecimal.ZERO;
    @Column(name = "taux_patronal", nullable = false, precision = 8, scale = 5)
    private BigDecimal tauxPatronal = BigDecimal.ZERO;
    @Column(precision = 15, scale = 2)
    private BigDecimal plafond;
    @Column(nullable = false)
    private boolean imposable = true;
    @Column(name = "soumise_cotisations", nullable = false)
    private boolean soumiseCotisations = true;
    @Column(nullable = false)
    private int ordre;
    @Column(name = "date_effet", nullable = false)
    private LocalDate dateEffet;
    @Column(name = "date_fin")
    private LocalDate dateFin;
}
