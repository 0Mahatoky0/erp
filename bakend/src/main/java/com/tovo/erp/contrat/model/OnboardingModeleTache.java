package com.tovo.erp.contrat.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Tache d'un modele d'onboarding ; depend_code permet l'ordonnancement. */
@Entity
@Table(name = "onboarding_modele_tache")
@Getter
@Setter
public class OnboardingModeleTache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tache_modele")
    private Long id;

    @Column(name = "id_modele", nullable = false)
    private Long idModele;

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

    @Column(name = "obligatoire", nullable = false)
    private boolean obligatoire = true;

    @Column(name = "bloquante", nullable = false)
    private boolean bloquante = false;

    @Column(name = "depend_code", length = 50)
    private String dependCode;
}
