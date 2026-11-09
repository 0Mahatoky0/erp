package com.tovo.erp.contrat.service;

import com.tovo.erp.besoin.dto.PosteDto;
import com.tovo.erp.besoin.service.PosteService;
import com.tovo.erp.contrat.dto.ContratDtos;
import com.tovo.erp.contrat.model.Affectation;
import com.tovo.erp.contrat.model.CategorieTache;
import com.tovo.erp.contrat.model.Contrat;
import com.tovo.erp.contrat.model.Onboarding;
import com.tovo.erp.contrat.model.OnboardingModele;
import com.tovo.erp.contrat.model.OnboardingModeleTache;
import com.tovo.erp.contrat.model.OnboardingStatut;
import com.tovo.erp.contrat.model.OnboardingTache;
import com.tovo.erp.contrat.model.TacheStatut;
import com.tovo.erp.contrat.repository.AffectationRepository;
import com.tovo.erp.contrat.repository.OnboardingModeleRepository;
import com.tovo.erp.contrat.repository.OnboardingModeleTacheRepository;
import com.tovo.erp.contrat.repository.OnboardingRepository;
import com.tovo.erp.contrat.repository.OnboardingTacheRepository;
import com.tovo.erp.shared.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Parcours d'integration du salarie (section 6.4).
 * <p>Le parcours est cree a la signature du contrat (RG-B4-08). La progression
 * est recalculee a chaque mise a jour ; la validation RH est bloquee tant que
 * les taches bloquantes ne sont pas faites (RG-B4-09).
 */
@Service
public class OnboardingService {

    /** Tache standard du parcours d'integration. */
    private record ModeleTache(String code, String libelle, CategorieTache categorie,
                               String responsable, int offsetJours, boolean obligatoire,
                               boolean bloquante, String dependDe) {
    }

    private static final List<ModeleTache> MODELE = List.of(
            new ModeleTache("ADMIN_CONTRAT_SIGNE", "Contrat signe par les deux parties",
                    CategorieTache.ADMIN, "RH", -3, true, true, null),
            new ModeleTache("ADMIN_DOSSIER", "Dossier administratif complet",
                    CategorieTache.ADMIN, "RH / Salarie", 7, true, true, null),
            new ModeleTache("ADMIN_VISITE_MEDICALE", "Visite medicale d'embauche",
                    CategorieTache.ADMIN, "RH", 15, true, false, "ADMIN_DOSSIER"),
            new ModeleTache("PAIE_INSCRIPTION", "Inscription a la paie",
                    CategorieTache.PAIE, "Paie", -2, true, true, "ADMIN_CONTRAT_SIGNE"),
            new ModeleTache("IT_COMPTE_MESSAGERIE", "Creation du compte et de la messagerie",
                    CategorieTache.IT, "IT", -2, true, true, null),
            new ModeleTache("MATERIEL_POSTE", "Ordinateur, telephone et fournitures",
                    CategorieTache.MATERIEL, "Services generaux / IT", -1, true, false,
                    "IT_COMPTE_MESSAGERIE"),
            new ModeleTache("ACCES_BADGE", "Badge, acces et enrolement du pointage",
                    CategorieTache.ACCES, "Securite", -1, true, true, "IT_COMPTE_MESSAGERIE"),
            new ModeleTache("FORMATION_ACCUEIL", "Formation d'accueil et de securite",
                    CategorieTache.FORMATION, "RH / Manager", 3, false, false,
                    "ADMIN_CONTRAT_SIGNE"),
            new ModeleTache("FORMATION_REGLEMENT", "Reglement interieur et accuse de reception",
                    CategorieTache.FORMATION, "RH", 1, true, true, "ADMIN_CONTRAT_SIGNE"),
            new ModeleTache("ADMIN_PRESENTATION_EQUIPE", "Presentation de l'equipe et parrain",
                    CategorieTache.ADMIN, "Manager", 1, false, false, "ADMIN_CONTRAT_SIGNE"),
            new ModeleTache("ADMIN_POINT_ESSAI", "Point de fin de periode d'essai",
                    CategorieTache.ADMIN, "Manager", 30, false, false, null));

    private final OnboardingRepository onboardings;
    private final OnboardingTacheRepository taches;
    private final OnboardingModeleRepository modeles;
    private final OnboardingModeleTacheRepository tachesModeles;
    private final AffectationRepository affectations;
    private final PosteService postes;
    private final ContratProperties properties;

    public OnboardingService(OnboardingRepository onboardings,
                             OnboardingTacheRepository taches,
                             OnboardingModeleRepository modeles,
                             OnboardingModeleTacheRepository tachesModeles,
                             AffectationRepository affectations,
                             PosteService postes,
                             ContratProperties properties) {
        this.onboardings = onboardings;
        this.taches = taches;
        this.modeles = modeles;
        this.tachesModeles = tachesModeles;
        this.affectations = affectations;
        this.postes = postes;
        this.properties = properties;
    }

    /** Cree le parcours et sa checklist a la signature du contrat (RG-B4-08). */
    @Transactional
    Onboarding creerPour(Contrat contrat, Long idResponsableRh) {
        return onboardings.findFirstByIdContrat(contrat.getId())
                .orElseGet(() -> creer(contrat, idResponsableRh));
    }

    private Onboarding creer(Contrat contrat, Long idResponsableRh) {
        LocalDate dateDebut = contrat.getDateDebut() == null ? LocalDate.now() : contrat.getDateDebut();
        Onboarding onboarding = new Onboarding();
        OnboardingModele modele = choisirModele(contrat);
        List<ModeleTache> tachesModele = modele == null
                ? MODELE : tachesDuModele(modele);

        onboarding.setIdEmploye(contrat.getIdEmploye());
        onboarding.setIdContrat(contrat.getId());
        onboarding.setTemplateId(modele == null ? null : modele.getId());
        onboarding.setTemplateCode(modele == null ? "STANDARD" : modele.getCode());
        onboarding.setTypeContrat(contrat.getTypeContrat() == null
                ? null : contrat.getTypeContrat().name());
        onboarding.setSite(siteDe(contrat));
        onboarding.setDateDebut(dateDebut);
        onboarding.setDateCible(dateDebut.plusDays(properties.onboardingJours()));
        onboarding.setIdResponsableRh(idResponsableRh);
        onboarding.setStatut(OnboardingStatut.A_FAIRE);
        onboarding.setProgression(0);
        onboardings.save(onboarding);

        java.util.Map<String, Long> ids = new java.util.HashMap<>();
        for (ModeleTache definition : tachesModele) {
            OnboardingTache tache = new OnboardingTache();
            tache.setOnboarding(onboarding);
            tache.setCode(definition.code());
            tache.setLibelle(definition.libelle());
            tache.setCategorie(definition.categorie());
            tache.setResponsable(definition.responsable());
            tache.setEcheanceOffsetJours(definition.offsetJours());
            tache.setEcheance(dateDebut.plusDays(definition.offsetJours()));
            tache.setObligatoire(definition.obligatoire());
            tache.setBloquante(definition.bloquante());
            tache.setIdDependDe(definition.dependDe() == null
                    ? null : ids.get(definition.dependDe()));
            if ("ADMIN_CONTRAT_SIGNE".equals(definition.code())) {
                tache.setStatut(TacheStatut.FAIT);
                tache.setDateFait(LocalDate.now());
            } else {
                tache.setStatut(TacheStatut.A_FAIRE);
            }
            taches.save(tache);
            ids.put(definition.code(), tache.getId());
        }
        return recalculer(onboarding);
    }

    private OnboardingModele choisirModele(Contrat contrat) {
        List<OnboardingModele> disponibles = modeles.findByActifTrueOrderByIdAsc();
        if (disponibles.isEmpty()) {
            return null;
        }
        String type = contrat.getTypeContrat() == null ? null : contrat.getTypeContrat().name();
        String categorie = null;
        String site = siteDe(contrat);
        PosteDto.Liste poste = posteDuContrat(contrat);
        if (poste != null) {
            categorie = poste.categorie();
            if (poste.site() != null && !poste.site().isBlank()) {
                site = poste.site();
            }
        }
        final String categorieFinale = categorie;
        final String siteFinal = site;
        return disponibles.stream()
                .filter(m -> correspond(m, type, categorieFinale, siteFinal))
                .max(Comparator.comparingInt(m -> score(m, type, categorieFinale, siteFinal)))
                .orElseGet(() -> disponibles.stream()
                        .filter(m -> m.getTypeContrat() == null
                                && m.getCategoriePoste() == null && m.getSite() == null)
                        .findFirst().orElse(null));
    }

    private PosteDto.Liste posteDuContrat(Contrat contrat) {
        Long idPoste = affectations.findByIdContratOrderByDateDebutDesc(contrat.getId()).stream()
                .map(Affectation::getIdPoste)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
        if (idPoste == null) {
            return null;
        }
        try {
            return postes.trouver(idPoste);
        } catch (BusinessException ex) {
            return null;
        }
    }

    private boolean correspond(OnboardingModele modele, String type, String categorie, String site) {
        if (modele.getTypeContrat() != null && !modele.getTypeContrat().equalsIgnoreCase(type)) {
            return false;
        }
        if (modele.getCategoriePoste() != null
                && !modele.getCategoriePoste().equalsIgnoreCase(categorie)) {
            return false;
        }
        return modele.getSite() == null || modele.getSite().equalsIgnoreCase(site);
    }

    private int score(OnboardingModele modele, String type, String categorie, String site) {
        int score = 0;
        if (modele.getTypeContrat() != null) {
            score += 4;
        }
        if (modele.getCategoriePoste() != null) {
            score += 2;
        }
        if (modele.getSite() != null) {
            score += 1;
        }
        return score;
    }

    private List<ModeleTache> tachesDuModele(OnboardingModele modele) {
        List<OnboardingModeleTache> configurees = tachesModeles
                .findByIdModeleOrderByIdAsc(modele.getId());
        if (configurees.isEmpty()) {
            return MODELE;
        }
        return configurees.stream()
                .map(t -> new ModeleTache(t.getCode(), t.getLibelle(), t.getCategorie(),
                        t.getResponsable(), t.getEcheanceOffsetJours() == null
                                ? 0 : t.getEcheanceOffsetJours(),
                        t.isObligatoire(), t.isBloquante(), t.getDependCode()))
                .toList();
    }

    private String siteDe(Contrat contrat) {
        return contrat.getLieuTravail();
    }

    @Transactional(readOnly = true)
    public ContratDtos.OnboardingDto onboarding(Long id) {
        Onboarding onboarding = onboardings.findById(id)
                .orElseThrow(() -> BusinessException.notFound(
                        "L'onboarding " + id + " n'existe pas."));
        return ContratDtos.OnboardingDto.de(onboarding, tachesDe(onboarding.getId()));
    }

    @Transactional(readOnly = true)
    public ContratDtos.OnboardingDto onboardingDe(Long idEmploye) {
        Onboarding onboarding = onboardings.findFirstByIdEmployeOrderByDateCreationDesc(idEmploye)
                .orElseThrow(() -> BusinessException.notFound(
                        "Aucun onboarding pour l'employe " + idEmploye + "."));
        return ContratDtos.OnboardingDto.de(onboarding, tachesDe(onboarding.getId()));
    }

    /** Variante toleree pour les ecrans d'embauche : null si le parcours n'existe pas. */
    @Transactional(readOnly = true)
    public ContratDtos.OnboardingDto onboardingDeOuNull(Long idEmploye) {
        return onboardings.findFirstByIdEmployeOrderByDateCreationDesc(idEmploye)
                .map(o -> ContratDtos.OnboardingDto.de(o, tachesDe(o.getId())))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public ContratDtos.OnboardingDto onboardingDuContrat(Long idContrat) {
        Onboarding onboarding = onboardings.findFirstByIdContrat(idContrat)
                .orElseThrow(() -> BusinessException.notFound(
                        "Aucun onboarding pour le contrat " + idContrat + "."));
        return ContratDtos.OnboardingDto.de(onboarding, tachesDe(onboarding.getId()));
    }

    @Transactional(readOnly = true)
    public List<ContratDtos.OnboardingDto> onboardings() {
        return onboardings.findAll().stream()
                .map(o -> ContratDtos.OnboardingDto.de(o, tachesDe(o.getId())))
                .toList();
    }

    @Transactional
    public ContratDtos.TacheDto mettreAJourTache(Long idTache,
                                                 ContratDtos.TacheMiseAJourRequest demande) {
        OnboardingTache tache = taches.findById(idTache)
                .orElseThrow(() -> BusinessException.notFound(
                        "La tache " + idTache + " n'existe pas."));
        if (demande.statut() == TacheStatut.FAIT && tache.getIdDependDe() != null) {
            OnboardingTache dependance = taches.findById(tache.getIdDependDe())
                    .orElseThrow(() -> BusinessException.notFound(
                            "La tache prealable " + tache.getIdDependDe() + " n'existe pas."));
            if (dependance.getStatut() != TacheStatut.FAIT
                    && dependance.getStatut() != TacheStatut.NON_APPLICABLE) {
                throw new BusinessException("TACHE_DEPENDANCE_NON_FAITE",
                        "La tache \"" + dependance.getLibelle()
                                + "\" doit etre terminee avant \"" + tache.getLibelle() + "\".");
            }
        }
        tache.setStatut(demande.statut());
        if (demande.dateFait() != null) {
            tache.setDateFait(demande.dateFait());
        } else if (demande.statut() == TacheStatut.FAIT && tache.getDateFait() == null) {
            tache.setDateFait(LocalDate.now());
        }
        if (demande.idPreuveDocument() != null) {
            tache.setIdPreuveDocument(demande.idPreuveDocument());
        }
        taches.save(tache);
        recalculer(tache.getOnboarding());
        return ContratDtos.TacheDto.de(tache);
    }

    /** Recalcule progression et statut du parcours (RG-B4-09). */
    @Transactional
    Onboarding recalculer(Onboarding onboarding) {
        List<OnboardingTache> liste = tachesDe(onboarding.getId());
        List<OnboardingTache> applicables = liste.stream()
                .filter(t -> t.getStatut() != TacheStatut.NON_APPLICABLE)
                .toList();
        long faites = applicables.stream().filter(t -> t.getStatut() == TacheStatut.FAIT).count();
        int progression = applicables.isEmpty() ? 100
                : (int) Math.round(faites * 100.0 / applicables.size());
        onboarding.setProgression(progression);

        boolean bloque = liste.stream().anyMatch(t -> t.getStatut() == TacheStatut.BLOQUE);
        boolean obligatoiresFaites = liste.stream()
                .filter(OnboardingTache::isObligatoire)
                .allMatch(t -> t.getStatut() == TacheStatut.FAIT
                        || t.getStatut() == TacheStatut.NON_APPLICABLE);
        boolean demarre = liste.stream().anyMatch(t -> t.getStatut() != TacheStatut.A_FAIRE);

        if (obligatoiresFaites) {
            onboarding.setStatut(OnboardingStatut.TERMINE);
            if (onboarding.getDateCloture() == null) {
                onboarding.setDateCloture(LocalDateTime.now());
            }
        } else if (bloque) {
            onboarding.setStatut(OnboardingStatut.BLOQUE);
        } else if (demarre) {
            onboarding.setStatut(OnboardingStatut.EN_COURS);
        } else {
            onboarding.setStatut(OnboardingStatut.A_FAIRE);
        }
        return onboardings.save(onboarding);
    }

    /**
     * Controle RG-B4-09 : retourne les taches bloquantes non terminees.
     * La liste vide signifie que la validation RH est autorisee.
     */
    @Transactional(readOnly = true)
    public List<ContratDtos.TacheDto> tachesBloquantesNonFaites(Long idContrat) {
        return onboardings.findFirstByIdContrat(idContrat)
                .map(o -> tachesDe(o.getId()).stream()
                        .filter(OnboardingTache::isBloquante)
                        .filter(t -> t.getStatut() != TacheStatut.FAIT
                                && t.getStatut() != TacheStatut.NON_APPLICABLE)
                        .map(ContratDtos.TacheDto::de)
                        .toList())
                .orElseGet(List::of);
    }

    List<OnboardingTache> tachesDe(Long idOnboarding) {
        return taches.findByOnboardingIdOrderById(idOnboarding);
    }
}
