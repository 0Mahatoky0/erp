package com.tovo.erp.rupture.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "droits_rupture")
@Getter
@Setter
public class DroitsRupture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_dossier", nullable = false, unique = true)
    private Long idDossier;

    @Column(name = "conge_solde", precision = 18, scale = 2)
    private BigDecimal congeSolde;

    @Column(name = "conge_details", columnDefinition = "TEXT")
    private String congeDetails;

    @Column(name = "conges_futurs_annules")
    private int congesFutursAnnules = 0;

    @Column(name = "primes_dues", precision = 18, scale = 2)
    private BigDecimal primesDues;

    @Column(name = "avances_restantes", precision = 18, scale = 2)
    private BigDecimal avancesRestantes;

    @Column(name = "dedit_formation", precision = 18, scale = 2)
    private BigDecimal deditFormation;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "date_arret")
    private LocalDateTime dateArret;
}
