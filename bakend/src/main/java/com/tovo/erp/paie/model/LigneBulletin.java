package com.tovo.erp.paie.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "ligne_bulletin")
@Getter
@Setter
public class LigneBulletin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ligne")
    private Long id;
    @Column(name = "id_bulletin", nullable = false)
    private Long idBulletin;
    @Column(name = "code_rubrique", nullable = false, length = 40)
    private String codeRubrique;
    @Column(nullable = false, length = 150)
    private String libelle;
    @Column(nullable = false)
    private int ordre;
    @Column(precision = 15, scale = 2)
    private BigDecimal base;
    @Column(precision = 8, scale = 5)
    private BigDecimal taux;
    @Column(precision = 12, scale = 2)
    private BigDecimal quantite;
    @Column(name = "montant_salarial", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantSalarial = BigDecimal.ZERO;
    @Column(name = "montant_patronal", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantPatronal = BigDecimal.ZERO;
    @Column(name = "formule_appliquee")
    private String formuleAppliquee;
    @Column(name = "version_regle", length = 50)
    private String versionRegle;
    @Column
    private String sources;
}
