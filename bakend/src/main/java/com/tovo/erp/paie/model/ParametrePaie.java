package com.tovo.erp.paie.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "parametre_paie")
@Getter
@Setter
public class ParametrePaie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_parametre")
    private Long id;
    @Column(name = "taux_cotisations_salariales", precision = 8, scale = 5)
    private BigDecimal tauxCotisationsSalariales;
    @Column(name = "taux_cotisations_patronales", precision = 8, scale = 5)
    private BigDecimal tauxCotisationsPatronales;
    @Column(name = "taux_irsa", precision = 8, scale = 5)
    private BigDecimal tauxIrsa;
    @Column(nullable = false)
    private boolean actif = true;
    @Column(name = "date_modification", nullable = false)
    private LocalDateTime dateModification = LocalDateTime.now();

    @Column(precision = 15, scale = 2)
    private BigDecimal smig;
    @Column(name = "plafond_cnaps_smig", precision = 6, scale = 2)
    private BigDecimal plafondCnapsSmig;
    @Column(name = "taux_cnaps_salarial", precision = 8, scale = 5)
    private BigDecimal tauxCnapsSalarial;
    @Column(name = "taux_cnaps_patronal", precision = 8, scale = 5)
    private BigDecimal tauxCnapsPatronal;
    @Column(name = "taux_ostie_salarial", precision = 8, scale = 5)
    private BigDecimal tauxOstieSalarial;
    @Column(name = "taux_ostie_patronal", precision = 8, scale = 5)
    private BigDecimal tauxOstiePatronal;
    @Column(name = "taux_fmfp_patronal", precision = 8, scale = 5)
    private BigDecimal tauxFmfpPatronal;
    @Column(name = "majoration_hs30", precision = 8, scale = 5)
    private BigDecimal majorationHs30;
    @Column(name = "majoration_hs50", precision = 8, scale = 5)
    private BigDecimal majorationHs50;
    @Column(name = "majoration_nuit", precision = 8, scale = 5)
    private BigDecimal majorationNuit;
    @Column(name = "majoration_dimanche", precision = 8, scale = 5)
    private BigDecimal majorationDimanche;
    @Column(name = "majoration_ferie", precision = 8, scale = 5)
    private BigDecimal majorationFerie;
    @Column(name = "minimum_irsa", precision = 15, scale = 2)
    private BigDecimal minimumIrsa;
    @Column(name = "reduction_charge", precision = 15, scale = 2)
    private BigDecimal reductionCharge;
    @Column(name = "quotient_retenue", precision = 8, scale = 5)
    private BigDecimal quotientRetenue;
    @Column(name = "date_effet")
    private LocalDate dateEffet;
}
