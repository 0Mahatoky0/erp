package com.tovo.erp.iam.model;

import jakarta.persistence.Column;
import com.tovo.erp.besoin.model.Departement;
import jakarta.persistence.Entity;
import com.tovo.erp.besoin.model.Departement;
import jakarta.persistence.FetchType;
import com.tovo.erp.besoin.model.Departement;
import jakarta.persistence.GeneratedValue;
import com.tovo.erp.besoin.model.Departement;
import jakarta.persistence.GenerationType;
import com.tovo.erp.besoin.model.Departement;
import jakarta.persistence.Id;
import com.tovo.erp.besoin.model.Departement;
import jakarta.persistence.JoinColumn;
import com.tovo.erp.besoin.model.Departement;
import jakarta.persistence.ManyToOne;
import com.tovo.erp.besoin.model.Departement;
import jakarta.persistence.Table;
import com.tovo.erp.besoin.model.Departement;
import lombok.Getter;
import lombok.Setter;

/**
 * Un compte applicatif (RM-001 a RM-005).
 * <p>Les liens vers les referentiels (service, role, poste...) sont charges en eager : ils sont
 * peu nombreux et toujours affiches, ce qui evite les erreurs de chargement tardif.
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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_departement")
    private Departement departement;

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
