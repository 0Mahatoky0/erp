package com.tovo.erp.employe.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Point de suivi d'une mission (avancement et indicateurs). */
@Entity
@Table(name = "suivi_mission")
@Getter
@Setter
public class SuiviMission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_suivi")
    private Long id;

    @Column(name = "id_mission", nullable = false)
    private Long idMission;

    @Column(name = "date_suivi", nullable = false)
    private LocalDateTime dateSuivi = LocalDateTime.now();

    @Column(name = "avancement")
    private Integer avancement;

    @Column(name = "manao_asa")
    private Boolean manaoAsa;

    @Column(name = "miassa_tsara")
    private Boolean miassaTsara;

    @Column(name = "commentaire", columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "id_auteur")
    private Long idAuteur;

    @PrePersist
    void onCreate() {
        if (dateSuivi == null) {
            dateSuivi = LocalDateTime.now();
        }
    }
}
