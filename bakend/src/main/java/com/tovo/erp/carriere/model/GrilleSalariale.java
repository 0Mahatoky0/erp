package com.tovo.erp.carriere.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "grille_salariale")
@Getter
@Setter
@NoArgsConstructor
public class GrilleSalariale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_grille")
    private Long id;

    @Column(name = "categorie", nullable = false, length = 50)
    private String categorie;

    @Column(name = "echelon", nullable = false, length = 50)
    private String echelon;

    @Column(name = "salaire_min", nullable = false, precision = 15, scale = 2)
    private BigDecimal salaireMin;

    @Column(name = "salaire_max", nullable = false, precision = 15, scale = 2)
    private BigDecimal salaireMax;

    @Column(name = "date_effet", nullable = false)
    private LocalDate dateEffet;

    @Column(name = "date_fin")
    private LocalDate dateFin;
}
