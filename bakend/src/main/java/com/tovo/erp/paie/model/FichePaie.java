package com.tovo.erp.paie.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "fiche_paie")
@Getter
@Setter
public class FichePaie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_fiche")
    private Long id;
    @Column(name = "id_bulletin", nullable = false, unique = true)
    private Long idBulletin;
    @Column(name = "id_periode", nullable = false)
    private Long idPeriode;
    @Column(name = "id_employe", nullable = false)
    private Long idEmploye;
    @Column(nullable = false)
    private LocalDate mois;
    @Column(length = 30)
    private String numero;
    @Column(name = "net_a_payer", nullable = false, precision = 15, scale = 2)
    private BigDecimal netAPayer;
    @Column(name = "contenu_json", nullable = false)
    private String contenuJson;
    @Column(nullable = false, length = 64)
    private String hash;
    @Column(name = "date_generation", nullable = false)
    private LocalDateTime dateGeneration = LocalDateTime.now();
}
