package com.tovo.erp.rupture.model;

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

import java.math.BigDecimal;

@Entity
@Table(name = "ligne_solde_rupture")
@Getter
@Setter
public class LigneSoldeRupture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_solde", nullable = false)
    private Long idSolde;

    @Column(length = 40)
    private String code;

    @Column(length = 150)
    private String libelle;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CategorieLigneSolde categorie;

    @Column(precision = 18, scale = 2)
    private BigDecimal base;

    @Column(precision = 18, scale = 2)
    private BigDecimal quantite;

    @Column(precision = 18, scale = 2)
    private BigDecimal taux;

    @Column(precision = 18, scale = 2)
    private BigDecimal montant;

    @Column(length = 200)
    private String source;
}
