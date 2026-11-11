package com.tovo.erp.paie.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
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
}
