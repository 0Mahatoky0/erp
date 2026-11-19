package com.tovo.erp.paie.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bulletin_paie",
        uniqueConstraints = @UniqueConstraint(name = "uq_bulletin_periode_employe_type",
                columnNames = {"id_periode", "id_employe", "type"}))
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
    @Column(nullable = false, length = 25)
    private StatutBulletinPaie statut = StatutBulletinPaie.BROUILLON;
    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();
    @Column(name = "date_validation")
    private LocalDateTime dateValidation;

    @Column(name = "jour_payes", nullable = false)
    private int jourPayes = 30;
    @Column(name = "salaire_base_periode", nullable = false, precision = 15, scale = 2)
    private BigDecimal salaireBasePeriode = BigDecimal.ZERO;
    @Column(name = "base_imposable", nullable = false, precision = 15, scale = 2)
    private BigDecimal baseImposable = BigDecimal.ZERO;
    @Column(name = "prime_exceptionnelle", nullable = false, precision = 15, scale = 2)
    private BigDecimal primeExceptionnelle = BigDecimal.ZERO;
    @Column(name = "avance", nullable = false, precision = 15, scale = 2)
    private BigDecimal avance = BigDecimal.ZERO;
    @Column(name = "retenue_manuelle", nullable = false, precision = 15, scale = 2)
    private BigDecimal retenueManuelle = BigDecimal.ZERO;
    @Column(name = "personnes_charge", nullable = false)
    private int personnesCharge;
    @Column(name = "motif_ajustement")
    private String motifAjustement;
    @Column(length = 30)
    private String numero;
    @Column(nullable = false, length = 25)
    private String type = "NORMAL";
    @Column(name = "id_contrat")
    private Long idContrat;
}
