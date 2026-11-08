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

/** Parcours d'integration cree automatiquement a l'acceptation (bloc 4). */
@Entity
@Table(name = "onboarding")
@Getter
@Setter
public class Onboarding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_onboarding")
    private Long id;

    @Column(name = "id_employe", nullable = false)
    private Long idEmploye;

    @Column(name = "id_contrat")
    private Long idContrat;

    @Column(name = "date_debut")
    private LocalDate dateDebut;

    @Column(name = "date_cible")
    private LocalDate dateCible;

    @Column(name = "id_responsable_rh")
    private Long idResponsableRh;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private OnboardingStatut statut = OnboardingStatut.A_FAIRE;

    @Column(name = "progression", nullable = false)
    private Integer progression = 0;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    @PrePersist
    void onCreate() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
    }
}
