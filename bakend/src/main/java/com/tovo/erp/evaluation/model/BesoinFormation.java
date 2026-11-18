package com.tovo.erp.evaluation.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "besoin_formation")
@Getter
@Setter
public class BesoinFormation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_besoin")
    private Long id;
    @Column(name = "id_employe", nullable = false)
    private Long idEmploye;
    @Column(name = "id_competence", nullable = false)
    private Long idCompetence;
    @Column(name = "niveau_cible", nullable = false)
    private int niveauCible;
    @Column(nullable = false, length = 25)
    private String origine;
    @Column(nullable = false, length = 10)
    private String priorite;
    @Column(name = "date_cible")
    private LocalDate dateCible;
    @Column(name = "budget_estime", precision = 15, scale = 2)
    private BigDecimal budgetEstime;
    @Column(nullable = false, length = 20)
    private String statut = "IDENTIFIE";
    @Column(name = "motif_refus", columnDefinition = "TEXT")
    private String motifRefus;
    @Column(name = "source_evaluation_id")
    private Long sourceEvaluationId;
    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    public enum Origine {
        ECART, EVALUATION, DEMANDE_SALARIE, DEMANDE_MANAGER, OBLIGATION_LEGALE, EVOLUTION_POSTE
    }

    public enum Statut {
        IDENTIFIE, VALIDE, PLANIFIE, REALISE, ABANDONNE
    }
}
