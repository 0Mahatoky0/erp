package com.tovo.erp.rupture.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "entretien_rupture")
@Getter
@Setter
public class EntretienRupture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_dossier", nullable = false)
    private Long idDossier;

    @Column(name = "date_entretien")
    private LocalDate dateEntretien;

    @Column(name = "animateur_id")
    private Long animateurId;

    @Column(name = "questionnaire_json", columnDefinition = "TEXT")
    private String questionnaireJson;

    @Column
    private Integer satisfaction;

    @Column(name = "commentaire_confidentiel", columnDefinition = "TEXT")
    private String commentaireConfidentiel;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation;

    @PrePersist
    void onCreate() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
    }
}
