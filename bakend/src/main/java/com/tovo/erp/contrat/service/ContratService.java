package com.tovo.erp.contrat.service;

import com.tovo.erp.contrat.dto.ContratDtos;
import com.tovo.erp.contrat.model.Affectation;
import com.tovo.erp.contrat.model.AffectationStatut;
import com.tovo.erp.contrat.model.Contrat;
import com.tovo.erp.contrat.model.ContratEtat;
import com.tovo.erp.contrat.model.MotifAffectation;
import com.tovo.erp.contrat.model.Onboarding;
import com.tovo.erp.contrat.model.OnboardingStatut;
import com.tovo.erp.contrat.model.StatutExecutionContrat;
import com.tovo.erp.contrat.model.TypeContrat;
import com.tovo.erp.contrat.repository.AffectationRepository;
import com.tovo.erp.contrat.repository.ContratRepository;
import com.tovo.erp.contrat.repository.OnboardingRepository;
import com.tovo.erp.employe.dto.DocumentEmbaucheDto;
import com.tovo.erp.employe.dto.EmployeEmbaucheDto;
import com.tovo.erp.employe.model.StatutEmploye;
import com.tovo.erp.employe.model.TypeDocumentEmploye;
import com.tovo.erp.employe.service.EmployeService;
import com.tovo.erp.shared.BusinessException;
import com.tovo.erp.shared.reference.ReferenceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Cycle de vie des contrats, affectations et alertes (bloc 4, section 6).
 * <p>Un contrat suit les etats CREE -> SIGNE -> VALIDE, avec annulation
 * possible avant le debut de l'execution (RG-B4-16). Les contrats successifs
 * ne s'ecrasent jamais : le contrat courant est celui dont l'execution est
 * EN_COURS (RG-B4-03).
 */
@Service
public class ContratService {

    private final ContratRepository contrats;
    private final AffectationRepository affectations;
    private final OnboardingRepository onboardings;
    private final OnboardingService onboardingService;
    private final EmployeService employes;
    private final ReferenceService references;
    private final ContratPdfService pdf;
    private final ContratProperties properties;

    public ContratService(ContratRepository contrats,
                          AffectationRepository affectations,
                          OnboardingRepository onboardings,
                          OnboardingService onboardingService,
                          EmployeService employes,
                          ReferenceService references,
                          ContratPdfService pdf,
                          ContratProperties properties) {
        this.contrats = contrats;
        this.affectations = affectations;
        this.onboardings = onboardings;
        this.onboardingService = onboardingService;
        this.employes = employes;
        this.references = references;
        this.pdf = pdf;
        this.properties = properties;
    }

    // ------------------------------------------------------------------
    // Creation
    // ------------------------------------------------------------------

    /** Cree le contrat, l'affectation et le PDF a l'acceptation (section 5.4). */
    @Transactional
    public ContratDtos.ContratEmbaucheDto creerContratEmbauche(
            ContratDtos.CreationContratRequest demande) {
        validerCreation(demande);
        LocalDate aujourdHui = LocalDate.now();
        int periodeEssai = demande.periodeEssaiMois() == null ? 1 : demande.periodeEssaiMois();

        Contrat contrat = new Contrat();
        contrat.setNumero(references.suivante("CONTRAT", "CT", 5));
        contrat.setIdEmploye(demande.idEmploye());
        contrat.setIdProposition(demande.idProposition());
        contrat.setTypeContrat(demande.typeContrat());
        contrat.setRang(1);
        contrat.setDateDebut(demande.dateDebut());
        contrat.setDateFinPrevue(demande.dateFinPrevue());
        contrat.setFinPeriodeEssai(demande.dateDebut().plusMonths(periodeEssai));
        contrat.setSalaireBase(demande.salaireBase());
        contrat.setDevise(valeur(demande.devise(), "MGA"));
        contrat.setPrimesAvantages(demande.primesAvantages());
        contrat.setHoraires(demande.horaires());
        contrat.setLieuTravail(demande.lieuTravail());
        contrat.setMotifCdd(demande.motifCdd());
        contrat.setEtat(ContratEtat.CREE);
        contrat.setStatutExecution(demande.dateDebut().isAfter(aujourdHui)
                ? StatutExecutionContrat.A_VENIR : StatutExecutionContrat.EN_COURS);
        contrats.save(contrat);

        Affectation affectation = creerAffectation(contrat, demande.idPoste(),
                demande.idDepartement(), demande.idService(), demande.idManager(),
                demande.lieuTravail(), demande.horaires(), demande.dateDebut(),
                demande.dateFinPrevue(), true, MotifAffectation.EMBAUCHE,
                demande.dateDebut().isAfter(aujourdHui)
                        ? AffectationStatut.PLANIFIEE : AffectationStatut.ACTIVE);

        genererDocument(contrat, demande.lieuTravail());

        return new ContratDtos.ContratEmbaucheDto(
                contrat.getId(), contrat.getNumero(), contrat.getIdEmploye(),
                contrat.getIdProposition(), contrat.getTypeContrat(),
                contrat.getDateDebut(), contrat.getDateFinPrevue(),
                contrat.getFinPeriodeEssai(), contrat.getSalaireBase(),
                contrat.getDevise(), contrat.getEtat(), contrat.getStatutExecution(),
                contrat.getDocumentPdfId(), affectation.getId(), null, List.of());
    }

    private void validerCreation(ContratDtos.CreationContratRequest demande) {
        if (demande.idEmploye() == null) {
            throw new BusinessException("CONTRAT_INCOMPLET", "Le salarie du contrat est obligatoire.");
        }
        if (demande.dateDebut() == null) {
            throw new BusinessException("CONTRAT_INCOMPLET", "La date de debut est obligatoire.");
        }
        if (demande.typeContrat() == null) {
            throw new BusinessException("CONTRAT_INCOMPLET", "Le type de contrat est obligatoire.");
        }
        if (demande.salaireBase() == null || demande.salaireBase().signum() < 0) {
            throw new BusinessException("CONTRAT_INCOMPLET", "Le salaire de base est invalide.");
        }
        if (demande.typeContrat().estDureeDeterminee() && demande.dateFinPrevue() == null) {
            throw new BusinessException("CONTRAT_INCOMPLET",
                    "Un CDD doit avoir une date de fin prevue.");
        }
        if (demande.typeContrat().estDureeDeterminee()
                && demande.dateFinPrevue().isBefore(demande.dateDebut())) {
            throw new BusinessException("CONTRAT_INCOMPLET",
                    "La date de fin ne peut pas precéder la date de debut.");
        }
        if (!demande.typeContrat().estDureeDeterminee() && demande.dateFinPrevue() != null) {
            throw new BusinessException("CONTRAT_INCOMPLET",
                    "Un contrat a duree indeterminee ne peut pas avoir de date de fin prevue.");
        }
    }

    // ------------------------------------------------------------------
    // Lecture
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ContratDtos.ContratDto contrat(Long id) {
        return ContratDtos.ContratDto.de(contratEntite(id));
    }

    @Transactional(readOnly = true)
    public List<ContratDtos.ContratDto> contratsDe(Long idEmploye) {
        return contrats.findByIdEmployeOrderByDateDebutDesc(idEmploye).stream()
                .map(ContratDtos.ContratDto::de).toList();
    }

    @Transactional(readOnly = true)
    public List<ContratDtos.ContratDto> lister(ContratEtat etat) {
        List<Contrat> liste = etat == null
                ? contrats.findAllByOrderByDateCreationDesc()
                : contrats.findByEtatOrderByDateDebutAsc(etat);
        return liste.stream().map(ContratDtos.ContratDto::de).toList();
    }

    /** Contrat dont l'execution est en cours a la date demandee (RG-B4-03). */
    @Transactional(readOnly = true)
    public ContratDtos.ContratDto courant(Long idEmploye) {
        LocalDate reference = LocalDate.now();
        return contrats.findByIdEmployeOrderByDateDebutDesc(idEmploye).stream()
                .filter(c -> c.getEtat() == ContratEtat.VALIDE || c.getEtat() == ContratEtat.SIGNE)
                .filter(c -> c.getStatutExecution() == StatutExecutionContrat.EN_COURS)
                .filter(c -> !c.getDateDebut().isAfter(reference))
                .filter(c -> c.getDateFinPrevue() == null
                        || !c.getDateFinPrevue().isBefore(reference))
                .findFirst()
                .map(ContratDtos.ContratDto::de)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<ContratDtos.ContratDto> actifs() {
        return contrats.findByStatutExecutionOrderByDateDebutAsc(
                        StatutExecutionContrat.EN_COURS).stream()
                .map(ContratDtos.ContratDto::de).toList();
    }

    @Transactional(readOnly = true)
    public List<ContratDtos.AffectationDto> affectationsDe(Long idEmploye) {
        return affectations.findByIdEmployeOrderByDateDebutDesc(idEmploye).stream()
                .map(ContratDtos.AffectationDto::de).toList();
    }

    @Transactional(readOnly = true)
    public List<ContratDtos.TacheDto> tachesDe(Long idOnboarding) {
        return onboardingService.tachesDe(idOnboarding).stream()
                .map(ContratDtos.TacheDto::de).toList();
    }

    @Transactional(readOnly = true)
    public ContratDtos.OnboardingDto onboardingDe(Long idEmploye) {
        return onboardingService.onboardingDe(idEmploye);
    }

    @Transactional(readOnly = true)
    public ContratDtos.OnboardingDto onboardingDeOuNull(Long idEmploye) {
        return onboardingService.onboardingDeOuNull(idEmploye);
    }

    // ------------------------------------------------------------------
    // Cycle de vie
    // ------------------------------------------------------------------

    /** Signature des deux parties, attribution du matricule et onboarding (RG-B4-05/06/08). */
    @Transactional
    public ContratDtos.ContratDto signer(Long id, ContratDtos.SignatureRequest demande) {
        Contrat contrat = contratEntite(id);
        if (contrat.getEtat() == ContratEtat.SIGNE || contrat.getEtat() == ContratEtat.VALIDE) {
            return ContratDtos.ContratDto.de(contrat);
        }
        if (contrat.getEtat() != ContratEtat.CREE) {
            throw new BusinessException("CONTRAT_NON_SIGNABLE",
                    "Seul un contrat cree peut etre signe (etat actuel : " + contrat.getEtat() + ").");
        }
        if (demande == null || demande.signataireEmployeurId() == null) {
            throw new BusinessException("SIGNATAIRE_MANQUANT",
                    "Le signataire employeur est obligatoire pour signer le contrat.");
        }
        LocalDateTime maintenant = LocalDateTime.now();
        contrat.setDateSignatureSalarie(maintenant);
        contrat.setDateSignatureEmployeur(maintenant);
        contrat.setSignataireEmployeurId(demande.signataireEmployeurId());
        contrat.setEtat(ContratEtat.SIGNE);

        employes.attribuerMatricule(contrat.getIdEmploye(),
                () -> references.suivante("MATRICULE", "MAT", 6).replace("-", ""));
        Onboarding onboarding = onboardingService.creerPour(contrat, demande.signataireEmployeurId());
        genererDocument(contrat, contrat.getLieuTravail());
        contrats.save(contrat);
        return ContratDtos.ContratDto.de(contrat);
    }

    /** Validation RH apres signature et taches bloquantes (RG-B4-09). */
    @Transactional
    public ContratDtos.ContratDto valider(Long id) {
        Contrat contrat = contratEntite(id);
        if (contrat.getEtat() == ContratEtat.VALIDE) {
            return ContratDtos.ContratDto.de(contrat);
        }
        if (contrat.getEtat() != ContratEtat.SIGNE) {
            throw new BusinessException("CONTRAT_NON_VALIDABLE",
                    "Le contrat doit etre signe avant validation (etat actuel : "
                            + contrat.getEtat() + ").");
        }
        List<ContratDtos.TacheDto> bloquantes =
                onboardingService.tachesBloquantesNonFaites(contrat.getId());
        if (!bloquantes.isEmpty()) {
            String liste = bloquantes.stream().map(ContratDtos.TacheDto::libelle)
                    .reduce((a, b) -> a + ", " + b).orElse("");
            throw new BusinessException("ONBOARDING_BLOQUANT",
                    "Validation RH impossible : taches bloquantes non terminees (" + liste + ").");
        }
        contrat.setEtat(ContratEtat.VALIDE);
        if (!contrat.getDateDebut().isAfter(LocalDate.now())) {
            activer(contrat);
        }
        contrats.save(contrat);
        return ContratDtos.ContratDto.de(contrat);
    }

    /**
     * Annulation. Un contrat CREE peut etre annule ; un contrat SIGNE uniquement
     * avant sa date de debut (retractation, RG-B4-16). Un contrat VALIDE est fige.
     */
    @Transactional
    public ContratDtos.ContratDto annuler(Long id, ContratDtos.AnnulationRequest demande) {
        Contrat contrat = contratEntite(id);
        if (demande == null || demande.motif() == null || demande.motif().isBlank()) {
            throw new BusinessException("MOTIF_ANNULATION_MANQUANT",
                    "Le motif d'annulation est obligatoire.");
        }
        if (contrat.getEtat() == ContratEtat.VALIDE) {
            throw new BusinessException("CONTRAT_VALIDE_NON_ANNULE",
                    "Un contrat valide ne peut pas etre annule.");
        }
        if (contrat.getEtat() == ContratEtat.SIGNE
                && !contrat.getDateDebut().isAfter(LocalDate.now())) {
            throw new BusinessException("RETRACTATION_TARDIVE",
                    "La retractation n'est possible qu'avant le debut du contrat.");
        }
        if (contrat.getEtat() == ContratEtat.ANNULE) {
            return ContratDtos.ContratDto.de(contrat);
        }
        contrat.setEtat(ContratEtat.ANNULE);
        contrat.setMotifAnnulation(demande.motif());
        contrat.setDateAnnulation(LocalDateTime.now());
        contrat.setDateCloture(LocalDateTime.now());
        contrats.save(contrat);

        for (Affectation affectation : affectations.findByIdContratOrderByDateDebutDesc(id)) {
            affectation.setStatut(AffectationStatut.ANNULEE);
            affectations.save(affectation);
        }
        onboardings.findFirstByIdContrat(id).ifPresent(o -> {
            o.setStatut(OnboardingStatut.BLOQUE);
            onboardings.save(o);
        });
        return ContratDtos.ContratDto.de(contrat);
    }

    /** Renouvellement d'un CDD dans la limite legale (RG-B4-12). */
    @Transactional
    public ContratDtos.ContratDto renouveler(Long id,
                                             ContratDtos.RenouvellementRequest demande) {
        Contrat precedent = contratEntite(id);
        if (!precedent.getTypeContrat().estDureeDeterminee()) {
            throw new BusinessException("RENOUVELLEMENT_INTERDIT",
                    "Seul un contrat a duree determinee peut etre renouvele.");
        }
        if (precedent.getEtat() != ContratEtat.VALIDE) {
            throw new BusinessException("CONTRAT_NON_RENOUVELABLE",
                    "Le contrat doit etre valide pour etre renouvele.");
        }
        if (precedent.getDateFinPrevue() == null) {
            throw new BusinessException("RENOUVELLEMENT_INTERDIT",
                    "Le contrat precedent n'a pas de date de fin.");
        }
        if (demande == null || demande.dateFinPrevue() == null) {
            throw new BusinessException("RENOUVELLEMENT_INCOMPLET",
                    "La date de fin du renouvellement est obligatoire.");
        }
        LocalDate debut = precedent.getDateFinPrevue().plusDays(1);
        if (!demande.dateFinPrevue().isAfter(debut)) {
            throw new BusinessException("RENOUVELLEMENT_INCOMPLET",
                    "La date de fin doit etre posterieure au debut du renouvellement.");
        }
        List<Contrat> chaine = chaine(precedent);
        long renouvellements = chaine.stream()
                .filter(c -> c.getTypeContrat() == TypeContrat.CDD_RENOUVELLEMENT)
                .count();
        if (renouvellements >= properties.renouvellementsCddMax()) {
            throw new BusinessException("RENOUVELLEMENT_LIMITE",
                    "Nombre maximal de renouvellements atteint ("
                            + properties.renouvellementsCddMax()
                            + "). Proposez une transformation en CDI.");
        }
        LocalDate premierDebut = chaine.get(0).getDateDebut();
        long jours = ChronoUnit.DAYS.between(premierDebut,
                demande.dateFinPrevue().plusDays(1));
        if (jours > (long) properties.dureeMaxCddMois() * 31) {
            throw new BusinessException("DUREE_CDD_DEPASSEE",
                    "La duree cumulee depasserait le plafond de "
                            + properties.dureeMaxCddMois() + " mois.");
        }
        if (demande.motifCdd() == null || demande.motifCdd().isBlank()) {
            throw new BusinessException("MOTIF_CDD_MANQUANT",
                    "Le motif du recours au CDD est obligatoire.");
        }

        Contrat nouveau = new Contrat();
        nouveau.setNumero(references.suivante("CONTRAT", "CT", 5));
        nouveau.setIdEmploye(precedent.getIdEmploye());
        nouveau.setIdProposition(precedent.getIdProposition());
        nouveau.setIdContratPrecedent(precedent.getId());
        nouveau.setRang(precedent.getRang() + 1);
        nouveau.setTypeContrat(TypeContrat.CDD_RENOUVELLEMENT);
        nouveau.setDateDebut(debut);
        nouveau.setDateFinPrevue(demande.dateFinPrevue());
        nouveau.setSalaireBase(demande.salaireBase() == null
                ? precedent.getSalaireBase() : demande.salaireBase());
        nouveau.setDevise(precedent.getDevise());
        nouveau.setPrimesAvantages(demande.primesAvantages() == null
                ? precedent.getPrimesAvantages() : demande.primesAvantages());
        nouveau.setHoraires(demande.horaires() == null
                ? precedent.getHoraires() : demande.horaires());
        nouveau.setLieuTravail(demande.lieuTravail() == null
                ? precedent.getLieuTravail() : demande.lieuTravail());
        nouveau.setMotifCdd(demande.motifCdd());
        nouveau.setEtat(ContratEtat.CREE);
        nouveau.setStatutExecution(StatutExecutionContrat.A_VENIR);
        contrats.save(nouveau);
        creerAffectationSuccession(nouveau, precedent);
        genererDocument(nouveau, nouveau.getLieuTravail());
        return ContratDtos.ContratDto.de(nouveau);
    }

    /** Transformation d'un CDD en CDI, sans rupture de continuite (RG-B4-12). */
    @Transactional
    public ContratDtos.ContratDto transformerEnCdi(
            Long id, ContratDtos.TransformationCdiRequest demande) {
        Contrat precedent = contratEntite(id);
        if (!precedent.getTypeContrat().estDureeDeterminee()) {
            throw new BusinessException("TRANSFORMATION_INTERDITE",
                    "Seul un contrat a duree determinee peut etre transforme en CDI.");
        }
        if (precedent.getEtat() != ContratEtat.VALIDE) {
            throw new BusinessException("CONTRAT_NON_TRANSFORMABLE",
                    "Le contrat doit etre valide pour etre transforme.");
        }
        LocalDate debut = demande != null && demande.dateDebut() != null
                ? demande.dateDebut()
                : (precedent.getDateFinPrevue() == null
                        ? LocalDate.now() : precedent.getDateFinPrevue().plusDays(1));
        if (precedent.getDateFinPrevue() != null && !debut.isAfter(precedent.getDateDebut())) {
            throw new BusinessException("TRANSFORMATION_INVALIDE",
                    "La date de debut du CDI doit etre posterieure au debut du CDD.");
        }
        Contrat nouveau = new Contrat();
        nouveau.setNumero(references.suivante("CONTRAT", "CT", 5));
        nouveau.setIdEmploye(precedent.getIdEmploye());
        nouveau.setIdProposition(precedent.getIdProposition());
        nouveau.setIdContratPrecedent(precedent.getId());
        nouveau.setRang(precedent.getRang() + 1);
        nouveau.setTypeContrat(TypeContrat.CDI);
        nouveau.setDateDebut(debut);
        nouveau.setDateFinPrevue(null);
        nouveau.setSalaireBase(demande == null || demande.salaireBase() == null
                ? precedent.getSalaireBase() : demande.salaireBase());
        nouveau.setDevise(precedent.getDevise());
        nouveau.setPrimesAvantages(demande == null || demande.primesAvantages() == null
                ? precedent.getPrimesAvantages() : demande.primesAvantages());
        nouveau.setHoraires(demande == null || demande.horaires() == null
                ? precedent.getHoraires() : demande.horaires());
        nouveau.setLieuTravail(demande == null || demande.lieuTravail() == null
                ? precedent.getLieuTravail() : demande.lieuTravail());
        nouveau.setEtat(ContratEtat.CREE);
        nouveau.setStatutExecution(StatutExecutionContrat.A_VENIR);
        contrats.save(nouveau);
        creerAffectationSuccession(nouveau, precedent);
        genererDocument(nouveau, nouveau.getLieuTravail());
        return ContratDtos.ContratDto.de(nouveau);
    }

    /**
     * Ajoute une affectation (mutation, promotion) sur le contrat en cours.
     * Une affectation principale ferme automatiquement la precedente.
     */
    @Transactional
    public ContratDtos.AffectationDto ajouterAffectation(
            Long idEmploye, ContratDtos.AffectationRequest demande) {
        if (demande == null || demande.dateDebut() == null || demande.motif() == null) {
            throw new BusinessException("AFFECTATION_INCOMPLETE",
                    "La date de debut et le motif de l'affectation sont obligatoires.");
        }
        Contrat contrat = contrats.findByIdEmployeOrderByDateDebutDesc(idEmploye).stream()
                .filter(c -> c.getEtat() == ContratEtat.VALIDE
                        && c.getStatutExecution() == StatutExecutionContrat.EN_COURS)
                .findFirst()
                .orElseThrow(() -> new BusinessException("AUCUN_CONTRAT_ACTIF",
                        "Aucun contrat actif pour l'employe " + idEmploye + "."));
        boolean future = demande.dateDebut().isAfter(LocalDate.now());
        AffectationStatut statut = demande.principale() && !future
                ? AffectationStatut.ACTIVE : AffectationStatut.PLANIFIEE;
        if (demande.principale() && !future) {
            for (Affectation ancienne : affectations.findByIdEmployeOrderByDateDebutDesc(idEmploye)) {
                if (ancienne.isPrincipale() && ancienne.getStatut() == AffectationStatut.ACTIVE) {
                    ancienne.setPrincipale(false);
                    ancienne.setStatut(AffectationStatut.TERMINEE);
                    ancienne.setDateFin(demande.dateDebut().minusDays(1));
                    affectations.save(ancienne);
                }
            }
        }
        int taux = demande.tauxActivite() == null ? 100 : demande.tauxActivite();
        if (taux < 1 || taux > 100) {
            throw new BusinessException("TAUX_INVALIDE",
                    "Le taux d'activite doit etre compris entre 1 et 100.");
        }
        Affectation affectation = creerAffectation(contrat, demande.idPoste(),
                demande.idDepartement(), demande.idService(), demande.idManager(),
                demande.lieuTravail(), demande.horaires(), demande.dateDebut(),
                demande.dateFin(), demande.principale(), demande.motif(), statut);
        affectation.setTauxActivite(taux);
        return ContratDtos.AffectationDto.de(affectations.save(affectation));
    }

    // ------------------------------------------------------------------
    // Echeances et alertes
    // ------------------------------------------------------------------

    /** Resultat d'un passage du planificateur. */
    public record ResultatEcheances(int actives, int termines, int absences) {
    }

    /**
     * Applique les transitions dues : activation, fin de contrat et
     * non-presentation (RG-B4-17). Idempotent, appele par le planificateur.
     */
    @Transactional
    public ResultatEcheances traiterEcheances() {
        LocalDate aujourdHui = LocalDate.now();
        int actives = 0;
        int termines = 0;
        int absences = 0;
        for (Contrat contrat : contrats.findAllByOrderByDateCreationDesc()) {
            if (contrat.getEtat() == ContratEtat.ANNULE) {
                continue;
            }
            if (contrat.getEtat() == ContratEtat.VALIDE
                    && contrat.getStatutExecution() == StatutExecutionContrat.A_VENIR
                    && !contrat.getDateDebut().isAfter(aujourdHui)) {
                activer(contrat);
                contrats.save(contrat);
                actives++;
            }
            if (contrat.getEtat() == ContratEtat.VALIDE
                    && contrat.getStatutExecution() == StatutExecutionContrat.EN_COURS
                    && contrat.getDateFinPrevue() != null
                    && contrat.getDateFinPrevue().isBefore(aujourdHui)) {
                terminer(contrat);
                contrats.save(contrat);
                termines++;
            }
            if ((contrat.getEtat() == ContratEtat.SIGNE || contrat.getEtat() == ContratEtat.VALIDE)
                    && contrat.getStatutExecution() == StatutExecutionContrat.A_VENIR
                    && contrat.getDateDebut()
                            .plusDays(properties.nonPresentationJours()).isBefore(aujourdHui)) {
                try {
                    EmployeEmbaucheDto salarie = employes.dossier(contrat.getIdEmploye());
                    if (salarie.statut() == StatutEmploye.PRE_EMBAUCHE) {
                        employes.changerStatut(contrat.getIdEmploye(),
                                StatutEmploye.NON_PRIS_POSTE);
                        absences++;
                    }
                } catch (BusinessException ignore) {
                    // Le salarie n'existe plus : rien a marquer.
                }
            }
        }
        return new ResultatEcheances(actives, termines, absences);
    }

    /** Alertes calculees (signature, CDD, periode d'essai, onboarding en retard). */
    @Transactional(readOnly = true)
    public List<ContratDtos.AlerteDto> alertes() {
        LocalDate aujourdHui = LocalDate.now();
        List<ContratDtos.AlerteDto> resultat = new ArrayList<>();
        for (Contrat contrat : contrats.findAllByOrderByDateCreationDesc()) {
            if (contrat.getEtat() == ContratEtat.ANNULE) {
                continue;
            }
            if (contrat.getEtat() == ContratEtat.CREE) {
                long jours = ChronoUnit.DAYS.between(aujourdHui, contrat.getDateDebut());
                if (jours < 0) {
                    resultat.add(alerte("SIGNATURE", "DANGER",
                            "Contrat " + contrat.getNumero() + " non signe alors que la date de debut est passee.",
                            contrat, contrat.getDateDebut()));
                } else if (jours <= properties.signatureAlerteJours()) {
                    resultat.add(alerte("SIGNATURE", "ATTENTION",
                            "Contrat " + contrat.getNumero() + " a signer sous " + jours + " jour(s).",
                            contrat, contrat.getDateDebut()));
                }
            }
            if (contrat.getEtat() == ContratEtat.SIGNE
                    && !contrat.getDateDebut().isAfter(aujourdHui)) {
                resultat.add(alerte("VALIDATION", "ATTENTION",
                        "Contrat " + contrat.getNumero()
                                + " signe mais non valide alors qu'il devrait demarrer.",
                        contrat, contrat.getDateDebut()));
            }
            if (contrat.getEtat() == ContratEtat.VALIDE
                    && contrat.getTypeContrat().estDureeDeterminee()
                    && contrat.getDateFinPrevue() != null) {
                long jours = ChronoUnit.DAYS.between(aujourdHui, contrat.getDateFinPrevue());
                if (jours < 0) {
                    resultat.add(alerte("FIN_CDD", "DANGER",
                            "CDD " + contrat.getNumero() + " expire depuis "
                                    + (-jours) + " jour(s).", contrat, contrat.getDateFinPrevue()));
                } else if (jours <= properties.cddAlerteJours3()) {
                    resultat.add(alerte("FIN_CDD", "DANGER",
                            "CDD " + contrat.getNumero() + " expire dans " + jours + " jour(s).",
                            contrat, contrat.getDateFinPrevue()));
                } else if (jours <= properties.cddAlerteJours2()) {
                    resultat.add(alerte("FIN_CDD", "ATTENTION",
                            "CDD " + contrat.getNumero() + " expire dans " + jours + " jour(s).",
                            contrat, contrat.getDateFinPrevue()));
                } else if (jours <= properties.cddAlerteJours1()) {
                    resultat.add(alerte("FIN_CDD", "INFO",
                            "CDD " + contrat.getNumero() + " expire dans " + jours + " jour(s).",
                            contrat, contrat.getDateFinPrevue()));
                }
            }
            if (contrat.getEtat() == ContratEtat.VALIDE && contrat.getFinPeriodeEssai() != null) {
                long jours = ChronoUnit.DAYS.between(aujourdHui, contrat.getFinPeriodeEssai());
                if (jours >= 0 && jours <= properties.essaiAlerteJours1()) {
                    resultat.add(alerte("FIN_ESSAI",
                            jours <= properties.essaiAlerteJours2() ? "DANGER" : "ATTENTION",
                            "Periode d'essai du contrat " + contrat.getNumero()
                                    + " se termine dans " + jours + " jour(s).",
                            contrat, contrat.getFinPeriodeEssai()));
                }
            }
            Optional<Onboarding> onboarding = onboardings.findFirstByIdContrat(contrat.getId());
            if (onboarding.isPresent() && onboarding.get().getStatut() != OnboardingStatut.TERMINE
                    && onboarding.get().getDateCible() != null
                    && onboarding.get().getDateCible().isBefore(aujourdHui)) {
                resultat.add(alerte("ONBOARDING", "ATTENTION",
                        "L'onboarding du contrat " + contrat.getNumero() + " est en retard (cible "
                                + onboarding.get().getDateCible() + ").",
                        contrat, onboarding.get().getDateCible()));
            }
        }
        resultat.sort(Comparator.comparing(ContratDtos.AlerteDto::echeance,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return resultat;
    }

    // ------------------------------------------------------------------
    // Aides internes
    // ------------------------------------------------------------------

    private Contrat contratEntite(Long id) {
        return contrats.findById(id)
                .orElseThrow(() -> BusinessException.notFound(
                        "Le contrat " + id + " n'existe pas."));
    }

    private void activer(Contrat contrat) {
        contrat.setStatutExecution(StatutExecutionContrat.EN_COURS);
        if (contrat.getIdContratPrecedent() != null) {
            contrats.findById(contrat.getIdContratPrecedent()).ifPresent(precedent -> {
                if (precedent.getStatutExecution() == StatutExecutionContrat.EN_COURS) {
                    precedent.setStatutExecution(StatutExecutionContrat.TERMINE);
                    precedent.setDateFinReelle(precedent.getDateFinPrevue() == null
                            ? contrat.getDateDebut().minusDays(1)
                            : precedent.getDateFinPrevue());
                    precedent.setDateCloture(LocalDateTime.now());
                    contrats.save(precedent);
                }
            });
        }
        for (Affectation affectation : affectations.findByIdContratOrderByDateDebutDesc(contrat.getId())) {
            affectation.setStatut(AffectationStatut.ACTIVE);
            if (contrat.getIdContratPrecedent() != null) {
                affectation.setPrincipale(true);
            }
            affectations.save(affectation);
        }
        if (contrat.getIdContratPrecedent() != null) {
            for (Affectation ancienne : affectations.findByIdContratOrderByDateDebutDesc(
                    contrat.getIdContratPrecedent())) {
                ancienne.setStatut(AffectationStatut.TERMINEE);
                ancienne.setPrincipale(false);
                ancienne.setDateFin(contrat.getDateDebut().minusDays(1));
                affectations.save(ancienne);
            }
        }
        employes.changerStatut(contrat.getIdEmploye(), StatutEmploye.ACTIF);
    }

    private void terminer(Contrat contrat) {
        contrat.setStatutExecution(StatutExecutionContrat.TERMINE);
        contrat.setDateFinReelle(contrat.getDateFinPrevue());
        contrat.setDateCloture(LocalDateTime.now());
        for (Affectation affectation : affectations.findByIdContratOrderByDateDebutDesc(contrat.getId())) {
            affectation.setStatut(AffectationStatut.TERMINEE);
            affectation.setDateFin(contrat.getDateFinPrevue());
            affectations.save(affectation);
        }
        boolean autreContrat = contrats.findByIdEmployeOrderByDateDebutDesc(contrat.getIdEmploye())
                .stream()
                .anyMatch(c -> !c.getId().equals(contrat.getId())
                        && c.getStatutExecution() == StatutExecutionContrat.EN_COURS);
        if (!autreContrat) {
            employes.changerStatut(contrat.getIdEmploye(), StatutEmploye.INACTIF);
        }
    }

    private Affectation creerAffectation(Contrat contrat, Long idPoste, Long idDepartement,
                                         Long idService, Long idManager, String lieu,
                                         String horaires, LocalDate debut, LocalDate fin,
                                         boolean principale, MotifAffectation motif,
                                         AffectationStatut statut) {
        Affectation affectation = new Affectation();
        affectation.setIdEmploye(contrat.getIdEmploye());
        affectation.setIdContrat(contrat.getId());
        affectation.setIdPoste(idPoste);
        affectation.setIdDepartement(idDepartement);
        affectation.setIdService(idService);
        affectation.setIdManager(idManager);
        affectation.setLieuTravail(lieu);
        affectation.setHoraires(horaires);
        affectation.setDateDebut(debut);
        affectation.setDateFin(fin);
        affectation.setTauxActivite(100);
        affectation.setPrincipale(principale);
        affectation.setMotif(motif);
        affectation.setStatut(statut);
        return affectations.save(affectation);
    }

    private void creerAffectationSuccession(Contrat nouveau, Contrat precedent) {
        List<Affectation> anciennes =
                affectations.findByIdContratOrderByDateDebutDesc(precedent.getId());
        Affectation modele = anciennes.isEmpty() ? null : anciennes.get(0);
        creerAffectation(nouveau,
                modele == null ? null : modele.getIdPoste(),
                modele == null ? null : modele.getIdDepartement(),
                modele == null ? null : modele.getIdService(),
                modele == null ? null : modele.getIdManager(),
                nouveau.getLieuTravail(), nouveau.getHoraires(), nouveau.getDateDebut(),
                nouveau.getDateFinPrevue(), false, MotifAffectation.EMBAUCHE,
                AffectationStatut.PLANIFIEE);
    }

    private List<Contrat> chaine(Contrat contrat) {
        List<Contrat> resultat = new ArrayList<>();
        Contrat courant = contrat;
        while (courant != null) {
            resultat.add(0, courant);
            if (courant.getIdContratPrecedent() == null) {
                break;
            }
            courant = contrats.findById(courant.getIdContratPrecedent()).orElse(null);
        }
        return resultat;
    }

    private void genererDocument(Contrat contrat, String lieu) {
        EmployeEmbaucheDto salarie = employes.dossier(contrat.getIdEmploye());
        byte[] contenu = pdf.generer(contrat, salarie, salarie.poste(), salarie.service(),
                salarie.departement(), lieu);
        DocumentEmbaucheDto document = employes.enregistrerDocument(
                contrat.getIdEmploye(), TypeDocumentEmploye.CONTRAT,
                "Contrat " + contrat.getNumero(),
                "contrat-" + contrat.getNumero() + ".pdf",
                "application/pdf", contenu, contrat.getId(), LocalDate.now(), null);
        contrat.setDocumentPdfId(document.id());
        contrats.save(contrat);
    }

    private ContratDtos.AlerteDto alerte(String categorie, String niveau, String message,
                                         Contrat contrat, LocalDate echeance) {
        return new ContratDtos.AlerteDto(categorie, niveau, message, contrat.getId(),
                contrat.getNumero(), contrat.getIdEmploye(), echeance);
    }

    private static String valeur(String valeur, String defaut) {
        return valeur == null || valeur.isBlank() ? defaut : valeur;
    }
}
