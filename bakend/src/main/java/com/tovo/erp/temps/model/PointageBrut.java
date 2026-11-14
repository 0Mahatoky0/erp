package com.tovo.erp.temps.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Badgeage brut et immuable (RG-B6-05, RG-B6-06, RG-B6-07, RG-B6-21).
 * Aucune mise a jour n'est autorisee : une correction cree une nouvelle ligne
 * MANUEL qui reference celle qu'elle remplace.
 */
@Entity
@Table(name = "pointage_brut")
@Getter
@Setter
@NoArgsConstructor
public class PointageBrut {

    public enum Sens {
        ENTREE, SORTIE, INDETERMINE
    }

    public enum Source {
        BIOMETRIQUE, BADGE, WEB, MOBILE, MANUEL
    }

    public enum Statut {
        VALIDE, A_VERIFIER, REJETE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pointage_brut")
    private Long id;

    @Column(name = "id_employe", nullable = false)
    private Long idEmploye;

    @Column(name = "horodatage", nullable = false)
    private LocalDateTime horodatage;

    @Enumerated(EnumType.STRING)
    @Column(name = "sens", nullable = false, length = 20)
    private Sens sens = Sens.INDETERMINE;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 30)
    private Source source = Source.MANUEL;

    @Column(name = "terminal_id", length = 80)
    private String terminalId;

    @Column(name = "terminal_authentifie", nullable = false)
    private boolean terminalAuthentifie;

    @Column(name = "lieu_id")
    private Long lieuId;

    @Column(name = "latitude", precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 9, scale = 6)
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private Statut statut = Statut.VALIDE;

    @Column(name = "saisi_par")
    private Long saisiPar;

    @Column(name = "motif", length = 500)
    private String motif;

    @Column(name = "id_pointage_brut_remplace")
    private Long idPointageBrutRemplace;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;

    @PrePersist
    void onCreate() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
    }
}
