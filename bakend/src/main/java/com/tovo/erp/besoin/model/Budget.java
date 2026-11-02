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
import java.time.LocalDateTime;

/**
 * Budget annuel alloue a un service pour un exercice donne (RM-048, RM-049).
 * <p>Les liens vers les referentiels (service, role, poste...) sont charges en eager : ils sont
 * peu nombreux et toujours affiches, ce qui evite les erreurs de chargement tardif.
 */
@Entity
@Table(name = "budget_service")
@Getter
@Setter
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_budget_service")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_service", nullable = false)
    private Service service;

    @Column(name = "exercice", nullable = false)
    private Integer exercice;

    @Column(name = "montant_alloue", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantAlloue;

    /** Depenses reelles imputees au budget (RM-051). */
    @Column(name = "montant_depense", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantDepense = BigDecimal.ZERO;

    /** Engagements budgets : lignes acceptees (RM-051, RM-056). */
    @Column(name = "montant_engage", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantEngage = BigDecimal.ZERO;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "date_modification", nullable = false)
    private LocalDateTime dateModification = LocalDateTime.now();

    /**
     * Budget encore disponible : allocation moins depenses reelles moins engagements
     * deja reserves (RM-051).
     */
    public BigDecimal getMontantDisponible() {
        return montantAlloue
                .subtract(montantDepense)
                .subtract(montantEngage)
                .max(BigDecimal.ZERO);
    }
}
