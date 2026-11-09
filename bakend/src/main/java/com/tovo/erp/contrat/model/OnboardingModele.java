package com.tovo.erp.contrat.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Modele parametrable de parcours d'integration (RG-B4-08). */
@Entity
@Table(name = "onboarding_modele")
@Getter
@Setter
public class OnboardingModele {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_modele")
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "libelle", nullable = false, length = 200)
    private String libelle;

    @Column(name = "type_contrat", length = 30)
    private String typeContrat;

    @Column(name = "categorie_poste", length = 50)
    private String categoriePoste;

    @Column(name = "site", length = 150)
    private String site;

    @Column(name = "actif", nullable = false)
    private boolean actif = true;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();
}
