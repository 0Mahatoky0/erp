package com.tovo.erp.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Un poste officiel du catalogue (RM-006 a RM-011).
 * <p>Les liens vers les referentiels (service, role, poste...) sont charges en eager : ils sont
 * peu nombreux et toujours affiches, ce qui evite les erreurs de chargement tardif.
 */
@Entity
@Table(name = "poste")
@Getter
@Setter
public class Poste {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_poste")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_service", nullable = false)
    private Service service;

    @Column(name = "intitule", nullable = false)
    private String intitule;

    @Column(name = "description")
    private String description;

    @Column(name = "salaire_min", precision = 15, scale = 2)
    private BigDecimal salaireMin;

    @Column(name = "salaire_max", precision = 15, scale = 2)
    private BigDecimal salaireMax;

    /** Un poste deja utilise n'est pas supprime mais desactive (RM-011). */
    @Column(name = "actif", nullable = false)
    private boolean actif = true;
}
