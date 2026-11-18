package com.tovo.erp.evaluation.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "ecart_competence")
@Getter
@Setter
public class EcartCompetence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ecart")
    private Long id;
    @Column(name = "id_employe", nullable = false)
    private Long idEmploye;
    @Column(name = "id_competence", nullable = false)
    private Long idCompetence;
    @Column(name = "id_evaluation", nullable = false)
    private Long idEvaluation;
    @Column(name = "niveau_requis", nullable = false)
    private int niveauRequis;
    @Column(name = "niveau_acquis", nullable = false)
    private int niveauAcquis;
    @Column(nullable = false)
    private int ecart;
    @Column(name = "priorite_score", nullable = false, precision = 8, scale = 2)
    private BigDecimal prioriteScore;
    @Column(nullable = false, length = 10)
    private String priorite;
    @Column(nullable = false, length = 25)
    private String statut = "OUVERT";

    public enum Priorite {
        BASSE, MOYENNE, HAUTE
    }

    public enum Statut {
        OUVERT, FORMATION_PLANIFIEE, COMBLE, ACCEPTE
    }
}
