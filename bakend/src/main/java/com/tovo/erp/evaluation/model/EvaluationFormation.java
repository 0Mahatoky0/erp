package com.tovo.erp.evaluation.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "evaluation_formation")
@Getter
@Setter
public class EvaluationFormation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evaluation_formation")
    private Long id;
    @Column(name = "id_inscription", nullable = false)
    private Long idInscription;
    @Column(nullable = false, length = 15)
    private String type;
    @Column
    private Integer satisfaction;
    @Column(precision = 5, scale = 2)
    private BigDecimal acquis;
    @Column(precision = 5, scale = 2)
    private BigDecimal transfert;
    @Column(columnDefinition = "TEXT")
    private String commentaire;
    @Column(name = "date_evaluation", nullable = false)
    private LocalDateTime dateEvaluation = LocalDateTime.now();

    public enum Type {
        A_CHAUD, A_FROID_90J
    }
}
