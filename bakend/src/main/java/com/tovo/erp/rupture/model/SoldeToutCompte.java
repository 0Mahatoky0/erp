package com.tovo.erp.rupture.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "solde_tout_compte")
@Getter
@Setter
public class SoldeToutCompte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_dossier", nullable = false, unique = true)
    private Long idDossier;

    @Column(name = "salaire_du", precision = 18, scale = 2)
    private BigDecimal salaireDu;

    @Column(name = "indemnite_preavis", precision = 18, scale = 2)
    private BigDecimal indemnitePreavis;

    @Column(name = "indemnite_licenciement", precision = 18, scale = 2)
    private BigDecimal indemniteLicenciement;

    @Column(name = "indemnite_conges", precision = 18, scale = 2)
    private BigDecimal indemniteConges;

    @Column(precision = 18, scale = 2)
    private BigDecimal primes;

    @Column(name = "retenues_avances", precision = 18, scale = 2)
    private BigDecimal retenuesAvances;

    @Column(name = "retenues_dedit", precision = 18, scale = 2)
    private BigDecimal retenuesDedit;

    @Column(name = "retenues_materiel", precision = 18, scale = 2)
    private BigDecimal retenuesMateriel;

    @Column(name = "total_brut", precision = 18, scale = 2)
    private BigDecimal totalBrut;

    @Column(name = "total_retenues", precision = 18, scale = 2)
    private BigDecimal totalRetenues;

    @Column(name = "net_avant_cotisations", precision = 18, scale = 2)
    private BigDecimal netAvantCotisations;

    @Column(name = "montant_paye", precision = 18, scale = 2)
    private BigDecimal montantPaye;

    @Column(length = 10)
    private String devise;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private StatutSoldeRupture statut;

    @Column(name = "date_preparation")
    private LocalDateTime datePreparation;

    @Column(name = "id_calculateur")
    private Long idCalculateur;

    @Column(name = "date_validation")
    private LocalDateTime dateValidation;

    @Column(name = "id_validateur")
    private Long idValidateur;

    @Column(name = "date_paiement")
    private LocalDate datePaiement;

    @Column(name = "reference_paiement", length = 100)
    private String referencePaiement;

    @Column(name = "mode_paiement", length = 50)
    private String modePaiement;

    @Column(name = "motif_refus", columnDefinition = "TEXT")
    private String motifRefus;

    @Column(name = "id_bulletin_paie")
    private Long idBulletinPaie;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    @PrePersist
    void onCreate() {
        LocalDateTime maintenant = LocalDateTime.now();
        if (dateCreation == null) {
            dateCreation = maintenant;
        }
        if (dateModification == null) {
            dateModification = maintenant;
        }
    }

    @PreUpdate
    void onUpdate() {
        dateModification = LocalDateTime.now();
    }
}
