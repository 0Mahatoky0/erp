package com.tovo.erp.embauche.model;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Journal immuable des changements d'etat d'une proposition. */
@Entity
@Table(name = "historique_proposition")
@Getter
@Setter
public class HistoriqueProposition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historique")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_proposition", nullable = false)
    private PropositionEmbauche proposition;

    @Column(name = "id_utilisateur")
    private Long idUtilisateur;

    @Enumerated(EnumType.STRING)
    @Column(name = "auteur_type", nullable = false, length = 20)
    private AuteurProposition auteurType = AuteurProposition.UTILISATEUR;

    @Enumerated(EnumType.STRING)
    @Column(name = "ancien_statut", length = 30)
    private PropositionStatut ancienStatut;

    @Enumerated(EnumType.STRING)
    @Column(name = "nouveau_statut", nullable = false, length = 30)
    private PropositionStatut nouveauStatut;

    @Column(name = "motif", columnDefinition = "TEXT")
    private String motif;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "date_action", nullable = false)
    private LocalDateTime dateAction = LocalDateTime.now();

    @PrePersist
    void onCreate() {
        if (dateAction == null) {
            dateAction = LocalDateTime.now();
        }
    }
}
