package com.tovo.erp.besoin.service;

import com.tovo.erp.shared.BusinessException;
import com.tovo.erp.besoin.service.CircuitValidationProperties;
import com.tovo.erp.besoin.model.DemandeRecrutement;
import com.tovo.erp.besoin.model.DemandeRecrutementPoste;
import com.tovo.erp.besoin.model.HistoriqueStatut;
import com.tovo.erp.besoin.model.Poste;
import com.tovo.erp.shared.UtilisateurConnecte;
import com.tovo.erp.besoin.model.Validation;
import com.tovo.erp.besoin.model.Decision;
import com.tovo.erp.besoin.model.EtapeValidation;
import com.tovo.erp.besoin.model.StatutLigne;
import com.tovo.erp.besoin.repository.DemandeRecrutementPosteRepository;
import com.tovo.erp.besoin.repository.HistoriqueStatutRepository;
import com.tovo.erp.besoin.repository.PosteRepository;
import com.tovo.erp.besoin.repository.ValidationRepository;
import com.tovo.erp.besoin.dto.DemandeDto;
import com.tovo.erp.besoin.dto.ValidationDto;
import com.tovo.erp.besoin.service.BudgetService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Circuit de validation d'une ligne de demande (RM-032 a RM-040).
 *
 * <p>Chaque ligne est validee independamment. Les etapes ne peuvent pas etre sautees
 * et une meme etape ne peut pas etre validee deux fois sur un meme episode.
 */
@Service
public class ValidationService {

    private final DemandeRecrutementPosteRepository ligneRepository;
    private final ValidationRepository validationRepository;
    private final HistoriqueStatutRepository historiqueStatutRepository;
    private final PosteRepository posteRepository;
    private final StatutService statutService;
    private final BudgetService budgetService;
    private final DemandeService demandeService;
    private final LigneRegles ligneRegles;
    private final CircuitValidationProperties circuitProperties;

    public ValidationService(DemandeRecrutementPosteRepository ligneRepository,
                             ValidationRepository validationRepository,
                             HistoriqueStatutRepository historiqueStatutRepository,
                             PosteRepository posteRepository,
                             StatutService statutService,
                             BudgetService budgetService,
                             DemandeService demandeService,
                             LigneRegles ligneRegles,
                             CircuitValidationProperties circuitProperties) {
        this.ligneRepository = ligneRepository;
        this.validationRepository = validationRepository;
        this.historiqueStatutRepository = historiqueStatutRepository;
        this.posteRepository = posteRepository;
        this.statutService = statutService;
        this.budgetService = budgetService;
        this.demandeService = demandeService;
        this.ligneRegles = ligneRegles;
        this.circuitProperties = circuitProperties;
    }

    /**
     * Enregistre la decision d'un validateur et fait avancer la ligne dans le circuit.
     *
     * @param idLigne      ligne concernee
     * @param decision     decision prise (APPROUVE, REFUSE, A_CORRIGER)
     * @param validateur   utilisateur qui decide
     */
    @Transactional
    public DemandeDto.LigneResume decider(Long idLigne, ValidationDto.DecisionRequest decision,
                                          UtilisateurConnecte validateur) {
        DemandeRecrutementPoste ligne = ligneRepository.findById(idLigne)
                .orElseThrow(() -> BusinessException.notFound("La ligne " + idLigne + " n'existe pas."));
        DemandeRecrutement demande = ligne.getDemande();

        controlerEtape(ligne, validateur);
        controlerSeparationDesResponsabilites(demande, validateur);
        controlerDecisionUnique(ligne);

        EtapeValidation etape = ligne.getEtapeCourante();

        // Le RH examine le dossier et definit le budget et la fourchette salariale du profil.
        // Elles doivent etre posees avant le calcul de l'etape suivante (seuil Direction, RM-034).
        if (etape == EtapeValidation.RH && decision.decision() == Decision.APPROUVE) {
            definirEvaluationRh(ligne, decision);
        }

        // Les etapes autres que le RH travaillent sur le budget fixe par le RH.
        controlerBudgetDefini(ligne, etape);

        // RM-054 : le montant verifie par la finance doit correspondre au montant defini par le RH.
        controlerMontantVerifie(ligne, decision);

        // L'etape financiere verifie la disponibilite budgetaire (RM-050 a RM-055).
        if (etape == EtapeValidation.FINANCE) {
            budgetService.verifierLigne(ligne, decision.decision(), validateur, decision.commentaire());
        }

        validationRepository.save(construireDecision(ligne, validateur, etape, decision));

        StatutLigne ancienStatut = ligne.getStatut();
        appliquerDecision(ligne, decision.decision(), etape);

        ligne.setDateModification(LocalDateTime.now());
        ligneRepository.save(ligne);
        journaliser(demande, ligne, ancienStatut, ligne.getStatut(), validateur, decision.commentaire());

        List<DemandeRecrutementPoste> lignes = ligneRepository.findByDemandeIdOrderById(demande.getId());
        statutService.recalculer(demande, lignes, validateur,
                "Decision " + decision.decision() + " a l'etape " + etape + ".");
        return versResume(ligne);
    }

    /** File de travail du validateur : les lignes qui attendent sa decision. */
    @Transactional(readOnly = true)
    public List<DemandeDto.LigneResume> fileAttente(UtilisateurConnecte validateur) {
        EtapeValidation etape = etapeDuRole(validateur);
        if (etape == null) {
            return List.of();
        }
        return ligneRepository.findFileAttente(etape).stream().map(this::versResume).toList();
    }

    // ------------------------------------------------------------------
    // Transitions
    // ------------------------------------------------------------------

    private void appliquerDecision(DemandeRecrutementPoste ligne, Decision decision, EtapeValidation etape) {
        switch (decision) {
            case REFUSE -> ligne.setStatut(StatutLigne.REFUSEE);
            case A_CORRIGER -> {
                // La ligne attend une correction : elle reprendra a la meme etape (RM-038).
                ligne.setStatut(StatutLigne.A_CORRIGER);
                ligne.setEtapeCourante(etape);
            }
            case APPROUVE -> {
                EtapeValidation suivante = etapeSuivante(ligne, etape);
                if (suivante == null) {
                    ligne.setStatut(StatutLigne.ACCEPTEE);
                    ligne.setEtapeCourante(null);
                    accepterLigne(ligne);
                } else {
                    ligne.setStatut(demandeService.statutPourEtape(suivante));
                    ligne.setEtapeCourante(suivante);
                }
            }
            default -> throw new BusinessException("DECISION_INCONNUE", "Decision non gereree.");
        }
    }

    /**
     * Derniere etape du circuit : la ligne est acceptee.
     * Le poste officiel est cree si necessaire et le budget est engage (RM-009, RM-051, RM-056).
     */
    private void accepterLigne(DemandeRecrutementPoste ligne) {
        creerPosteSiNecessaire(ligne);
        budgetService.engager(ligne);
    }

    /**
     * RM-009 et RM-010 : si la ligne ne reference pas encore de poste officiel, le poste est
     * cree a partir des informations de la demande, dans la meme transaction (RM-066).
     */
    private void creerPosteSiNecessaire(DemandeRecrutementPoste ligne) {
        if (ligne.getPoste() != null) {
            return;
        }
        List<Poste> equivalents = posteRepository.findEquivalents(
                ligne.getService().getId(), ligne.getIntituleDemande(), null);
        if (!equivalents.isEmpty()) {
            ligne.setPoste(equivalents.get(0));
            return;
        }
        Poste poste = new Poste();
        poste.setService(ligne.getService());
        poste.setIntitule(ligne.getIntituleDemande());
        poste.setDescription(ligne.getDescriptionPoste());
        poste.setSalaireMin(ligne.getSalaireMinEstime());
        poste.setSalaireMax(ligne.getSalaireMaxEstime());
        poste.setActif(true);
        posteRepository.save(poste);
        ligne.setPoste(poste);
    }

    /** Etape suivante du circuit, ou null si la ligne est a sa derniere etape. */
    private EtapeValidation etapeSuivante(DemandeRecrutementPoste ligne, EtapeValidation etape) {
        List<EtapeValidation> etapes = etapesRequises(ligne);
        int index = etapes.indexOf(etape);
        if (index < 0 || index + 1 >= etapes.size()) {
            return null;
        }
        return etapes.get(index + 1);
    }

    /**
     * Etapes obligatoires pour une ligne. L'accord de la direction peut etre conditionne
     * a un seuil de budget (RM-034).
     */
    private List<EtapeValidation> etapesRequises(DemandeRecrutementPoste ligne) {
        List<EtapeValidation> etapes = new ArrayList<>(circuitProperties.etapesEnum());
        BigDecimal seuil = circuitProperties.seuilDirection();
        if (seuil.compareTo(BigDecimal.ZERO) > 0
                && ligne.getBudgetEstime() != null
                && ligne.getBudgetEstime().compareTo(seuil) < 0) {
            etapes.remove(EtapeValidation.DIRECTION);
        }
        return etapes;
    }

    // ------------------------------------------------------------------
    // Controles
    // ------------------------------------------------------------------

    /** RM-039 : on ne peut pas valider une ligne a une etape qui ne lui est pas destinee. */
    private void controlerEtape(DemandeRecrutementPoste ligne, UtilisateurConnecte validateur) {
        if (!ligne.getStatut().estEnValidation()) {
            throw new BusinessException("LIGNE_NON_VALIDABLE",
                    "La ligne n'est pas en attente de validation (statut : " + ligne.getStatut() + ").");
        }
        EtapeValidation etapeAttendue = ligne.getEtapeCourante();
        if (etapeAttendue == null) {
            throw new BusinessException("ETAPE_INCONNUE", "Aucune etape de validation n'est definie pour cette ligne.");
        }
        EtapeValidation etapeDuRole = etapeDuRole(validateur);
        if (etapeDuRole == null || etapeDuRole != etapeAttendue) {
            throw BusinessException.forbidden(
                    "La ligne attend une validation " + etapeAttendue
                            + " : votre role (" + validateur.getRole().getNom() + ") ne permet pas de la traiter.");
        }
    }

    /** RM-005 : un utilisateur ne peut pas valider sa propre demande. */
    private void controlerSeparationDesResponsabilites(DemandeRecrutement demande, UtilisateurConnecte validateur) {
        if (demande.getIdUtilisateur().equals(validateur.getId())) {
            throw BusinessException.forbidden(
                    "Vous ne pouvez pas valider une demande dont vous etes le createur.");
        }
    }

    /** RM-040 : une meme etape ne peut pas etre validee deux fois sur un meme episode. */
    private void controlerDecisionUnique(DemandeRecrutementPoste ligne) {
        boolean dejaValide = validationRepository
                .findByDemandePosteIdAndEpisodeOrderByDateActionAsc(ligne.getId(), ligne.getEpisode()).stream()
                .anyMatch(validation -> validation.getEtape() == ligne.getEtapeCourante()
                        && validation.getDecision() == Decision.APPROUVE);
        if (dejaValide) {
            throw new BusinessException("VALIDATION_DEJA_FAITE",
                    "L'etape " + ligne.getEtapeCourante() + " a deja ete approuvee pour cette ligne.");
        }
    }

    /**
     * Evaluation du RH : les valeurs de la decision priment, a defaut on garde celles deja portees
     * par la ligne (cas d'une ligne revenue apres correction). Le salaire maximum, qui fait
     * office de budget, reste obligatoire.
     */
    private void definirEvaluationRh(DemandeRecrutementPoste ligne, ValidationDto.DecisionRequest decision) {
        BigDecimal salaireMin = decision.salaireMinEstime() != null
                ? decision.salaireMinEstime() : ligne.getSalaireMinEstime();
        BigDecimal salaireMax = decision.salaireMaxEstime() != null
                ? decision.salaireMaxEstime() : ligne.getSalaireMaxEstime();
        ligneRegles.appliquerEvaluationRh(ligne, salaireMin, salaireMax);
    }

    /** Finance et Direction ne peuvent statuer que sur une ligne dont le RH a defini le budget. */
    private void controlerBudgetDefini(DemandeRecrutementPoste ligne, EtapeValidation etape) {
        if (etape != EtapeValidation.RH && ligne.getBudgetEstime() == null) {
            throw new BusinessException("BUDGET_NON_DEFINI",
                    "Le RH n'a pas encore defini le budget de la ligne '" + ligne.getIntituleDemande()
                            + "' : l'etape " + etape + " ne peut pas statuer.");
        }
    }

    /** RM-054 : le montant verifie doit correspondre au montant reellement soumis. */
    private void controlerMontantVerifie(DemandeRecrutementPoste ligne, ValidationDto.DecisionRequest decision) {
        if (decision.montantVerifie() == null || ligne.getBudgetEstime() == null) {
            return;
        }
        if (decision.montantVerifie().compareTo(ligne.getBudgetEstime()) != 0) {
            throw new BusinessException("MONTANT_INCOHERENT",
                    "Le montant verifie (" + decision.montantVerifie() + ") ne correspond pas au montant soumis ("
                            + ligne.getBudgetEstime() + ").");
        }
    }

    /** Etape du circuit correspondant au role de l'utilisateur. */
    private EtapeValidation etapeDuRole(UtilisateurConnecte utilisateur) {
        try {
            return EtapeValidation.valueOf(utilisateur.getRoleNom());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private Validation construireDecision(DemandeRecrutementPoste ligne, UtilisateurConnecte validateur,
                                         EtapeValidation etape, ValidationDto.DecisionRequest decision) {
        Validation validation = new Validation();
        validation.setDemandePoste(ligne);
        validation.setIdUtilisateur(validateur.getId());
        validation.setEtape(etape);
        validation.setDecision(decision.decision());
        validation.setCommentaire(decision.commentaire());
        validation.setEpisode(ligne.getEpisode());
        return validation;
    }

    private void journaliser(DemandeRecrutement demande, DemandeRecrutementPoste ligne,
                              StatutLigne ancien, StatutLigne nouveau, UtilisateurConnecte auteur, String commentaire) {
        HistoriqueStatut journal = new HistoriqueStatut();
        journal.setDemande(demande);
        journal.setDemandePoste(ligne);
        journal.setAncienStatut(ancien.name());
        journal.setNouveauStatut(nouveau.name());
        journal.setIdUtilisateur(auteur.getId());
        journal.setCommentaire(commentaire);
        historiqueStatutRepository.save(journal);
    }

    private DemandeDto.LigneResume versResume(DemandeRecrutementPoste ligne) {
        return new DemandeDto.LigneResume(
                ligne.getId(),
                ligne.getDemande().getId(),
                ligne.getService().getId(),
                ligne.getService().getNom(),
                ligne.getPoste() == null ? null : ligne.getPoste().getId(),
                ligne.getPoste() == null ? null : ligne.getPoste().getIntitule(),
                ligne.getTypeBesoin(),
                ligne.getTypeRemplacement(),
                ligne.getStatut(),
                ligne.getEtapeCourante(),
                ligne.getIntituleDemande(),
                ligne.getEffectif(),
                ligne.getBudgetEstime(),
                ligne.getEpisode(),
                ligne.getDateModification());
    }
}
