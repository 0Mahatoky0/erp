package com.tovo.erp.demande.domain;

import com.tovo.erp.domain.entity.Employe;
import com.tovo.erp.domain.entity.Poste;
import com.tovo.erp.domain.entity.Service;
import com.tovo.erp.demande.domain.EtapeValidation;
import com.tovo.erp.demande.domain.StatutLigne;
import com.tovo.erp.demande.domain.TypeBesoin;
import com.tovo.erp.demande.domain.TypeRemplacement;
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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Une ligne de demande de recrutement : un besoin unitaire, valide independamment
 * des autres lignes de la meme demande (RM-022 a RM-032).
 * <p>Les liens vers les referentiels (service, role, poste...) sont charges en eager : ils sont
 * peu nombreux et toujours affiches, ce qui evite les erreurs de chargement tardif.
 */
@Entity
@Table(name = "demande_recrutement_poste")
@Getter
@Setter
public class DemandeRecrutementPoste {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_demande_poste")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_demande", nullable = false)
    private DemandeRecrutement demande;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_service", nullable = false)
    private Service service;

    /** Poste officiel vise. Renseigne a la creation si le poste existe deja (RM-007). */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_poste")
    private Poste poste;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_besoin", nullable = false)
    private TypeBesoin typeBesoin;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_remplacement")
    private TypeRemplacement typeRemplacement;

    /** Employe remplace : peut rester null si le depart n'est pas encore identifie (RM-026). */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_employe_remplace")
    private Employe employeRemplace;

    @Column(name = "motif_remplacement")
    private String motifRemplacement;

    @Column(name = "date_debut_remplacement")
    private LocalDate dateDebutRemplacement;

    @Column(name = "date_fin_remplacement")
    private LocalDate dateFinRemplacement;

    @Column(name = "intitule_demande", nullable = false)
    private String intituleDemande;

    @Column(name = "description_poste")
    private String descriptionPoste;

    @Column(name = "profil_recherche")
    private String profilRecherche;

    @Column(name = "salaire_min_estime", precision = 15, scale = 2)
    private BigDecimal salaireMinEstime;

    @Column(name = "salaire_max_estime", precision = 15, scale = 2)
    private BigDecimal salaireMaxEstime;

    @Column(name = "effectif", nullable = false)
    private Integer effectif = 1;

    @Column(name = "budget_estime", nullable = false)
    private BigDecimal budgetEstime;

    /** Statut propre a la ligne : chaque ligne suit son propre circuit (RM-032). */
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private StatutLigne statut = StatutLigne.BROUILLON;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "date_modification", nullable = false)
    private LocalDateTime dateModification = LocalDateTime.now();

    @Column(name = "date_soumission")
    private LocalDateTime dateSoumission;

    /** Episode de validation : incremente a chaque resoumission apres correction (RM-040). */
    @Column(name = "episode", nullable = false)
    private Integer episode = 1;

    /** Etape du circuit a laquelle la ligne attend une decision (RM-038). */
    @Enumerated(EnumType.STRING)
    @Column(name = "etape_courante")
    private EtapeValidation etapeCourante;

    /** Vrai si la ligne attend encore une action de la part de quelqu'un. */
    public boolean estEnAttente() {
        return statut == StatutLigne.BROUILLON
                || statut == StatutLigne.A_CORRIGER
                || statut.estEnValidation();
    }
}
