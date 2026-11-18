package com.tovo.erp.evaluation.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "plan_formation", uniqueConstraints = @UniqueConstraint(name = "uq_plan_formation",
        columnNames = {"annee", "id_departement"}))
@Getter
@Setter
public class PlanFormation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_plan")
    private Long id;
    @Column(nullable = false)
    private int annee;
    @Column(name = "id_departement", nullable = false)
    private Long idDepartement;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal budget;
}
