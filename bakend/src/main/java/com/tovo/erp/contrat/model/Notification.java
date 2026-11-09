package com.tovo.erp.contrat.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Notification interne (relance, escalade ou alerte) consultable par les equipes. */
@Entity
@Table(name = "notification")
@Getter
@Setter
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notification")
    private Long id;

    @Column(name = "type", nullable = false, length = 40)
    private String type;

    @Column(name = "niveau", nullable = false, length = 20)
    private String niveau = "INFO";

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "id_contrat")
    private Long idContrat;

    @Column(name = "id_employe")
    private Long idEmploye;

    @Column(name = "id_tache")
    private Long idTache;

    @Column(name = "destinataire_role", length = 30)
    private String destinataireRole;

    @Column(name = "echeance")
    private LocalDate echeance;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private NotificationStatut statut = NotificationStatut.A_ENVOYER;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "date_envoi")
    private LocalDateTime dateEnvoi;

    @Column(name = "date_lecture")
    private LocalDateTime dateLecture;

    @PrePersist
    void onCreate() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
    }
}
