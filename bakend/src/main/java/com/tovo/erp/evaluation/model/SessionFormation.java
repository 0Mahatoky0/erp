package com.tovo.erp.evaluation.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "session_formation")
@Getter
@Setter
public class SessionFormation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_session")
    private Long id;
    @Column(name = "id_formation", nullable = false)
    private Long idFormation;
    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;
    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;
    @Column(length = 150)
    private String lieu;
    @Column(nullable = false)
    private int capacite;
    @Column(length = 150)
    private String formateur;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal cout;
    @Column(nullable = false, length = 20)
    private String statut = "PLANIFIEE";

    public enum Statut {
        PLANIFIEE, OUVERTE, COMPLETE, REALISEE, ANNULEE
    }
}
