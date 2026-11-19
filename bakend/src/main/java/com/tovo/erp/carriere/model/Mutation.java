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
@Table(name = "mutation")
@Getter
@Setter
@NoArgsConstructor
public class Mutation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mutation")
    private Long id;

    @Column(name = "id_mouvement", nullable = false, unique = true)
    private Long idMouvement;

    @Column(name = "type", nullable = false, length = 30)
    private String type;

    @Column(name = "initiative", nullable = false, length = 25)
    private String initiative;

    @Column(name = "id_poste_origine")
    private Long idPosteOrigine;

    @Column(name = "id_poste_destination")
    private Long idPosteDestination;

    @Column(name = "id_service_origine")
    private Long idServiceOrigine;

    @Column(name = "id_service_destination")
    private Long idServiceDestination;

    @Column(name = "id_lieu_origine", length = 150)
    private String idLieuOrigine;

    @Column(name = "id_lieu_destination", length = 150)
    private String idLieuDestination;

    @Column(name = "id_manager_origine")
    private Long idManagerOrigine;

    @Column(name = "id_manager_destination")
    private Long idManagerDestination;

    @Column(name = "date_retour_prevue")
    private LocalDate dateRetourPrevue;

    @Column(name = "clause_mobilite_verifiee", nullable = false)
    private boolean clauseMobiliteVerifiee;

    @Column(name = "accord_salarie_document_id")
    private Long accordSalarieDocumentId;

    @Column(name = "indemnite_mobilite", precision = 15, scale = 2)
    private BigDecimal indemniteMobilite;
}
