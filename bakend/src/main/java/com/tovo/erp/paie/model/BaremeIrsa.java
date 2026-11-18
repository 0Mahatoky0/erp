package com.tovo.erp.paie.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "bareme_irsa")
@Getter
@Setter
public class BaremeIrsa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_bareme")
    private Long id;
    @Column(name = "date_effet", nullable = false)
    private LocalDate dateEffet;
    @Column(name = "borne_inf", nullable = false, precision = 15, scale = 2)
    private BigDecimal borneInf;
    @Column(name = "borne_sup", precision = 15, scale = 2)
    private BigDecimal borneSup;
    @Column(nullable = false, precision = 8, scale = 5)
    private BigDecimal taux;
}
