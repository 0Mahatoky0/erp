package com.tovo.erp.temps.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "jour_ferie")
@Getter
@Setter
public class JourFerie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_jour_ferie")
    private Long id;
    @Column(name = "date_ferie", nullable = false, unique = true)
    private LocalDate date;
    @Column(nullable = false, length = 120)
    private String libelle;
    @Column(nullable = false)
    private boolean recurrent;
}
