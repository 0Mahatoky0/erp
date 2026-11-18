package com.tovo.erp.evaluation.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "poste_competence", uniqueConstraints = @UniqueConstraint(name = "uq_poste_competence",
        columnNames = {"id_poste", "id_competence"}))
@Getter
@Setter
public class PosteCompetence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_poste_competence")
    private Long id;
    @Column(name = "id_poste", nullable = false)
    private Long idPoste;
    @Column(name = "id_competence", nullable = false)
    private Long idCompetence;
    @Column(name = "niveau_requis", nullable = false)
    private int niveauRequis;
    @Column(nullable = false)
    private int poids;
}
