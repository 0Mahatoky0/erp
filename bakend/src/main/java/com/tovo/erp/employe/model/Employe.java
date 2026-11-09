package com.tovo.erp.employe.model;

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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Un employe de l'entreprise, cible possible d'un remplacement (RM-057 a RM-060).
 * <p>Le dossier est cree a l'acceptation de la proposition d'embauche en statut
 * PRE_EMBAUCHE et sans matricule (RG-B3-06) : le matricule n'est attribue qu'a
 * la signature du contrat (RG-B4-06).
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

    /** Null tant que le contrat n'est pas signe (bloc 4, RG-B4-06). */
    @Column(name = "matricule", unique = true)
    private String matricule;

    @Column(name = "nom", nullable = false)
    private String nom;

    @Column(name = "prenom", nullable = false)
    private String prenom;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "telephone")
    private String telephone;

    /** Candidat d'origine, sans relation JPA entre les modules recrutement et employe. */
    @Column(name = "id_candidat")
    private Long idCandidat;

    @Column(name = "cin")
    private String cin;

    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    @Column(name = "lieu_naissance")
    private String lieuNaissance;

    @Column(name = "adresse", columnDefinition = "TEXT")
    private String adresse;

    @Column(name = "nationalite")
    private String nationalite;

    @Column(name = "situation_familiale")
    private String situationFamiliale;

    @Column(name = "date_embauche")
    private LocalDate dateEmbauche;

    @Column(name = "date_premiere_embauche")
    private LocalDate datePremiereEmbauche;

    @Column(name = "id_poste")
    private Long idPoste;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private StatutEmploye statut = StatutEmploye.ACTIF;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "date_modification", nullable = false)
    private LocalDateTime dateModification = LocalDateTime.now();

    @PrePersist
    void onCreate() {
        LocalDateTime maintenant = LocalDateTime.now();
        if (dateCreation == null) {
            dateCreation = maintenant;
        }
        dateModification = maintenant;
    }

    @PreUpdate
    void onUpdate() {
        dateModification = LocalDateTime.now();
    }
}
