package com.tovo.erp.paie.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bulletin_paie",
        uniqueConstraints = @UniqueConstraint(name = "uq_bulletin_periode_employe",
                columnNames = {"id_periode", "id_employe"}))
@Getter
@Setter
public class BulletinPaie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_bulletin")
    private Long id;
    @Column(name = "id_periode", nullable = false)
    private Long idPeriode;
    @Column(name = "id_employe", nullable = false)
    private Long idEmploye;
    @Column(name = "salaire_brut", nullable = false, precision = 15, scale = 2)
    private BigDecimal salaireBrut;
    @Column(name = "cotisations_salariales", nullable = false, precision = 15, scale = 2)
    private BigDecimal cotisationsSalariales;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal impot;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal retenues = BigDecimal.ZERO;
    @Column(name = "salaire_net", nullable = false, precision = 15, scale = 2)
    private BigDecimal salaireNet;
    @Column(name = "cotisations_patronales", nullable = false, precision = 15, scale = 2)
    private BigDecimal cotisationsPatronales;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutBulletinPaie statut = StatutBulletinPaie.BROUILLON;
    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();
    @Column(name = "date_validation")
    private LocalDateTime dateValidation;
}
