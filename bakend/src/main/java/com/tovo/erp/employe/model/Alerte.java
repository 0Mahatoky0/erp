package com.tovo.erp.employe.model;

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

/** Alerte calculee par tache planifiee et lue par l'ecran RH (bloc 5). */
@Entity
@Table(name = "alerte")
@Getter
@Setter
public class Alerte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_alerte")
    private Long id;

    @Column(name = "id_employe")
    private Long idEmploye;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_alerte", nullable = false, length = 40)
    private TypeAlerte typeAlerte;

    @Enumerated(EnumType.STRING)
    @Column(name = "niveau", nullable = false, length = 20)
    private NiveauAlerte niveau = NiveauAlerte.AVERTISSEMENT;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "date_echeance")
    private LocalDate dateEcheance;

    @Column(name = "lue", nullable = false)
    private boolean lue;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    @PrePersist
    void onCreate() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
    }
}
