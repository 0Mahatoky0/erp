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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Piece du dossier salarie, stockee en base avec son empreinte SHA-256.
 * <p>Le contrat PDF est lui aussi conserve ici, ce qui garantit qu'une
 * signature pourra toujours etre opposee au document exact envoye.
 */
@Entity
@Table(name = "document_employe")
@Getter
@Setter
public class DocumentEmploye {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_document")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_employe", nullable = false)
    private Employe employe;

    @Column(name = "id_contrat")
    private Long idContrat;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_document", nullable = false, length = 40)
    private TypeDocumentEmploye typeDocument;

    @Column(name = "titre", nullable = false, length = 200)
    private String titre;

    @Column(name = "nom_fichier", length = 255)
    private String nomFichier;

    @Column(name = "type_mime", nullable = false, length = 150)
    private String typeMime = "application/pdf";

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "contenu", nullable = false)
    private byte[] contenu;

    @Column(name = "hash_sha256", nullable = false, length = 64)
    private String hashSha256;

    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private StatutDocument statut = StatutDocument.EN_ATTENTE;

    @Enumerated(EnumType.STRING)
    @Column(name = "confidentialite", nullable = false, length = 20)
    private ConfidentialiteDocument confidentialite = ConfidentialiteDocument.CONFIDENTIEL;

    @Column(name = "date_emission")
    private LocalDate dateEmission;

    @Column(name = "date_validite")
    private LocalDate dateValidite;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    @PrePersist
    void onCreate() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
    }
}
