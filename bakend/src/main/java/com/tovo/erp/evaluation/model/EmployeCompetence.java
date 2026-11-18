package com.tovo.erp.evaluation.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "employe_competence")
@Getter
@Setter
public class EmployeCompetence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_employe_competence")
    private Long id;
    @Column(name = "id_employe", nullable = false)
    private Long idEmploye;
    @Column(name = "id_competence", nullable = false)
    private Long idCompetence;
    @Column(name = "niveau_acquis", nullable = false)
    private int niveauAcquis;
    @Column(name = "date_evaluation", nullable = false)
    private LocalDate dateEvaluation;
    @Column(name = "date_validite")
    private LocalDate dateValidite;
    @Column(nullable = false, length = 40)
    private String source;
    @Column(name = "evaluation_id")
    private Long evaluationId;
    @Column(name = "formation_id")
    private Long formationId;
    @Column(name = "valide_par_id")
    private Long valideParId;

    public enum Source {
        EVALUATION, FORMATION, CERTIFICATION, DECLARATIF_VALIDE
    }
}
