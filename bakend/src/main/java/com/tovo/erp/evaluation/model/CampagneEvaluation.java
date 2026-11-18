package com.tovo.erp.evaluation.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "campagne_evaluation")
@Getter
@Setter
public class CampagneEvaluation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_campagne")
    private Long id;
    @Column(nullable = false, length = 150)
    private String libelle;
    @Column(nullable = false, length = 30)
    private String type;
    @Column(name = "periode_debut", nullable = false)
    private LocalDate periodeDebut;
    @Column(name = "periode_fin", nullable = false)
    private LocalDate periodeFin;
    @Column(name = "date_ouverture", nullable = false)
    private LocalDate dateOuverture;
    @Column(name = "date_cloture", nullable = false)
    private LocalDate dateCloture;
    @Column(name = "grille_json", columnDefinition = "TEXT")
    private String grilleJson;
    @Column(name = "ponderations_json", columnDefinition = "TEXT")
    private String ponderationsJson;
    @Column(columnDefinition = "TEXT")
    private String population;
    @Column(nullable = false, length = 30)
    private String statut = "PREPAREE";

    public enum Type {
        ANNUELLE, MI_PARCOURS, FIN_ESSAI
    }

    public enum Statut {
        PREPAREE, OUVERTE, EN_ENTRETIENS, CLOTUREE
    }
}
