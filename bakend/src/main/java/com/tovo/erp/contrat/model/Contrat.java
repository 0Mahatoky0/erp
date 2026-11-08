package com.tovo.erp.contrat.model;

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
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Contrat cree automatiquement a l'acceptation de la proposition (bloc 4).
 * <p>Le contrat est d'abord CREE et non signe : le matricule du salarie n'est
 * attribue qu'a la signature (RG-B4-06).
 */
@Entity
@Table(name = "contrat")
@Getter
@Setter
public class Contrat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_contrat")
    private Long id;

    @Column(name = "numero", nullable = false, unique = true, length = 30)
    private String numero;

    @Column(name = "id_employe", nullable = false)
    private Long idEmploye;

    @Column(name = "id_proposition")
    private Long idProposition;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_contrat", nullable = false, length = 30)
    private TypeContrat typeContrat;

    @Column(name = "id_contrat_precedent")
    private Long idContratPrecedent;

    @Column(name = "rang", nullable = false)
    private Integer rang = 1;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin_prevue")
    private LocalDate dateFinPrevue;

    @Column(name = "date_fin_reelle")
    private LocalDate dateFinReelle;

    @Column(name = "fin_periode_essai")
    private LocalDate finPeriodeEssai;

    @Column(name = "salaire_base", nullable = false, precision = 15, scale = 2)
    private BigDecimal salaireBase;

    @Column(name = "devise", nullable = false, length = 10)
    private String devise = "MGA";

    @Column(name = "primes_avantages", columnDefinition = "TEXT")
    private String primesAvantages;

    @Column(name = "horaires", columnDefinition = "TEXT")
    private String horaires;

    @Column(name = "lieu_travail", length = 150)
    private String lieuTravail;

    @Column(name = "motif_cdd", columnDefinition = "TEXT")
    private String motifCdd;

    @Enumerated(EnumType.STRING)
    @Column(name = "etat", nullable = false, length = 20)
    private ContratEtat etat = ContratEtat.CREE;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_execution", nullable = false, length = 20)
    private StatutExecutionContrat statutExecution = StatutExecutionContrat.A_VENIR;

    @Column(name = "date_signature_salarie")
    private LocalDateTime dateSignatureSalarie;

    @Column(name = "date_signature_employeur")
    private LocalDateTime dateSignatureEmployeur;

    @Column(name = "signataire_employeur_id")
    private Long signataireEmployeurId;

    @Column(name = "document_pdf_id")
    private Long documentPdfId;

    @Column(name = "gabarit_version", length = 50)
    private String gabaritVersion;

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
