package com.tovo.erp.evaluation.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "formation")
@Getter
@Setter
public class Formation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_formation")
    private Long id;
    @Column(nullable = false, unique = true, length = 50)
    private String code;
    @Column(nullable = false, length = 200)
    private String titre;
    @Column(nullable = false, length = 20)
    private String type;
    @Column(length = 150)
    private String organisme;
    @Column(name = "duree_h", nullable = false, precision = 8, scale = 2)
    private BigDecimal dureeH;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal cout;
    @Column(nullable = false)
    private boolean certifiante;
    @Column(name = "competences_visees", columnDefinition = "TEXT")
    private String competencesVisees;
    @Column(name = "seuil_dedit", precision = 15, scale = 2)
    private BigDecimal seuilDedit;

    public enum Type {
        INTERNE, EXTERNE, E_LEARNING
    }
}
