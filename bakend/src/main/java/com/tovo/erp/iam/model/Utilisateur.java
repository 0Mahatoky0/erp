package com.tovo.erp.iam.model;

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

/**
 * Un compte applicatif (RM-001 a RM-005).
 * <p>Le departement est reference par son identifiant : iam ne depend pas du
 * module besoin et aucune relation JPA inter-modules n'est creee.
 */
@Entity
@Table(name = "utilisateur")
@Getter
@Setter
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_utilisateur")
    private Long id;

    @Column(name = "id_departement")
    private Long idDepartement;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_role", nullable = false)
    private Role role;

    @Column(name = "nom", nullable = false)
    private String nom;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    /** Hash du mot de passe : jamais le mot de passe en clair (RM-002). */
    @Column(name = "mot_de_passe", nullable = false)
    private String motDePasse;

    /** Un utilisateur inactif ne peut plus se connecter ni agir (RM-003). */
    @Column(name = "actif", nullable = false)
    private boolean actif = true;
}
