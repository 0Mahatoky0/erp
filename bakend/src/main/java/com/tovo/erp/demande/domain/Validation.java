package com.tovo.erp.domain.entity;

import com.tovo.erp.domain.enums.Decision;
import com.tovo.erp.domain.enums.EtapeValidation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Une decision de validation. L'historique est conserve : une nouvelle decision
 * s'ajoute, elle n'ecrase jamais la precedente (RM-033, RM-065).
 * <p>Les liens vers les referentiels (service, role, poste...) sont charges en eager : ils sont
 * peu nombreux et toujours affiches, ce qui evite les erreurs de chargement tardif.
 */
@Entity
@Table(name = "validation")
@Getter
@Setter
public class Validation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_validation")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_demande_poste", nullable = false)
    private DemandeRecrutementPoste demandePoste;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_utilisateur", nullable = false)
    private Utilisateur utilisateur;

    @Enumerated(EnumType.STRING)
    @Column(name = "etape", nullable = false)
    private EtapeValidation etape;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false)
    private Decision decision;

    @Column(name = "commentaire")
    private String commentaire;

    @Column(name = "date_action", nullable = false)
    private LocalDateTime dateAction = LocalDateTime.now();

    /** Episode de validation de la ligne auquel la decision se rattache (RM-040). */
    @Column(name = "episode", nullable = false)
    private Integer episode = 1;
}
