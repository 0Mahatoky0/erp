package com.tovo.erp.contrat.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Tache du parcours d'integration, rattachee a un onboarding. */
@Entity
@Table(name = "onboarding_tache")
@Getter
@Setter
public class OnboardingTache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tache")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_onboarding", nullable = false)
    private Onboarding onboarding;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "libelle", nullable = false, length = 200)
    private String libelle;

    @Enumerated(EnumType.STRING)
    @Column(name = "categorie", nullable = false, length = 20)
    private CategorieTache categorie;

    @Column(name = "responsable", length = 100)
    private String responsable;

    @Column(name = "echeance_offset_jours")
    private Integer echeanceOffsetJours;

    @Column(name = "echeance")
    private LocalDate echeance;

    @Column(name = "obligatoire", nullable = false)
    private boolean obligatoire = true;

    @Column(name = "bloquante", nullable = false)
    private boolean bloquante = false;

    @Column(name = "id_depend_de")
    private Long idDependDe;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private TacheStatut statut = TacheStatut.A_FAIRE;

    @Column(name = "date_fait")
    private LocalDate dateFait;

    @Column(name = "id_preuve_document")
    private Long idPreuveDocument;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    @PrePersist
    void onCreate() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
    }
}
