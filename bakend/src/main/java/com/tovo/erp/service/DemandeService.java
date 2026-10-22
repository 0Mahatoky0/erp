package com.tovo.erp.service;

import com.tovo.erp.common.BusinessException;
import com.tovo.erp.common.Recherche;
import com.tovo.erp.config.CircuitValidationProperties;
import com.tovo.erp.config.DemandeProperties;
import com.tovo.erp.domain.entity.DemandeRecrutement;
import com.tovo.erp.domain.entity.DemandeRecrutementPoste;
import com.tovo.erp.domain.entity.HistoriqueStatut;
import com.tovo.erp.domain.entity.Utilisateur;
import com.tovo.erp.domain.enums.EtapeValidation;
import com.tovo.erp.domain.enums.StatutDemande;
import com.tovo.erp.domain.enums.StatutLigne;
import com.tovo.erp.domain.enums.TypeBesoin;
import com.tovo.erp.repository.DemandeRecrutementPosteRepository;
import com.tovo.erp.repository.DemandeRecrutementRepository;
import com.tovo.erp.repository.HistoriqueStatutRepository;
import com.tovo.erp.repository.ValidationRepository;
import com.tovo.erp.repository.VerificationBudgetRepository;
import com.tovo.erp.web.dto.DemandeDto;
import com.tovo.erp.web.dto.ValidationDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Cycle de vie des demandes de recrutement et de leurs lignes (RM-012 a RM-031, RM-041 a RM-047).
 *
 * <p>Une demande est un ensemble de lignes valides separement. Le statut global de la
 * demande est toujours recalcule par {@link StatutService} a partir des lignes.
 */
@Service
public class DemandeService {

    private final DemandeRecrutementRepository demandeRepository;
    private final DemandeRecrutementPosteRepository ligneRepository;
    private final ValidationRepository validationRepository;
    private final VerificationBudgetRepository verificationBudgetRepository;
    private final HistoriqueStatutRepository historiqueStatutRepository;
    private final LigneRegles ligneRegles;
    private final StatutService statutService;
    private final CircuitValidationProperties circuitProperties;
    private final DemandeProperties demandeProperties;

    public DemandeService(DemandeRecrutementRepository demandeRepository,
                          DemandeRecrutementPosteRepository ligneRepository,
                          ValidationRepository validationRepository,
                          VerificationBudgetRepository verificationBudgetRepository,
                          HistoriqueStatutRepository historiqueStatutRepository,
                          LigneRegles ligneRegles,
                          StatutService statutService,
                          CircuitValidationProperties circuitProperties,
                          DemandeProperties demandeProperties) {
        this.demandeRepository = demandeRepository;
        this.ligneRepository = ligneRepository;
        this.validationRepository = validationRepository;
        this.verificationBudgetRepository = verificationBudgetRepository;
        this.historiqueStatutRepository = historiqueStatutRepository;
        this.ligneRegles = ligneRegles;
        this.statutService = statutService;
        this.circuitProperties = circuitProperties;
        this.demandeProperties = demandeProperties;
    }

    // ------------------------------------------------------------------
    // Consultation
    // ------------------------------------------------------------------

    /**
     * Liste des demandes visibles par l'utilisateur (RM-067).
     * Un MANAGER ne voit que ses propres demandes, les autres roles voient le perimetre complet.
     */
    @Transactional(readOnly = true)
    public List<DemandeDto.Resume> lister(Utilisateur lecteur, StatutDemande statut, String recherche,
                                          Boolean mesDemandes) {
        boolean uniquementLesMiennes = Boolean.TRUE.equals(mesDemandes)
                || "MANAGER".equals(lecteur.getRole().getNom());
        List<DemandeRecrutement> demandes = demandeRepository.rechercher(
                lecteur.getId(), uniquementLesMiennes, statut, Recherche.motif(recherche));
        return demandes.stream()
                .map(demande -> DemandeDto.versResume(demande, ligneRepository.findByDemandeIdOrderById(demande.getId())))
                .toList();
    }

    /** Detail complet d'une demande : lignes, historique de decisions et journal des statuts. */
    @Transactional(readOnly = true)
    public DemandeDto.Detail detail(Long idDemande, Utilisateur lecteur) {
        DemandeRecrutement demande = demandeExistant(idDemande);
        controlerAccesLecture(demande, lecteur);
        List<DemandeRecrutementPoste> lignes = ligneRepository.findParDemande(idDemande);
        return new DemandeDto.Detail(
                demande.getId(),
                demande.getMotif(),
                demande.getDateSouhaitee(),
                demande.getStatut(),
                demande.getDateCreation(),
                demande.getDateModification(),
                demande.getDateSoumission(),
                demande.getUtilisateur().getNom(),
                demande.getUtilisateur().getId(),
                lignes.stream().map(this::versLigneDetail).toList(),
                historiqueStatutRepository.findByDemandeIdOrderByDateActionAsc(idDemande).stream()
                        .map(ValidationDto.ChangementStatut::de)
                        .toList());
    }

    // ------------------------------------------------------------------
    // Creation et modification
    // ------------------------------------------------------------------

    /** Cree une demande a l'etat BROUILLON avec ses lignes (RM-012, RM-015). */
    @Transactional
    public DemandeDto.Resume creer(Utilisateur auteur, DemandeDto.Creation demande) {
        controlerDateSouhaitee(demande.dateSouhaitee());
        if (demande.lignes() == null || demande.lignes().isEmpty()) {
            throw new BusinessException("DEMANDE_SANS_LIGNE",
                    "Une demande doit contenir au moins une ligne.");
        }

        DemandeRecrutement entite = new DemandeRecrutement();
        entite.setUtilisateur(auteur);
        entite.setMotif(demande.motif().trim());
        entite.setDateSouhaitee(demande.dateSouhaitee());
        entite.setStatut(StatutDemande.BROUILLON);
        demandeRepository.save(entite);

        for (DemandeDto.LigneCreation ligne : demande.lignes()) {
            DemandeRecrutementPoste nouvelle = ligneRegles.construire(entite, ligne);
            ligneRepository.save(nouvelle);
        }
        return DemandeDto.versResume(entite, lignesDe(entite));
    }

    /** Modifie l'entete d'une demande tant qu'elle est en brouillon (RM-019). */
    @Transactional
    public DemandeDto.Resume modifierEntete(Long idDemande, Utilisateur auteur, DemandeDto.Modification demande) {
        DemandeRecrutement entite = demandeExistant(idDemande);
        controlerProprietaire(entite, auteur);
        controlerBrouillon(entite);
        controlerDateSouhaitee(demande.dateSouhaitee());

        entite.setMotif(demande.motif().trim());
        entite.setDateSouhaitee(demande.dateSouhaitee());
        entite.setDateModification(LocalDateTime.now());
        demandeRepository.save(entite);
        return DemandeDto.versResume(entite, lignesDe(entite));
    }

    /** Ajoute une ligne a une demande encore en brouillon (RM-015, RM-019). */
    @Transactional
    public DemandeDto.LigneResume ajouterLigne(Long idDemande, Utilisateur auteur,
                                               DemandeDto.LigneCreation ligne) {
        DemandeRecrutement entite = demandeExistant(idDemande);
        controlerProprietaire(entite, auteur);
        controlerBrouillon(entite);

        DemandeRecrutementPoste nouvelle = ligneRegles.construire(entite, ligne);
        ligneRepository.save(nouvelle);
        entite.setDateModification(LocalDateTime.now());
        demandeRepository.save(entite);
        return versLigneResume(nouvelle);
    }

    /**
     * Modifie une ligne (RM-019, RM-021).
     *
     * <p>Tant que la demande est un brouillon, son createur peut modifier ses lignes.
     * Une demande deja soumise n'est plus modifiee silencieusement : seule une ligne
     * en attente de correction (A_CORRIGER) peut etre corrigee par son demandeur.
     */
    @Transactional
    public DemandeDto.LigneResume modifierLigne(Long idDemande, Long idLigne, Utilisateur auteur,
                                                DemandeDto.LigneModification modification) {
        DemandeRecrutement entite = demandeExistant(idDemande);
        controlerProprietaire(entite, auteur);
        DemandeRecrutementPoste ligne = ligneExistant(idDemande, idLigne);
        controlerLigneModifiable(entite, ligne);

        ligneRegles.controlerModification(ligne, modification);
        ligne.setDateModification(LocalDateTime.now());
        ligneRepository.save(ligne);
        entite.setDateModification(LocalDateTime.now());
        demandeRepository.save(entite);
        return versLigneResume(ligne);
    }

    /** Ligne modifiable : brouillon, ou attente de correction sur une demande soumise. */
    private void controlerLigneModifiable(DemandeRecrutement demande, DemandeRecrutementPoste ligne) {
        if (demande.getStatut() == StatutDemande.BROUILLON) {
            if (ligne.getStatut() != StatutLigne.BROUILLON) {
                throw new BusinessException("LIGNE_NON_MODIFIABLE",
                        "Seules les lignes en brouillon peuvent etre modifiees.");
            }
            return;
        }
        if (ligne.getStatut() != StatutLigne.A_CORRIGER) {
            throw new BusinessException("LIGNE_NON_MODIFIABLE",
                    "La demande a ete soumise : seule une ligne en attente de correction (A_CORRIGER) "
                            + "peut etre modifiee, par son demandeur.");
        }
    }

    /** Supprime une ligne d'une demande en brouillon. */
    @Transactional
    public void supprimerLigne(Long idDemande, Long idLigne, Utilisateur auteur) {
        DemandeRecrutement entite = demandeExistant(idDemande);
        controlerProprietaire(entite, auteur);
        controlerBrouillon(entite);
        DemandeRecrutementPoste ligne = ligneExistant(idDemande, idLigne);
        ligneRepository.delete(ligne);
        entite.setDateModification(LocalDateTime.now());
        demandeRepository.save(entite);
    }

    // ------------------------------------------------------------------
    // Soumission, resoumission, annulation
    // ------------------------------------------------------------------

    /**
     * Soumet la demande : toutes les lignes passent a la premiere etape du circuit (RM-020).
     * La demande ne peut etre soumise que si elle contient au moins une ligne complete.
     */
    @Transactional
    public DemandeDto.Resume soumettre(Long idDemande, Utilisateur auteur) {
        DemandeRecrutement entite = demandeExistant(idDemande);
        controlerProprietaire(entite, auteur);
        controlerBrouillon(entite);

        List<DemandeRecrutementPoste> lignes = lignesDe(entite);
        if (lignes.isEmpty()) {
            throw new BusinessException("DEMANDE_SANS_LIGNE",
                    "Une demande ne peut pas etre soumise sans au moins une ligne.");
        }
        for (DemandeRecrutementPoste ligne : lignes) {
            controlerLigneComplete(ligne);
        }

        EtapeValidation premiereEtape = premiereEtape();
        LocalDateTime maintenant = LocalDateTime.now();
        for (DemandeRecrutementPoste ligne : lignes) {
            StatutLigne ancien = ligne.getStatut();
            ligne.setStatut(statutPourEtape(premiereEtape));
            ligne.setEtapeCourante(premiereEtape);
            ligne.setDateSoumission(maintenant);
            ligne.setDateModification(maintenant);
            ligneRepository.save(ligne);
            journaliser(entite, ligne, ancien, ligne.getStatut(), auteur, "Soumission de la demande.");
        }

        entite.setDateSoumission(maintenant);
        entite.setDateModification(maintenant);
        demandeRepository.save(entite);
        statutService.recalculer(entite, lignes, auteur, "Soumission de la demande.");
        return DemandeDto.versResume(entite, lignes);
    }

    /**
     * Resoumet une ligne apres correction (RM-038).
     * Le circuit reprend a l'etape qui avait demande la correction et un nouvel episode
     * de validation est ouvert (RM-040).
     */
    @Transactional
    public DemandeDto.Resume resoumettreLigne(Long idDemande, Long idLigne, Utilisateur auteur) {
        DemandeRecrutement entite = demandeExistant(idDemande);
        controlerProprietaire(entite, auteur);
        DemandeRecrutementPoste ligne = ligneExistant(idDemande, idLigne);

        if (ligne.getStatut() != StatutLigne.A_CORRIGER) {
            throw new BusinessException("LIGNE_NON_RESOUMISSABLE",
                    "Seule une ligne en attente de correction peut etre resoumise.");
        }
        controlerLigneComplete(ligne);

        EtapeValidation reprise = ligne.getEtapeCourante() == null ? premiereEtape() : ligne.getEtapeCourante();
        StatutLigne ancien = ligne.getStatut();
        ligne.setEpisode(ligne.getEpisode() + 1);
        ligne.setStatut(statutPourEtape(reprise));
        ligne.setEtapeCourante(reprise);
        ligne.setDateSoumission(LocalDateTime.now());
        ligne.setDateModification(LocalDateTime.now());
        ligneRepository.save(ligne);
        journaliser(entite, ligne, ancien, ligne.getStatut(), auteur,
                "Resoumission apres correction (episode " + ligne.getEpisode() + ").");

        List<DemandeRecrutementPoste> lignes = lignesDe(entite);
        statutService.recalculer(entite, lignes, auteur, "Resoumission d'une ligne.");
        return DemandeDto.versResume(entite, lignes);
    }

    /** Annule une ligne de la demande. */
    @Transactional
    public DemandeDto.Resume annulerLigne(Long idDemande, Long idLigne, Utilisateur auteur) {
        DemandeRecrutement entite = demandeExistant(idDemande);
        controlerProprietaire(entite, auteur);
        DemandeRecrutementPoste ligne = ligneExistant(idDemande, idLigne);

        if (ligne.getStatut() == StatutLigne.ANNULEE || ligne.getStatut() == StatutLigne.REFUSEE) {
            throw new BusinessException("LIGNE_DEJA_TERMINEE",
                    "Cette ligne est deja terminee et ne peut plus etre annulee.");
        }

        StatutLigne ancien = ligne.getStatut();
        ligne.setStatut(StatutLigne.ANNULEE);
        ligne.setEtapeCourante(null);
        ligne.setDateModification(LocalDateTime.now());
        ligneRepository.save(ligne);
        journaliser(entite, ligne, ancien, StatutLigne.ANNULEE, auteur, "Annulation de la ligne.");

        List<DemandeRecrutementPoste> lignes = lignesDe(entite);
        statutService.recalculer(entite, lignes, auteur, "Annulation d'une ligne.");
        return DemandeDto.versResume(entite, lignes);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Statut d'une ligne correspondant a une etape du circuit. */
    public StatutLigne statutPourEtape(EtapeValidation etape) {
        return switch (etape) {
            case RH -> StatutLigne.EN_ATTENTE_RH;
            case FINANCE -> StatutLigne.EN_ATTENTE_FINANCE;
            case DIRECTION -> StatutLigne.EN_ATTENTE_DIRECTION;
        };
    }

    /** Premiere etape du circuit configuree. */
    public EtapeValidation premiereEtape() {
        List<EtapeValidation> etapes = circuitProperties.etapesEnum();
        return etapes.isEmpty() ? EtapeValidation.RH : etapes.get(0);
    }

    public DemandeRecrutement demandeExistant(Long idDemande) {
        return demandeRepository.findById(idDemande)
                .orElseThrow(() -> BusinessException.notFound("La demande " + idDemande + " n'existe pas."));
    }

    public DemandeRecrutementPoste ligneExistant(Long idDemande, Long idLigne) {
        return ligneRepository.findById(idLigne)
                .filter(ligne -> ligne.getDemande().getId().equals(idDemande))
                .orElseThrow(() -> BusinessException.notFound(
                        "La ligne " + idLigne + " n'appartient pas a la demande " + idDemande + "."));
    }

    private List<DemandeRecrutementPoste> lignesDe(DemandeRecrutement demande) {
        return ligneRepository.findByDemandeIdOrderById(demande.getId());
    }

    /** Le createur d'une demande peut seul la modifier (RM-067). */
    private void controlerProprietaire(DemandeRecrutement demande, Utilisateur auteur) {
        if (!demande.getUtilisateur().getId().equals(auteur.getId())) {
            throw BusinessException.forbidden(
                    "Seul le createur de la demande peut la modifier.");
        }
    }

    private void controlerBrouillon(DemandeRecrutement demande) {
        if (demande.getStatut() != StatutDemande.BROUILLON || demande.getDateSoumission() != null) {
            throw new BusinessException("DEMANDE_NON_MODIFIABLE",
                    "La demande a ete soumise : elle ne peut plus etre modifiee directement.");
        }
    }

    /** RM-067 : un MANAGER ne consulte que ses propres demandes. */
    private void controlerAccesLecture(DemandeRecrutement demande, Utilisateur lecteur) {
        if ("MANAGER".equals(lecteur.getRole().getNom())
                && !demande.getUtilisateur().getId().equals(lecteur.getId())) {
            throw BusinessException.forbidden("Cette demande ne fait pas partie de votre perimetre.");
        }
    }

    /** RM-020 : une ligne soumise doit etre complete. */
    private void controlerLigneComplete(DemandeRecrutementPoste ligne) {
        if (ligne.getIntituleDemande() == null || ligne.getIntituleDemande().isBlank()) {
            throw new BusinessException("LIGNE_INCOMPLETE",
                    "Une ligne doit porter un intitule de poste.");
        }
        if (ligne.getEffectif() == null || ligne.getEffectif() <= 0) {
            throw new BusinessException("LIGNE_INCOMPLETE",
                    "L'effectif de la ligne '" + ligne.getIntituleDemande() + "' doit etre superieur a zero.");
        }
        if (ligne.getBudgetEstime() == null || ligne.getBudgetEstime().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new BusinessException("LIGNE_INCOMPLETE",
                    "Le budget estime de la ligne '" + ligne.getIntituleDemande() + "' est invalide.");
        }
        if (ligne.getTypeBesoin() == TypeBesoin.REMPLACEMENT) {
            if (ligne.getTypeRemplacement() == null) {
                throw new BusinessException("LIGNE_INCOMPLETE",
                        "La ligne '" + ligne.getIntituleDemande() + "' doit preciser le type de remplacement.");
            }
            if (ligne.getEmployeRemplace() == null
                    && (ligne.getMotifRemplacement() == null || ligne.getMotifRemplacement().isBlank())) {
                throw new BusinessException("LIGNE_INCOMPLETE",
                        "La ligne '" + ligne.getIntituleDemande() + "' doit indiquer l'employe remplace "
                                + "ou expliquer le remplacement.");
            }
        }
    }

    /** RM-014 : la date souhaitee respecte le delai de planification de l'entreprise. */
    private void controlerDateSouhaitee(LocalDate dateSouhaitee) {
        LocalDate minimum = LocalDate.now().plus(demandeProperties.delaiMinimum());
        if (dateSouhaitee.isBefore(minimum)) {
            throw new BusinessException("DATE_SOUHAITEE_INVALIDE",
                    "La date souhaitee de recrutement ne peut pas etre anterieure au " + minimum + ".");
        }
    }

    private void journaliser(DemandeRecrutement demande, DemandeRecrutementPoste ligne,
                              StatutLigne ancien, StatutLigne nouveau, Utilisateur auteur, String commentaire) {
        HistoriqueStatut journal = new HistoriqueStatut();
        journal.setDemande(demande);
        journal.setDemandePoste(ligne);
        journal.setAncienStatut(ancien.name());
        journal.setNouveauStatut(nouveau.name());
        journal.setUtilisateur(auteur);
        journal.setCommentaire(commentaire);
        historiqueStatutRepository.save(journal);
    }

    private DemandeDto.LigneResume versLigneResume(DemandeRecrutementPoste ligne) {
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

    private DemandeDto.LigneDetail versLigneDetail(DemandeRecrutementPoste ligne) {
        return new DemandeDto.LigneDetail(
                ligne.getId(),
                ligne.getDemande().getId(),
                ligne.getService().getId(),
                ligne.getService().getNom(),
                ligne.getService().getDepartement().getNom(),
                ligne.getPoste() == null ? null : ligne.getPoste().getId(),
                ligne.getPoste() == null ? null : ligne.getPoste().getIntitule(),
                ligne.getTypeBesoin(),
                ligne.getTypeRemplacement(),
                ligne.getEmployeRemplace() == null ? null : ligne.getEmployeRemplace().getId(),
                ligne.getEmployeRemplace() == null ? null
                        : ligne.getEmployeRemplace().getPrenom() + " " + ligne.getEmployeRemplace().getNom(),
                ligne.getEmployeRemplace() == null ? null : ligne.getEmployeRemplace().getMatricule(),
                ligne.getMotifRemplacement(),
                ligne.getDateDebutRemplacement(),
                ligne.getDateFinRemplacement(),
                ligne.getIntituleDemande(),
                ligne.getDescriptionPoste(),
                ligne.getProfilRecherche(),
                ligne.getSalaireMinEstime(),
                ligne.getSalaireMaxEstime(),
                ligne.getEffectif(),
                ligne.getBudgetEstime(),
                ligne.getStatut(),
                ligne.getEtapeCourante(),
                ligne.getEpisode(),
                ligne.getDateCreation(),
                ligne.getDateModification(),
                ligne.getDateSoumission(),
                validationRepository.findByDemandePosteIdOrderByDateActionAsc(ligne.getId()).stream()
                        .map(ValidationDto.Historique::de)
                        .toList(),
                verificationBudgetRepository.findByDemandePosteIdOrderByDateVerificationAsc(ligne.getId()).stream()
                        .map(ValidationDto.VerificationBudgetaire::de)
                        .toList());
    }
}
