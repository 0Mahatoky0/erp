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
import java.time.LocalDate;

@Entity
@Table(name = "materiel_rupture")
@Getter
@Setter
public class MaterielRupture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_dossier", nullable = false)
    private Long idDossier;

    @Column(length = 150)
    private String libelle;

    @Column(length = 80)
    private String type;

    @Column(name = "numero_serie", length = 100)
    private String numeroSerie;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private StatutMaterielRupture statut;

    @Column(name = "date_restitution")
    private LocalDate dateRestitution;

    @Column(name = "montant_retenue", precision = 18, scale = 2)
    private BigDecimal montantRetenue;
}
