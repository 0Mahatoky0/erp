package com.tovo.erp.demande.domain;

import com.tovo.erp.domain.entity.Utilisateur;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Journal des changements de statut (RM-065).
 * <p>Si {@code idDemandePoste} est null, le changement concerne le statut global
 * de la demande ; sinon il concerne le statut de la ligne.
 * <p>Les liens vers les referentiels (service, role, poste...) sont charges en eager : ils sont
 * peu nombreux et toujours affiches, ce qui evite les erreurs de chargement tardif.
 */
@Entity
@Table(name = "historique_statut")
@Getter
@Setter
public class HistoriqueStatut {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historique")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_demande", nullable = false)
    private DemandeRecrutement demande;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_demande_poste")
    private DemandeRecrutementPoste demandePoste;

    @Column(name = "ancien_statut")
    private String ancienStatut;

    @Column(name = "nouveau_statut", nullable = false)
    private String nouveauStatut;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_utilisateur")
    private Utilisateur utilisateur;

    @Column(name = "commentaire")
    private String commentaire;

    @Column(name = "date_action", nullable = false)
    private LocalDateTime dateAction = LocalDateTime.now();
}
