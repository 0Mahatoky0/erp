package com.tovo.erp.conges.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "droit_conge")
@Getter
@Setter
public class DroitConge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_droit_conge")
    private Long id;
    @Column(name = "id_employe", nullable = false)
    private Long idEmploye;
    @Column(name = "id_type_conge", nullable = false)
    private Long idTypeConge;
    @Column(nullable = false)
    private int annee;
    @Column(name = "jours_autorises", nullable = false, precision = 6, scale = 2)
    private BigDecimal joursAutorises = BigDecimal.ZERO;
    @Column(name = "report_entrant", nullable = false, precision = 6, scale = 2)
    private BigDecimal reportEntrant = BigDecimal.ZERO;
}
