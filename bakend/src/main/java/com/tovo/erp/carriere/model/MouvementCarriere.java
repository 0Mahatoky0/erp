package com.tovo.erp.carriere.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "mouvement_carriere")
@Getter
@Setter
@NoArgsConstructor
public class MouvementCarriere {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mouvement")
    private Long id;

    @Column(name = "reference", nullable = false, length = 30, unique = true)
    private String reference;

    @Column(name = "id_employe", nullable = false)
    private Long idEmploye;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private TypeMouvementCarriere type;

    @Column(name = "initiateur", nullable = false, length = 30)
    private String initiateur;

    @Column(name = "motif", nullable = false, length = 100)
    private String motif;

    @Column(name = "justification", nullable = false, columnDefinition = "TEXT")
    private String justification;

    @Column(name = "date_effet_souhaitee", nullable = false)
    private LocalDate dateEffetSouhaitee;

    @Column(name = "date_effet")
    private LocalDate dateEffet;

    @Column(name = "id_evaluation")
    private Long idEvaluation;

    @Column(name = "id_avenant")
    private Long idAvenant;

    @Column(name = "documents", columnDefinition = "TEXT")
    private String documents;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 30)
    private StatutMouvementCarriere statut = StatutMouvementCarriere.BROUILLON;

    @Column(name = "version", nullable = false)
    private int version = 1;

    @Column(name = "id_campagne")
    private Long idCampagne;

    @Column(name = "id_demandeur", nullable = false)
    private Long idDemandeur;

    @Column(name = "id_validateur")
    private Long idValidateur;

    @Column(name = "motif_refus", columnDefinition = "TEXT")
    private String motifRefus;

    @Column(name = "derogation_direction", nullable = false)
    private boolean derogationDirection;

    @Column(name = "motif_derogation", columnDefinition = "TEXT")
    private String motifDerogation;

    @Column(name = "conditions_verifiees", columnDefinition = "TEXT")
    private String conditionsVerifiees;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_decision")
    private LocalDateTime dateDecision;

    @Column(name = "date_application")
    private LocalDateTime dateApplication;

    @PrePersist
    void onCreate() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
    }
}
