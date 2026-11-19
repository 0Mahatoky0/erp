package com.tovo.erp.rupture.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "dossier_rupture")
@Getter
@Setter
public class DossierRupture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String reference;

    @Column(name = "id_contrat", nullable = false)
    private Long idContrat;

    @Column(name = "id_employe", nullable = false)
    private Long idEmploye;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private MotifRupture motif;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InitiateurRupture initiateur;

    @Column(name = "date_notification", nullable = false)
    private LocalDate dateNotification;

    @Column(name = "date_preavis_debut")
    private LocalDate datePreavisDebut;

    @Column(name = "date_preavis_fin")
    private LocalDate datePreavisFin;

    @Column(name = "date_sortie")
    private LocalDate dateSortie;

    @Column(name = "preavis_jours", nullable = false)
    private int preavisJours = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "preavis_mode", nullable = false, length = 30)
    private ModePreavis preavisMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatutDossierRupture statut;

    @Column(name = "motif_autre", length = 255)
    private String motifAutre;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "date_decision")
    private LocalDate dateDecision;

    @Column(name = "id_procedure_disciplinaire")
    private Long idProcedureDisciplinaire;

    @Column(name = "id_initiateur")
    private Long idInitiateur;

    @Column(name = "id_validateur")
    private Long idValidateur;

    @Column(name = "date_validation")
    private LocalDateTime dateValidation;

    @Column(name = "date_archivage")
    private LocalDate dateArchivage;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_modification", nullable = false)
    private LocalDateTime dateModification;

    @PrePersist
    void onCreate() {
        LocalDateTime maintenant = LocalDateTime.now();
        if (dateCreation == null) {
            dateCreation = maintenant;
        }
        if (dateModification == null) {
            dateModification = maintenant;
        }
    }

    @PreUpdate
    void onUpdate() {
        dateModification = LocalDateTime.now();
    }
}
