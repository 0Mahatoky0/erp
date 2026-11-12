package com.tovo.erp.conges.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "demande_conge")
@Getter
@Setter
public class DemandeConge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_demande_conge")
    private Long id;
    @Column(name = "id_employe", nullable = false)
    private Long idEmploye;
    @Column(name = "id_type_conge", nullable = false)
    private Long idTypeConge;
    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;
    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;
    @Column(name = "nombre_jours", nullable = false, precision = 6, scale = 2)
    private BigDecimal nombreJours;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String motif;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutDemandeConge statut = StatutDemandeConge.SOUMISE;
    @Column(name = "id_validateur")
    private Long idValidateur;
    @Column(name = "motif_decision", columnDefinition = "TEXT")
    private String motifDecision;
    @Column(name = "date_decision")
    private LocalDateTime dateDecision;
    @Column(name = "date_depot", nullable = false)
    private LocalDateTime dateDepot = LocalDateTime.now();
    @Enumerated(EnumType.STRING)
    @Column(name = "demi_journee_debut", length = 10)
    private DemiJournee demiJourneeDebut;
    @Enumerated(EnumType.STRING)
    @Column(name = "demi_journee_fin", length = 10)
    private DemiJournee demiJourneeFin;
    @Column(name = "id_validateur_manager")
    private Long idValidateurManager;
    @Column(name = "date_validation_manager")
    private LocalDateTime dateValidationManager;
    @Column(name = "id_validateur_rh")
    private Long idValidateurRh;
    @Column(name = "date_validation_rh")
    private LocalDateTime dateValidationRh;
    @Column(name = "id_remplacant")
    private Long idRemplacant;
    @Column(name = "justificatif_document_id")
    private Long justificatifDocumentId;
    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();
}
