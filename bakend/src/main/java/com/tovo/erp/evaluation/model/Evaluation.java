package com.tovo.erp.evaluation.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "evaluation", uniqueConstraints = @UniqueConstraint(name = "uq_evaluation_employe_campagne_version",
        columnNames = {"id_employe", "id_campagne", "version"}))
@Getter
@Setter
public class Evaluation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evaluation")
    private Long id;
    @Column(name = "id_employe", nullable = false)
    private Long idEmploye;
    @Column(name = "id_campagne", nullable = false)
    private Long idCampagne;
    @Column(name = "id_evaluateur", nullable = false)
    private Long idEvaluateur;
    @Column(name = "id_valideur_n2")
    private Long idValideurN2;
    @Column(name = "auto_evaluation", columnDefinition = "TEXT")
    private String autoEvaluation;
    @Column(name = "evaluation_manager", columnDefinition = "TEXT")
    private String evaluationManager;
    @Column(name = "note_objectifs", precision = 5, scale = 2)
    private BigDecimal noteObjectifs;
    @Column(name = "note_competences", precision = 5, scale = 2)
    private BigDecimal noteCompetences;
    @Column(name = "note_comportement", precision = 5, scale = 2)
    private BigDecimal noteComportement;
    @Column(name = "note_globale", precision = 5, scale = 2)
    private BigDecimal noteGlobale;
    @Column(length = 30)
    private String appreciation;
    @Column(name = "points_forts", columnDefinition = "TEXT")
    private String pointsForts;
    @Column(name = "axes_progres", columnDefinition = "TEXT")
    private String axesProgres;
    @Column(name = "date_entretien")
    private LocalDateTime dateEntretien;
    @Column(name = "commentaire_salarie", columnDefinition = "TEXT")
    private String commentaireSalarie;
    @Column(name = "date_signature_salarie")
    private LocalDateTime dateSignatureSalarie;
    @Column(nullable = false, length = 30)
    private String statut = "BROUILLON";
    @Column(nullable = false)
    private int version = 1;
    @Column(name = "motif_contestation", columnDefinition = "TEXT")
    private String motifContestation;
    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();
    @Column(name = "date_cloture")
    private LocalDateTime dateCloture;

    public enum Statut {
        BROUILLON, AUTO_EVALUATION, EVALUATION_MANAGER, ENTRETIEN, VALIDEE_N2, SIGNEE, CLOTUREE, CONTESTEE
    }
}
