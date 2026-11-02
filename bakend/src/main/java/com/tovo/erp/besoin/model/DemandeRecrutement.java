package com.tovo.erp.besoin.model;

import com.tovo.erp.iam.model.Utilisateur;
import com.tovo.erp.besoin.model.StatutDemande;
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

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Une demande de recrutement : une demande globale pouvant contenir plusieurs lignes,
 * chacune validee separement (RM-012 a RM-021).
 * <p>Les liens vers les referentiels (service, role, poste...) sont charges en eager : ils sont
 * peu nombreux et toujours affiches, ce qui evite les erreurs de chargement tardif.
 */
@Entity
@Table(name = "demande_recrutement")
@Getter
@Setter
public class DemandeRecrutement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_demande")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_utilisateur", nullable = false)
    private Utilisateur utilisateur;

    /** Motif general de la demande : obligatoire (RM-013). */
    @Column(name = "motif", nullable = false)
    private String motif;

    /** Date souhaitee de recrutement : obligatoire (RM-014). */
    @Column(name = "date_souhaitee", nullable = false)
    private LocalDate dateSouhaitee;

    /**
     * Statut global, recalcule par le backend a partir des lignes (RM-047).
     * Jamais modifie directement par un utilisateur.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private StatutDemande statut = StatutDemande.BROUILLON;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "date_modification", nullable = false)
    private LocalDateTime dateModification = LocalDateTime.now();

    /** Renseignee a la soumission : tant qu'elle est nulle la demande est BROUILLON (RM-041). */
    @Column(name = "date_soumission")
    private LocalDateTime dateSoumission;
}
