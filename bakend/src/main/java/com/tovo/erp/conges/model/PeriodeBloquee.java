package com.tovo.erp.conges.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "periode_bloquee")
@Getter
@Setter
public class PeriodeBloquee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_periode_bloquee")
    private Long id;
    @Column(nullable = false, length = 120)
    private String libelle;
    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;
    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;
    @Column(name = "derogation_direction", nullable = false)
    private boolean derogationDirection;
    @Column(nullable = false)
    private boolean actif = true;
}
