package com.tovo.erp.besoin.service;

import com.tovo.erp.besoin.model.DemandeRecrutement;
import com.tovo.erp.shared.UtilisateurConnecte;
import com.tovo.erp.besoin.model.EtapeValidation;
import com.tovo.erp.besoin.model.StatutDemande;
import com.tovo.erp.besoin.model.StatutLigne;
import com.tovo.erp.besoin.repository.BudgetRepository;
import com.tovo.erp.besoin.repository.DemandeRecrutementPosteRepository;
import com.tovo.erp.besoin.repository.DemandeRecrutementRepository;
import com.tovo.erp.besoin.dto.DashboardDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Tableau de bord : indicateurs adapts au role de l'utilisateur connecte.
 */
@Service
public class DashboardService {

    private final DemandeRecrutementRepository demandeRepository;
    private final DemandeRecrutementPosteRepository ligneRepository;
    private final BudgetRepository budgetRepository;

    public DashboardService(DemandeRecrutementRepository demandeRepository,
                            DemandeRecrutementPosteRepository ligneRepository,
                            BudgetRepository budgetRepository) {
        this.demandeRepository = demandeRepository;
        this.ligneRepository = ligneRepository;
        this.budgetRepository = budgetRepository;
    }

    /** Synthese des demandes, de la file de validation et des budgets. */
    @Transactional(readOnly = true)
    public DashboardDto.Resume synthese(UtilisateurConnecte utilisateur, Integer exercice) {
        boolean manager = "MANAGER".equals(utilisateur.getRoleNom());
        List<DemandeRecrutement> demandes = demandeRepository.rechercher(
                utilisateur.getId(), manager, null, null, List.of());

        long brouillons = demandes.stream().filter(d -> d.getStatut() == StatutDemande.BROUILLON).count();
        long enValidation = demandes.stream().filter(d -> d.getStatut() == StatutDemande.EN_VALIDATION).count();
        long aCorriger = demandes.stream().filter(d -> d.getStatut() == StatutDemande.A_CORRIGER).count();
        long finalisees = demandes.stream().filter(d -> d.getStatut() == StatutDemande.FINALISEE
                || d.getStatut() == StatutDemande.FINALISEE_PARTIELLE).count();
        long refusees = demandes.stream().filter(d -> d.getStatut() == StatutDemande.REFUSEE).count();
        long annulees = demandes.stream().filter(d -> d.getStatut() == StatutDemande.ANNULEE).count();

        EtapeValidation etape = etapeDuRole(utilisateur);
        List<StatutLigne> statutsEnAttente = List.of(StatutLigne.EN_ATTENTE_RH,
                StatutLigne.EN_ATTENTE_FINANCE, StatutLigne.EN_ATTENTE_DIRECTION);
        long aValider = etape == null ? 0
                : ligneRepository.findFileAttente(etape).stream()
                .filter(ligne -> !ligne.getDemande().getIdUtilisateur().equals(utilisateur.getId()))
                .count();

        long lignesAcceptees = ligneRepository.findByStatutOrderByDateCreationDesc(StatutLigne.ACCEPTEE).size();
        long lignesACorriger = ligneRepository.findByStatutOrderByDateCreationDesc(StatutLigne.A_CORRIGER).size();

        int exerciceCourant = exercice == null ? java.time.Year.now().getValue() : exercice;
        List<com.tovo.erp.besoin.model.Budget> budgets =
                budgetRepository.rechercher(null, exerciceCourant);
        BigDecimal alloue = budgets.stream()
                .map(com.tovo.erp.besoin.model.Budget::getMontantAlloue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal engage = budgets.stream()
                .map(com.tovo.erp.besoin.model.Budget::getMontantEngage)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal disponible = budgets.stream()
                .map(com.tovo.erp.besoin.model.Budget::getMontantDisponible)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new DashboardDto.Resume(
                demandes.size(), brouillons, enValidation, aCorriger, finalisees, refusees, annulees,
                aValider, lignesAcceptees, lignesACorriger,
                etape == null ? null : etape.name(),
                exerciceCourant, alloue, engage, disponible);
    }

    private EtapeValidation etapeDuRole(UtilisateurConnecte utilisateur) {
        try {
            return EtapeValidation.valueOf(utilisateur.getRoleNom());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
