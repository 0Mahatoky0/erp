package com.tovo.erp.shared.reference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Compteur de references metier (PE-AAAA-NNNN, CT-AAAA-NNNNN).
 * <p>Une ligne par type et par annee : le compteur est verrouille pendant
 * l'increment afin qu'une reference ne soit jamais attribuee deux fois.
 */
@Entity
@Table(name = "compteur_reference",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_compteur_reference", columnNames = {"type_compteur", "annee"}))
@Getter
@Setter
public class CompteurReference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "type_compteur", nullable = false, length = 30)
    private String type;

    @Column(name = "annee", nullable = false)
    private int annee;

    @Column(name = "valeur", nullable = false)
    private long valeur;
}
