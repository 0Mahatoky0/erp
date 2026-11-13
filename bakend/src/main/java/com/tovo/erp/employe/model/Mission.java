package com.tovo.erp.employe.model;

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

/**
 * Mission confiee a un salarie, suivie avec les indicateurs du cahier :
 * « Manao asa ve » (le travail est-il realise ?) et « Miassa tsara ve »
 * (est-il bien fait ?).
 */
@Entity
@Table(name = "mission")
@Getter
@Setter
public class Mission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mission")
    private Long id;

    @Column(name = "id_employe", nullable = false)
    private Long idEmploye;

    @Column(name = "reference", unique = true, length = 40)
    private String reference;

    @Column(name = "titre", nullable = false, length = 200)
    private String titre;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "lieu", length = 150)
    private String lieu;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin_prevue")
    private LocalDate dateFinPrevue;

    @Column(name = "date_fin_reelle")
    private LocalDate dateFinReelle;

    @Enumerated(EnumType.STRING)
    @Column(name = "priorite", nullable = false, length = 20)
    private PrioriteMission priorite = PrioriteMission.NORMALE;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private StatutMission statut = StatutMission.PLANIFIEE;

    @Column(name = "id_responsable")
    private Long idResponsable;

    @Column(name = "avancement", nullable = false)
    private Integer avancement = 0;

    @Column(name = "manao_asa")
    private Boolean manaoAsa;

    @Column(name = "miassa_tsara")
    private Boolean miassaTsara;

    @Column(name = "commentaire_suivi", columnDefinition = "TEXT")
    private String commentaireSuivi;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "date_modification", nullable = false)
    private LocalDateTime dateModification = LocalDateTime.now();

    @PrePersist
    void onCreate() {
        LocalDateTime maintenant = LocalDateTime.now();
        if (dateCreation == null) {
            dateCreation = maintenant;
        }
        dateModification = maintenant;
    }

    @PreUpdate
    void onUpdate() {
        dateModification = LocalDateTime.now();
    }
}
