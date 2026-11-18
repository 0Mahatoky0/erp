package com.tovo.erp.evaluation.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inscription_formation")
@Getter
@Setter
public class InscriptionFormation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_inscription")
    private Long id;
    @Column(name = "id_session", nullable = false)
    private Long idSession;
    @Column(name = "id_employe", nullable = false)
    private Long idEmploye;
    @Column(name = "date_demande", nullable = false)
    private LocalDateTime dateDemande = LocalDateTime.now();
    @Column(nullable = false, length = 25)
    private String statut = "DEMANDEE";
    @Column(name = "presence_h", precision = 8, scale = 2)
    private BigDecimal presenceH;
    @Column(columnDefinition = "TEXT")
    private String resultat;
    @Column(name = "certificat_document_id")
    private Long certificatDocumentId;
    @Column(name = "id_besoin")
    private Long idBesoin;

    public enum Statut {
        DEMANDEE, VALIDEE_MANAGER, CONFIRMEE, LISTE_ATTENTE, PRESENT, ABSENT, ANNULEE
    }
}
