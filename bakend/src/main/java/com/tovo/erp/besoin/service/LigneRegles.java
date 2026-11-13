package com.tovo.erp.besoin.service;

import com.tovo.erp.shared.BusinessException;
import com.tovo.erp.besoin.model.DemandeRecrutement;
import com.tovo.erp.besoin.model.DemandeRecrutementPoste;
import com.tovo.erp.besoin.model.Poste;
import com.tovo.erp.besoin.model.Service;
import com.tovo.erp.besoin.model.TypeBesoin;
import com.tovo.erp.besoin.model.TypeRemplacement;
import com.tovo.erp.besoin.repository.PosteRepository;
import com.tovo.erp.besoin.repository.ServiceRepository;
import com.tovo.erp.employe.dto.EmployeEmbaucheDto;
import com.tovo.erp.employe.service.EmployeService;
import com.tovo.erp.besoin.dto.DemandeDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Regles metier appliquees a une ligne de demande avant enregistrement.
 *
 * <p>Elles couvrent RM-016 a RM-030, RM-061 et RM-062. Les regles non expresses
 * ici sont garanties par des contraintes SQL (voir V1__bloc1_schema.sql).
 */
@org.springframework.stereotype.Service
public class LigneRegles {

    private final ServiceRepository serviceRepository;
    private final PosteRepository posteRepository;
    private final EmployeService employeService;

    public LigneRegles(ServiceRepository serviceRepository,
                       PosteRepository posteRepository,
                       EmployeService employeService) {
        this.serviceRepository = serviceRepository;
        this.posteRepository = posteRepository;
        this.employeService = employeService;
    }

    /** Construit une ligne de demande a partir des donnees saisies et verifie sa coherence. */
    public DemandeRecrutementPoste construire(DemandeRecrutement demande, DemandeDto.LigneCreation source) {
        DemandeRecrutementPoste ligne = new DemandeRecrutementPoste();
        ligne.setDemande(demande);
        ligne.setService(serviceExistant(source.idService()));

        ligne.setTypeBesoin(source.typeBesoin());
        ligne.setTypeRemplacement(source.typeRemplacement());
        ligne.setIntituleDemande(source.intituleDemande().trim());
        ligne.setDescriptionPoste(source.descriptionPoste());
        ligne.setProfilRecherche(source.profilRecherche());
        ligne.setEffectif(source.effectif());

        // Salaires et budget ne sont pas saisis par le manager : le RH les definit a sa validation.
        controlerEffectif(ligne);

        if (source.typeBesoin() == TypeBesoin.REMPLACEMENT) {
            controlerRemplacement(ligne, source);
        } else {
            controlerCreation(ligne, source);
        }

        controlerCoherenceService(ligne);
        return ligne;
    }

    /** Verifie les donnees modifiees d'une ligne (uniquement pendant la phase brouillon). */
    public void controlerModification(DemandeRecrutementPoste ligne, DemandeDto.LigneModification source) {
        ligne.setIntituleDemande(source.intituleDemande().trim());
        ligne.setDescriptionPoste(source.descriptionPoste());
        ligne.setProfilRecherche(source.profilRecherche());
        ligne.setEffectif(source.effectif());

        controlerEffectif(ligne);

        if (ligne.getTypeBesoin() == TypeBesoin.REMPLACEMENT) {
            ligne.setMotifRemplacement(source.motifRemplacement());
            ligne.setDateDebutRemplacement(source.dateDebutRemplacement());
            ligne.setDateFinRemplacement(source.dateFinRemplacement());
            controlerDatesRemplacement(ligne.getTypeRemplacement(),
                    ligne.getDateDebutRemplacement(), ligne.getDateFinRemplacement(),
                    ligne.getMotifRemplacement());
        }

        controlerCoherenceService(ligne);
    }

    /**
     * Applique l'evaluation du RH sur une ligne : fourchette salariale (RM-017, RM-018).
     *
     * <p>Le salaire maximum est obligatoire et fait office de budget de la ligne. Le salaire
     * minimum est facultatif mais doit rester coherent avec le maximum.
     */
    public void appliquerEvaluationRh(DemandeRecrutementPoste ligne, BigDecimal salaireMin,
                                      BigDecimal salaireMax) {
        if (salaireMax == null) {
            throw new BusinessException("SALAIRE_MAX_RH_MANQUANT",
                    "Le RH doit definir le salaire maximum du profil '" + ligne.getIntituleDemande()
                            + "' (il fait office de budget) avant de l'approuver.");
        }
        ligne.setSalaireMinEstime(salaireMin);
        ligne.setSalaireMaxEstime(salaireMax);
        ligne.setBudgetEstime(salaireMax);
        controlerSalaires(ligne);
        controlerBudget(ligne);
    }

    // ------------------------------------------------------------------
    // Regles internes
    // ------------------------------------------------------------------

    private Service serviceExistant(Long idService) {
        return serviceRepository.findById(idService)
                .orElseThrow(() -> BusinessException.notFound("Le service " + idService + " n'existe pas."));
    }

    /** RM-016. */
    private void controlerEffectif(DemandeRecrutementPoste ligne) {
        if (ligne.getEffectif() == null || ligne.getEffectif() <= 0) {
            throw new BusinessException("EFFECTIF_INVALIDE",
                    "L'effectif doit etre strictement superieur a zero.");
        }
    }

    /** RM-018. */
    private void controlerBudget(DemandeRecrutementPoste ligne) {
        if (ligne.getBudgetEstime() == null || ligne.getBudgetEstime().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("BUDGET_INVALIDE",
                    "Le budget estime doit etre positif ou nul.");
        }
    }

    /** RM-017 : les salaires sont positifs et coherents entre eux. */
    private void controlerSalaires(DemandeRecrutementPoste ligne) {
        BigDecimal min = ligne.getSalaireMinEstime();
        BigDecimal max = ligne.getSalaireMaxEstime();
        if (min != null && min.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("SALAIRE_INVALIDE",
                    "Le salaire minimum estime doit etre positif ou nul.");
        }
        if (max != null && max.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("SALAIRE_INVALIDE",
                    "Le salaire maximum estime doit etre positif ou nul.");
        }
        if (min != null && max != null && max.compareTo(min) < 0) {
            throw new BusinessException("SALAIRE_INVALIDE",
                    "Le salaire maximum estime doit etre superieur ou egal au salaire minimum estime.");
        }
    }

    /** RM-022, RM-007, RM-008 : creation de poste ou d'effectif supplementaire. */
    private void controlerCreation(DemandeRecrutementPoste ligne, DemandeDto.LigneCreation source) {
        if (source.typeRemplacement() != null) {
            throw new BusinessException("TYPE_BESOIN_INVALIDE",
                    "Une creation de poste ne peut pas porter de type de remplacement.");
        }
        if (source.idEmployeRemplace() != null) {
            throw new BusinessException("TYPE_BESOIN_INVALIDE",
                    "Une creation de poste ne peut pas referencer un employe remplace.");
        }
        if (source.idPoste() != null) {
            ligne.setPoste(posteDuService(source.idPoste(), ligne.getService()));
        }
    }

    /** RM-023 a RM-030 : remplacement d'un employe. */
    private void controlerRemplacement(DemandeRecrutementPoste ligne, DemandeDto.LigneCreation source) {
        if (source.typeRemplacement() == null) {
            throw new BusinessException("TYPE_REMPLACEMENT_MANQUANT",
                    "Un remplacement doit preciser son type (TEMPORAIRE ou DEFINITIF).");
        }

        EmployeEmbaucheDto employeRemplace = null;
        if (source.idEmployeRemplace() != null) {
            employeRemplace = employeService.dossier(source.idEmployeRemplace());
            controlerStatutEmployeRemplace(employeRemplace, source.typeRemplacement());
            ligne.setIdEmployeRemplace(employeRemplace.id());
        } else if (source.motifRemplacement() == null || source.motifRemplacement().isBlank()) {
            // RM-026 : si l'employe n'est pas identifie, le motif doit expliquer le remplacement.
            throw new BusinessException("MOTIF_REMPLACEMENT_MANQUANT",
                    "L'employe a remplacer n'etant pas identifie, le motif du remplacement est obligatoire.");
        }

        ligne.setMotifRemplacement(source.motifRemplacement());
        ligne.setDateDebutRemplacement(source.dateDebutRemplacement());
        ligne.setDateFinRemplacement(source.dateFinRemplacement());
        controlerDatesRemplacement(source.typeRemplacement(),
                source.dateDebutRemplacement(), source.dateFinRemplacement(), source.motifRemplacement());

        // RM-030 : si l'employe occupe un poste, le poste officiel est repris.
        if (employeRemplace != null && employeRemplace.idPoste() != null) {
            ligne.setPoste(posteDuService(employeRemplace.idPoste(), ligne.getService()));
        } else if (source.idPoste() != null) {
            ligne.setPoste(posteDuService(source.idPoste(), ligne.getService()));
        }
    }

    /** RM-027 : l'employe remplace doit etre coherent avec le scenario. */
    private void controlerStatutEmployeRemplace(EmployeEmbaucheDto employe, TypeRemplacement typeRemplacement) {
        if (typeRemplacement == TypeRemplacement.TEMPORAIRE && !"ACTIF".equals(employe.statut())) {
            throw new BusinessException("EMPLOYE_NON_VALIDE",
                    "Un remplacement temporaire concerne un employe actif (statut actuel : "
                            + employe.statut() + ").");
        }
        if (typeRemplacement == TypeRemplacement.DEFINITIF && "INACTIF".equals(employe.statut())) {
            throw new BusinessException("EMPLOYE_NON_VALIDE",
                    "Un remplacement definitif concerne un employe actif ou parti (statut actuel : "
                            + employe.statut() + ").");
        }
    }

    /** RM-028, RM-029, RM-062 : dates et motif du remplacement. */
    private void controlerDatesRemplacement(TypeRemplacement typeRemplacement,
                                            LocalDate debut, LocalDate fin, String motif) {
        if (typeRemplacement == TypeRemplacement.TEMPORAIRE) {
            if (debut == null) {
                throw new BusinessException("DATE_REMPLACEMENT_MANQUANTE",
                        "Un remplacement temporaire exige une date de debut.");
            }
            if (fin == null) {
                throw new BusinessException("DATE_REMPLACEMENT_MANQUANTE",
                        "Un remplacement temporaire exige une date de fin.");
            }
            if (fin.isBefore(debut)) {
                throw new BusinessException("DATES_REMPLACEMENT_INVALIDES",
                        "La date de fin du remplacement ne peut pas preceder la date de debut.");
            }
        }
        if (typeRemplacement == TypeRemplacement.DEFINITIF
                && (motif == null || motif.isBlank())) {
            throw new BusinessException("MOTIF_REMPLACEMENT_MANQUANT",
                    "Le motif du remplacement definitif est obligatoire.");
        }
    }

    /** RM-061 : le service de la ligne doit correspondre au service du poste reference. */
    private void controlerCoherenceService(DemandeRecrutementPoste ligne) {
        if (ligne.getPoste() == null) {
            return;
        }
        if (!ligne.getPoste().getService().getId().equals(ligne.getService().getId())) {
            throw new BusinessException("SERVICE_INCOHERENT",
                    "Le poste '" + ligne.getPoste().getIntitule() + "' n'appartient pas au service "
                            + ligne.getService().getNom() + ".");
        }
    }

    /** Le poste reference doit exister et appartenir au service de la ligne. */
    private Poste posteDuService(Long idPoste, Service service) {
        Poste poste = posteRepository.findById(idPoste)
                .orElseThrow(() -> BusinessException.notFound("Le poste " + idPoste + " n'existe pas."));
        if (!poste.getService().getId().equals(service.getId())) {
            throw new BusinessException("SERVICE_INCOHERENT",
                    "Le poste '" + poste.getIntitule() + "' n'appartient pas au service " + service.getNom() + ".");
        }
        return poste;
    }
}
