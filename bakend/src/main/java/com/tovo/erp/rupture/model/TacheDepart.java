package com.tovo.erp.rupture.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tache_depart")
@Getter
@Setter
public class TacheDepart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_dossier", nullable = false)
    private Long idDossier;

    @Column(length = 50)
    private String code;

    @Column(length = 150)
    private String libelle;

    @Column(length = 50)
    private String categorie;

    @Column(length = 100)
    private String responsable;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatutTacheRupture statut;

    @Column(name = "date_echeance")
    private LocalDate dateEcheance;

    @Column(name = "date_cloture")
    private LocalDateTime dateCloture;

    @Column(name = "id_responsable")
    private Long idResponsable;

    @Column
    private boolean bloquante = false;

    @Column(columnDefinition = "TEXT")
    private String commentaire;
}
