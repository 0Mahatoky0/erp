package com.tovo.erp.contrat.model;

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
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Modification d'un contrat signe, historisee et jamais ecrasee (RG-B4-01).
 * <p>Un avenant nait CREE, devient SIGNE a la signature des deux parties puis
 * VALIDE apres controle RH. Les valeurs avant/apres restent consultables.
 */
@Entity
@Table(name = "avenant")
@Getter
@Setter
public class Avenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_avenant")
    private Long id;

    @Column(name = "id_contrat", nullable = false)
    private Long idContrat;

    @Column(name = "numero", nullable = false, length = 40)
    private String numero;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TypeAvenant type;

    @Column(name = "date_effet", nullable = false)
    private LocalDate dateEffet;

    @Column(name = "anciennes_valeurs", columnDefinition = "TEXT")
    private String anciennesValeurs;

    @Column(name = "nouvelles_valeurs", columnDefinition = "TEXT")
    private String nouvellesValeurs;

    @Column(name = "nouveau_salaire", precision = 15, scale = 2)
    private BigDecimal nouveauSalaire;

    @Column(name = "nouvelle_date_fin")
    private LocalDate nouvelleDateFin;

    @Column(name = "nouveaux_horaires", columnDefinition = "TEXT")
    private String nouveauxHoraires;

    @Column(name = "nouveau_lieu", length = 150)
    private String nouveauLieu;

    @Column(name = "id_document")
    private Long idDocument;

    @Enumerated(EnumType.STRING)
    @Column(name = "etat", nullable = false, length = 20)
    private AvenantEtat etat = AvenantEtat.CREE;

    @Column(name = "date_signature")
    private LocalDateTime dateSignature;

    @Column(name = "signataire_id")
    private Long signataireId;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    @PrePersist
    void onCreate() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
    }
}
