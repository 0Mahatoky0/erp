package com.tovo.erp.demande.domain;

import com.tovo.erp.domain.entity.Budget;
import com.tovo.erp.domain.entity.Utilisateur;
import com.tovo.erp.demande.domain.Decision;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Une verification budgetaire. Chaque verification est conservee, meme si le montant
 * est recalcule apres une correction (RM-053).
 * <p>Les liens vers les referentiels (service, role, poste...) sont charges en eager : ils sont
 * peu nombreux et toujours affiches, ce qui evite les erreurs de chargement tardif.
 */
@Entity
@Table(name = "verification_budget")
@Getter
@Setter
public class VerificationBudget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_verification")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_demande_poste", nullable = false)
    private DemandeRecrutementPoste demandePoste;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_budget_service", nullable = false)
    private Budget budgetService;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_utilisateur", nullable = false)
    private Utilisateur utilisateur;

    /** Budget alloue au service pour l'exercice concerne. */
    @Column(name = "budget_demande", nullable = false, precision = 15, scale = 2)
    private BigDecimal budgetDemande;

    /** Budget disponible au moment de la verification. */
    @Column(name = "budget_disponible", nullable = false, precision = 15, scale = 2)
    private BigDecimal budgetDisponible;

    /** Montant de la ligne verifie : doit correspondre au montant soumis (RM-054). */
    @Column(name = "montant_estime", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantEstime;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false)
    private Decision decision;

    @Column(name = "commentaire")
    private String commentaire;

    @Column(name = "date_verification", nullable = false)
    private LocalDateTime dateVerification = LocalDateTime.now();

    /** Episode de validation de la ligne (RM-040). */
    @Column(name = "episode", nullable = false)
    private Integer episode = 1;
}
