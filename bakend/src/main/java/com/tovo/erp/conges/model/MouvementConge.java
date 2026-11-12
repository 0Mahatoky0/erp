package com.tovo.erp.conges.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "mouvement_conge")
@Getter
@Setter
public class MouvementConge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mouvement_conge")
    private Long id;
    @Column(name = "id_employe", nullable = false)
    private Long idEmploye;
    @Column(name = "id_type_conge", nullable = false)
    private Long idTypeConge;
    @Column(name = "id_droit_conge")
    private Long idDroitConge;
    @Column(name = "id_demande_conge")
    private Long idDemandeConge;
    @Enumerated(EnumType.STRING)
    @Column(name = "type_mouvement", nullable = false, length = 20)
    private TypeMouvementConge typeMouvement;
    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal jours;
    @Column(name = "date_effet", nullable = false)
    private LocalDate dateEffet;
    @Column(columnDefinition = "TEXT")
    private String commentaire;
    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();
}
