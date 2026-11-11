package com.tovo.erp.paie.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "periode_paie")
@Getter
@Setter
public class PeriodePaie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_periode")
    private Long id;
    @Column(nullable = false, unique = true)
    private LocalDate mois;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutPeriodePaie statut = StatutPeriodePaie.BROUILLON;
    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();
    @Column(name = "date_cloture")
    private LocalDateTime dateCloture;
}
