package com.tovo.erp.employe.model;

import com.tovo.erp.employe.model.StatutEmploye;
import jakarta.persistence.Column;
import com.tovo.erp.besoin.model.Poste;
import jakarta.persistence.Entity;
import com.tovo.erp.besoin.model.Poste;
import jakarta.persistence.EnumType;
import com.tovo.erp.besoin.model.Poste;
import jakarta.persistence.Enumerated;
import com.tovo.erp.besoin.model.Poste;
import jakarta.persistence.FetchType;
import com.tovo.erp.besoin.model.Poste;
import jakarta.persistence.GeneratedValue;
import com.tovo.erp.besoin.model.Poste;
import jakarta.persistence.GenerationType;
import com.tovo.erp.besoin.model.Poste;
import jakarta.persistence.Id;
import com.tovo.erp.besoin.model.Poste;
import jakarta.persistence.JoinColumn;
import com.tovo.erp.besoin.model.Poste;
import jakarta.persistence.ManyToOne;
import com.tovo.erp.besoin.model.Poste;
import jakarta.persistence.Table;
import com.tovo.erp.besoin.model.Poste;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Un employe de l'entreprise, cible possible d'un remplacement (RM-057 a RM-060).
 * <p>Les liens vers les referentiels (service, role, poste...) sont charges en eager : ils sont
 * peu nombreux et toujours affiches, ce qui evite les erreurs de chargement tardif.
 */
@Entity
@Table(name = "employe")
@Getter
@Setter
public class Employe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_employe")
    private Long id;

    @Column(name = "matricule", nullable = false, unique = true)
    private String matricule;

    @Column(name = "nom", nullable = false)
    private String nom;

    @Column(name = "prenom", nullable = false)
    private String prenom;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "telephone")
    private String telephone;

    @Column(name = "date_embauche")
    private LocalDate dateEmbauche;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_poste")
    private Poste poste;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private StatutEmploye statut = StatutEmploye.ACTIF;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();
}
