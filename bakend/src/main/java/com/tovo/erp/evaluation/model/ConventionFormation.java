package com.tovo.erp.evaluation.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "convention_formation")
@Getter
@Setter
public class ConventionFormation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_convention")
    private Long id;
    @Column(name = "id_inscription", nullable = false, unique = true)
    private Long idInscription;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal cout;
    @Column(name = "duree_engagement_mois", nullable = false)
    private int dureeEngagementMois;
    @Column(name = "clause_dedit", nullable = false, columnDefinition = "TEXT")
    private String clauseDedit;
}
