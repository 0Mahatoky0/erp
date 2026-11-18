package com.tovo.erp.evaluation.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "objectif")
@Getter
@Setter
public class Objectif {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_objectif")
    private Long id;
    @Column(name = "id_employe", nullable = false)
    private Long idEmploye;
    @Column(name = "id_campagne", nullable = false)
    private Long idCampagne;
    @Column(name = "parent_objectif_id")
    private Long parentObjectifId;
    @Column(nullable = false, length = 200)
    private String libelle;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(nullable = false, length = 150)
    private String indicateur;
    @Column(length = 30)
    private String unite;
    @Column(name = "valeur_cible", nullable = false, precision = 15, scale = 2)
    private BigDecimal valeurCible;
    @Column(name = "valeur_realisee", precision = 15, scale = 2)
    private BigDecimal valeurRealisee;
    @Column(nullable = false, length = 25)
    private String sens = "PLUS_EST_MIEUX";
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal poids;
    @Column(name = "date_echeance", nullable = false)
    private LocalDate dateEcheance;
    @Column(nullable = false, length = 25)
    private String statut = "DEFINI";
    @Column(name = "taux_atteinte", precision = 8, scale = 4)
    private BigDecimal tauxAtteinte;
    @Column(nullable = false)
    private int version = 1;
    @Column(name = "motif_avenant", columnDefinition = "TEXT")
    private String motifAvenant;
    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    public enum Statut {
        DEFINI, VALIDE, EN_COURS, ATTEINT, PARTIEL, NON_ATTEINT, ABANDONNE
    }

    public enum Sens {
        PLUS_EST_MIEUX, MOINS_EST_MIEUX
    }
}
