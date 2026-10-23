package com.tovo.erp.service;

import com.tovo.erp.common.BusinessException;
import com.tovo.erp.domain.entity.Budget;
import com.tovo.erp.demande.domain.DemandeRecrutementPoste;
import com.tovo.erp.domain.entity.Utilisateur;
import com.tovo.erp.demande.domain.VerificationBudget;
import com.tovo.erp.demande.domain.Decision;
import com.tovo.erp.repository.BudgetRepository;
import com.tovo.erp.demande.repository.VerificationBudgetRepository;
import com.tovo.erp.web.dto.BudgetDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Budgets de service et controle budgetaire (RM-048 a RM-056).
 */
@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final VerificationBudgetRepository verificationRepository;
    private final com.tovo.erp.repository.ServiceRepository serviceRepository;

    public BudgetService(BudgetRepository budgetRepository,
                         VerificationBudgetRepository verificationRepository,
                         com.tovo.erp.repository.ServiceRepository serviceRepository) {
        this.budgetRepository = budgetRepository;
        this.verificationRepository = verificationRepository;
        this.serviceRepository = serviceRepository;
    }

    /** Liste des budgets filtres par service et par exercice. */
    @Transactional(readOnly = true)
    public List<BudgetDto.Liste> lister(Long idService, Integer exercice) {
        return budgetRepository.rechercher(idService, exercice).stream()
                .map(BudgetDto.Liste::de)
                .toList();
    }

    /** Detail du budget d'un service pour un exercice donne. */
    @Transactional(readOnly = true)
    public BudgetDto.Liste obtenir(Long idService, Integer exercice) {
        return BudgetDto.Liste.de(budgetExistant(idService, exercice));
    }

    /**
     * Definit le montant alloue a un service pour un exercice (RM-048, RM-049).
     * Un couple (service, exercice) ne peut avoir qu'un seul budget.
     */
    @Transactional
    public BudgetDto.Liste definirBudget(BudgetDto.MiseAJour demande) {
        com.tovo.erp.domain.entity.Service service = serviceRepository.getReferenceById(demande.idService());
        if (demande.montantAlloue() == null || demande.montantAlloue().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("BUDGET_INVALIDE",
                    "Le montant alloue doit etre positif ou nul.");
        }
        Budget budget = budgetRepository
                .findByServiceIdAndExercice(demande.idService(), demande.exercice())
                .orElseGet(() -> {
                    Budget nouveau = new Budget();
                    nouveau.setService(service);
                    nouveau.setExercice(demande.exercice());
                    nouveau.setMontantAlloue(BigDecimal.ZERO);
                    nouveau.setMontantDepense(BigDecimal.ZERO);
                    nouveau.setMontantEngage(BigDecimal.ZERO);
                    return nouveau;
                });
        budget.setMontantAlloue(demande.montantAlloue());
        budget.setDateModification(LocalDateTime.now());
        budgetRepository.save(budget);
        return BudgetDto.Liste.de(budget);
    }

    /**
     * Verifie la disponibilite budgetaire d'une ligne et enregistre la verification (RM-050 a RM-055).
     *
     * <p>Le budget est verrouille pendant l'operation : deux verifications simultanees
     * ne peuvent pas depasser le budget disponible (RM-056).
     *
     * @return le budget disponible apres prise en compte de la ligne en attente
     */
    @Transactional
    public BigDecimal verifierLigne(DemandeRecrutementPoste ligne, Decision decision,
                                    Utilisateur validateur, String commentaire) {
        Integer exercice = exerciceDe(ligne);
        Budget budget = budgetRepository.findForMiseAJour(ligne.getService().getId(), exercice)
                .orElseThrow(() -> new BusinessException("BUDGET_ABSENT",
                        "Aucun budget n'est defini pour le service " + ligne.getService().getNom()
                                + " sur l'exercice " + exercice + ". Impossible de valider la ligne."));

        BigDecimal disponible = budget.getMontantDisponible();
        BigDecimal montant = ligne.getBudgetEstime();

        if (decision == Decision.APPROUVE && montant.compareTo(disponible) > 0) {
            throw new BusinessException("BUDGET_INSUFFISANT",
                    "Budget insuffisant : le besoin de " + montant + " depasse le budget disponible de "
                            + disponible + " pour le service " + ligne.getService().getNom()
                            + " sur l'exercice " + exercice + ".");
        }

        VerificationBudget verification = new VerificationBudget();
        verification.setDemandePoste(ligne);
        verification.setBudgetService(budget);
        verification.setUtilisateur(validateur);
        verification.setBudgetDemande(budget.getMontantAlloue());
        verification.setBudgetDisponible(disponible);
        verification.setMontantEstime(montant);
        verification.setDecision(decision);
        verification.setCommentaire(commentaire);
        verification.setEpisode(ligne.getEpisode());
        verificationRepository.save(verification);
        return disponible;
    }

    /**
     * Engage le budget d'une ligne acceptee (RM-051, RM-056).
     * L'engagement est reserve au moment de l'acceptation de la ligne.
     */
    @Transactional
    public void engager(DemandeRecrutementPoste ligne) {
        Integer exercice = exerciceDe(ligne);
        Budget budget = budgetRepository.findForMiseAJour(ligne.getService().getId(), exercice)
                .orElseThrow(() -> new BusinessException("BUDGET_ABSENT",
                        "Aucun budget n'est defini pour le service " + ligne.getService().getNom()
                                + " sur l'exercice " + exercice + " : l'engagement est impossible."));
        budget.setMontantEngage(budget.getMontantEngage().add(ligne.getBudgetEstime()));
        budget.setDateModification(LocalDateTime.now());
        budgetRepository.save(budget);
    }

    /** Annule un engagement lors qu'une ligne acceptee est annulee avant le recrutement. */
    @Transactional
    public void liberer(DemandeRecrutementPoste ligne) {
        Integer exercice = exerciceDe(ligne);
        Budget budget = budgetRepository.findForMiseAJour(ligne.getService().getId(), exercice)
                .orElse(null);
        if (budget == null) {
            return;
        }
        BigDecimal engage = budget.getMontantEngage().subtract(ligne.getBudgetEstime()).max(BigDecimal.ZERO);
        budget.setMontantEngage(engage);
        budget.setDateModification(LocalDateTime.now());
        budgetRepository.save(budget);
    }

    /** Exercice budgetaire de la ligne : annee de la date souhaitee de la demande (RM-050). */
    private Integer exerciceDe(DemandeRecrutementPoste ligne) {
        return ligne.getDemande().getDateSouhaitee().getYear();
    }

    private Budget budgetExistant(Long idService, Integer exercice) {
        return budgetRepository.findByServiceIdAndExercice(idService, exercice)
                .orElseThrow(() -> BusinessException.notFound(
                        "Aucun budget pour le service " + idService + " sur l'exercice " + exercice + "."));
    }
}
