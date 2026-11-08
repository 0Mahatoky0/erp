package com.tovo.erp.contrat.model;

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
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Affectation d'un salarie a un poste, un service et un manager (bloc 4).
 * <p>L'affectation est PLANIFIEE a l'acceptation puis devient ACTIVE a la prise
 * de poste. Une seule affectation principale peut etre active par salarie.
 */
@Entity
@Table(name = "affectation")
@Getter
@Setter
public class Affectation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_affectation")
    private Long id;

    @Column(name = "id_employe", nullable = false)
    private Long idEmploye;

    @Column(name = "id_contrat")
    private Long idContrat;

    @Column(name = "id_poste")
    private Long idPoste;

    @Column(name = "id_departement")
    private Long idDepartement;

    @Column(name = "id_service")
    private Long idService;

    @Column(name = "id_manager")
    private Long idManager;

    @Column(name = "lieu_travail", length = 150)
    private String lieuTravail;

    @Column(name = "horaires", columnDefinition = "TEXT")
    private String horaires;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Column(name = "taux_activite", nullable = false)
    private Integer tauxActivite = 100;

    @Column(name = "principale", nullable = false)
    private boolean principale = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "motif", nullable = false, length = 20)
    private MotifAffectation motif = MotifAffectation.EMBAUCHE;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private AffectationStatut statut = AffectationStatut.PLANIFIEE;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    @PrePersist
    void onCreate() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
    }
}
