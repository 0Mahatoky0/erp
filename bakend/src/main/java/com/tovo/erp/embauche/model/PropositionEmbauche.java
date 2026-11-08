package com.tovo.erp.embauche.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Proposition d'embauche adressee a un candidat retenu (section 5.2).
 * <p>Une seule proposition est active par candidature. Une contre-proposition
 * cree une nouvelle version BROUILLON et retire l'ancienne.
 */
@Entity
@Table(name = "proposition_embauche",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_proposition_candidature_version",
                columnNames = {"id_candidature", "version"}))
@Getter
@Setter
public class PropositionEmbauche {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_proposition")
    private Long id;

    @Column(name = "reference", nullable = false, unique = true, length = 50)
    private String reference;

    @Column(name = "id_candidature", nullable = false)
    private Long idCandidature;

    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "id_poste")
    private Long idPoste;

    @Column(name = "id_departement")
    private Long idDepartement;

    @Column(name = "id_service")
    private Long idService;

    @Column(name = "id_manager")
    private Long idManager;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_contrat", nullable = false, length = 30)
    private TypeContratEmbauche typeContrat;

    @Column(name = "duree_mois")
    private Integer dureeMois;

    @Column(name = "date_debut_prevue", nullable = false)
    private LocalDate dateDebutPrevue;

    @Column(name = "periode_essai_mois", nullable = false)
    private Integer periodeEssaiMois = 1;

    @Column(name = "salaire_brut_mensuel", nullable = false, precision = 15, scale = 2)
    private BigDecimal salaireBrutMensuel;

    @Column(name = "devise", nullable = false, length = 10)
    private String devise = "MGA";

    @Column(name = "primes_avantages", columnDefinition = "TEXT")
    private String primesAvantages;

    @Column(name = "horaires", columnDefinition = "TEXT")
    private String horaires;

    @Column(name = "lieu_travail", length = 150)
    private String lieuTravail;

    @Column(name = "date_envoi")
    private LocalDateTime dateEnvoi;

    @Column(name = "date_expiration")
    private LocalDateTime dateExpiration;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 30)
    private PropositionStatut statut = PropositionStatut.BROUILLON;

    @Column(name = "token_acceptation", unique = true, length = 128)
    private String tokenAcceptation;

    @Column(name = "date_reponse")
    private LocalDateTime dateReponse;

    @Column(name = "motif_refus", columnDefinition = "TEXT")
    private String motifRefus;

    @Column(name = "preuve_reponse", length = 255)
    private String preuveReponse;

    @Column(name = "ip_reponse", length = 64)
    private String ipReponse;

    @Column(name = "empreinte_reponse", length = 128)
    private String empreinteReponse;

    @Column(name = "derogation_finance_direction", nullable = false)
    private boolean derogationFinanceDirection = false;

    @Column(name = "id_utilisateur_createur")
    private Long idUtilisateurCreateur;

    @Column(name = "id_utilisateur_validateur")
    private Long idUtilisateurValidateur;

    @Column(name = "id_employe_cree")
    private Long idEmployeCree;

    @Column(name = "id_contrat_cree")
    private Long idContratCree;

    @Column(name = "id_affectation_creee")
    private Long idAffectationCreee;

    @Column(name = "id_onboarding_cree")
    private Long idOnboardingCree;

    @Column(name = "id_document_pdf")
    private Long idDocumentPdf;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "date_modification", nullable = false)
    private LocalDateTime dateModification = LocalDateTime.now();

    public boolean estBrouillon() {
        return statut == PropositionStatut.BROUILLON;
    }

    public boolean estEnAttenteDeReponse() {
        return statut == PropositionStatut.ENVOYEE;
    }

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
