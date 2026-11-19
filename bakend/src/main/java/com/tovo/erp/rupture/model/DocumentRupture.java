package com.tovo.erp.rupture.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "document_rupture")
@Getter
@Setter
public class DocumentRupture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_dossier", nullable = false)
    private Long idDossier;

    @Column(length = 50)
    private String code;

    @Column(length = 150)
    private String libelle;

    @Column(length = 20)
    private String format;

    @Column(name = "chemin_ou_hash", length = 255)
    private String cheminOuHash;

    @Column(name = "hash_sha256", length = 128)
    private String hashSha256;

    @Column(name = "id_document_employe")
    private Long idDocumentEmploye;

    @Column(name = "date_generation")
    private LocalDateTime dateGeneration;

    @Column(name = "date_remise")
    private LocalDate dateRemise;

    @Column(name = "mode_remise", length = 50)
    private String modeRemise;

    @Column(name = "reference_preuve", length = 150)
    private String referencePreuve;
}
