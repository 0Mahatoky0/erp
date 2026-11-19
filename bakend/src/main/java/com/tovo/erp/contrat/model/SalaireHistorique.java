package com.tovo.erp.contrat.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Ligne datee de remuneration ; la valeur courante a une date de fin nulle. */
@Entity
@Table(name = "salaire_historique")
@Getter
@Setter
@NoArgsConstructor
public class SalaireHistorique {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_salaire_historique")
    private Long id;

    @Column(name = "id_employe", nullable = false)
    private Long idEmploye;

    @Column(name = "id_contrat")
    private Long idContrat;

    @Column(name = "montant_brut", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantBrut;

    @Column(name = "devise", nullable = false, length = 10)
    private String devise = "MGA";

    @Column(name = "date_effet", nullable = false)
    private LocalDate dateEffet;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Column(name = "motif", length = 100)
    private String motif;

    @Column(name = "id_utilisateur")
    private Long idUtilisateur;

    @Column(name = "enregistre_le", nullable = false)
    private LocalDateTime enregistreLe = LocalDateTime.now();

    @Column(name = "enregistre_par")
    private Long enregistrePar;

    @Column(name = "remplace_le")
    private LocalDateTime remplaceLe;

    @Column(name = "remplace_par_id")
    private Long remplaceParId;

    @Column(name = "motif_correction", columnDefinition = "TEXT")
    private String motifCorrection;

    @Column(name = "id_mouvement")
    private Long idMouvement;

    @Column(name = "id_avenant")
    private Long idAvenant;

    @Column(name = "ancien_montant", precision = 15, scale = 2)
    private BigDecimal ancienMontant;

    @Column(name = "variation_pct", precision = 8, scale = 4)
    private BigDecimal variationPct;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;

    @PrePersist
    void onCreate() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
    }
}
