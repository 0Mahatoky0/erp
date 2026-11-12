package com.tovo.erp.conges.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "suppleance")
@Getter
@Setter
public class Suppleance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_suppleance")
    private Long id;
    @Column(name = "id_employe_absent", nullable = false)
    private Long idEmployeAbsent;
    @Column(name = "id_employe_remplacant", nullable = false)
    private Long idEmployeRemplacant;
    @Column(name = "id_demande_conge")
    private Long idDemandeConge;
    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;
    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutSuppleance statut = StatutSuppleance.PROPOSEE;
    @Column(length = 500)
    private String motif;
    @Column(name = "date_proposition", nullable = false)
    private LocalDateTime dateProposition = LocalDateTime.now();
    @Column(name = "date_reponse")
    private LocalDateTime dateReponse;
    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();
}
