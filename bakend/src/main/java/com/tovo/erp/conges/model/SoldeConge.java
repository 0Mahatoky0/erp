package com.tovo.erp.conges.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "solde_conge")
@Getter
@Setter
public class SoldeConge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solde_conge")
    private Long id;
    @Column(name = "id_employe", nullable = false)
    private Long idEmploye;
    @Column(name = "id_type_conge", nullable = false)
    private Long idTypeConge;
    @Column(name = "id_droit_conge")
    private Long idDroitConge;
    @Column(nullable = false)
    private int annee;
    @Column(name = "jours_acquis", nullable = false, precision = 6, scale = 2)
    private BigDecimal joursAcquis = BigDecimal.ZERO;
    @Column(name = "jours_pris", nullable = false, precision = 6, scale = 2)
    private BigDecimal joursPris = BigDecimal.ZERO;
    @Column(name = "jours_reserves", nullable = false, precision = 6, scale = 2)
    private BigDecimal joursReserves = BigDecimal.ZERO;
}
