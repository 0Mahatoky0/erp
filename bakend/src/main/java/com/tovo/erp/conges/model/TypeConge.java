package com.tovo.erp.conges.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "type_conge")
@Getter
@Setter
public class TypeConge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_type_conge")
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 100)
    private String libelle;

    @Column(name = "jours_acquis_mois", nullable = false, precision = 5, scale = 2)
    private BigDecimal joursAcquisMois = new BigDecimal("2.50");

    @Column(name = "justificatif_obligatoire", nullable = false)
    private boolean justificatifObligatoire;

    @Column(nullable = false)
    private boolean actif = true;
}
