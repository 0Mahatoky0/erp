package com.tovo.erp.employe.model;

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

import java.time.LocalDateTime;

/** Demande de correction d'une donnee du dossier salarie, validee par le RH. */
@Entity
@Table(name = "demande_modification")
@Getter
@Setter
public class DemandeModification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_demande")
    private Long id;

    @Column(name = "id_employe", nullable = false)
    private Long idEmploye;

    @Column(name = "champ", nullable = false, length = 80)
    private String champ;

    @Column(name = "ancienne_valeur", columnDefinition = "TEXT")
    private String ancienneValeur;

    @Column(name = "nouvelle_valeur", nullable = false, columnDefinition = "TEXT")
    private String nouvelleValeur;

    @Column(name = "motif", columnDefinition = "TEXT")
    private String motif;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private StatutDemandeModification statut = StatutDemandeModification.EN_ATTENTE;

    @Column(name = "id_demandeur")
    private Long idDemandeur;

    @Column(name = "id_validateur")
    private Long idValidateur;

    @Column(name = "motif_decision", columnDefinition = "TEXT")
    private String motifDecision;

    @Column(name = "date_decision")
    private LocalDateTime dateDecision;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    @PrePersist
    void onCreate() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
    }
}
