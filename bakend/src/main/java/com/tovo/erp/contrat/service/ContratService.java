package com.tovo.erp.contrat.service;

import com.tovo.erp.contrat.dto.ContratDtos;
import com.tovo.erp.contrat.model.Affectation;
import com.tovo.erp.contrat.model.AffectationStatut;
import com.tovo.erp.contrat.model.CategorieTache;
import com.tovo.erp.contrat.model.Contrat;
import com.tovo.erp.contrat.model.ContratEtat;
import com.tovo.erp.contrat.model.MotifAffectation;
import com.tovo.erp.contrat.model.Onboarding;
import com.tovo.erp.contrat.model.OnboardingStatut;
import com.tovo.erp.contrat.model.OnboardingTache;
import com.tovo.erp.contrat.model.StatutExecutionContrat;
import com.tovo.erp.contrat.model.TacheStatut;
import com.tovo.erp.contrat.repository.AffectationRepository;
import com.tovo.erp.contrat.repository.ContratRepository;
import com.tovo.erp.contrat.repository.OnboardingRepository;
import com.tovo.erp.contrat.repository.OnboardingTacheRepository;
import com.tovo.erp.employe.dto.DocumentEmbaucheDto;
import com.tovo.erp.employe.dto.EmployeEmbaucheDto;
import com.tovo.erp.employe.model.TypeDocumentEmploye;
import com.tovo.erp.employe.service.EmployeService;
import com.tovo.erp.shared.BusinessException;
import com.tovo.erp.shared.reference.ReferenceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Creation et consultation des contrats, affectations et parcours d'integration
 * (bloc 4). La creation est declenchee par l'acceptation d'une proposition.
 */
@Service
public class ContratService {

    private final ContratRepository contrats;
    private final AffectationRepository affectations;
    private final OnboardingRepository onboardings;
    private final OnboardingTacheRepository taches;
    private final EmployeService employes;
    private final ReferenceService references;
    private final ContratPdfService pdf;

    public ContratService(ContratRepository contrats,
                          AffectationRepository affectations,
                          OnboardingRepository onboardings,
                          OnboardingTacheRepository taches,
                          EmployeService employes,
                          ReferenceService references,
                          ContratPdfService pdf) {
        this.contrats = contrats;
        this.affectations = affectations;
        this.onboardings = onboardings;
        this.taches = taches;
        this.employes = employes;
        this.references = references;
        this.pdf = pdf;
    }

    /** Cree le contrat, l'affectation, l'onboarding et son PDF (section 5.4). */
    @Transactional
    public ContratDtos.ContratEmbaucheDto creerContratEmbauche(ContratDtos.CreationContratRequest demande) {
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
        if (demande.typeContrat() == com.tovo.erp.contrat.model.TypeContrat.CDD
                && demande.dateFinPrevue() == null) {
            throw new BusinessException("CONTRAT_INCOMPLET", "Un CDD doit avoir une date de fin prevue.");
        }

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
        contrat.setDevise(demande.devise() == null || demande.devise().isBlank() ? "MGA" : demande.devise());
        contrat.setPrimesAvantages(demande.primesAvantages());
        contrat.setHoraires(demande.horaires());
        contrat.setLieuTravail(demande.lieuTravail());
        contrat.setMotifCdd(demande.motifCdd());
        contrat.setEtat(ContratEtat.CREE);
        contrat.setStatutExecution(demande.dateDebut().isAfter(aujourdHui)
                ? StatutExecutionContrat.A_VENIR : StatutExecutionContrat.EN_COURS);
        contrats.save(contrat);

        Affectation affectation = new Affectation();
        affectation.setIdEmploye(demande.idEmploye());
        affectation.setIdContrat(contrat.getId());
        affectation.setIdPoste(demande.idPoste());
        affectation.setIdDepartement(demande.idDepartement());
        affectation.setIdService(demande.idService());
        affectation.setIdManager(demande.idManager());
        affectation.setLieuTravail(demande.lieuTravail());
        affectation.setHoraires(demande.horaires());
        affectation.setDateDebut(demande.dateDebut());
        affectation.setTauxActivite(100);
        affectation.setPrincipale(true);
        affectation.setMotif(MotifAffectation.EMBAUCHE);
        affectation.setStatut(demande.dateDebut().isAfter(aujourdHui)
                ? AffectationStatut.PLANIFIEE : AffectationStatut.ACTIVE);
        affectations.save(affectation);

        Onboarding onboarding = new Onboarding();
        onboarding.setIdEmploye(demande.idEmploye());
        onboarding.setIdContrat(contrat.getId());
        onboarding.setDateDebut(demande.dateDebut());
        onboarding.setDateCible(demande.dateDebut());
        onboarding.setIdResponsableRh(demande.idResponsableRh());
        onboarding.setStatut(OnboardingStatut.A_FAIRE);
        onboarding.setProgression(0);
        onboardings.save(onboarding);

        List<OnboardingTache> tachesCreees = creerTaches(onboarding, demande.dateDebut());
        List<Long> idsTaches = tachesCreees.stream().map(OnboardingTache::getId).toList();

        EmployeEmbaucheDto salarie = employes.dossier(demande.idEmploye());
        byte[] contenu = pdf.generer(contrat, salarie, salarie.poste(), salarie.service(),
                salarie.departement(), demande.lieuTravail());
        DocumentEmbaucheDto document = employes.enregistrerDocument(
                demande.idEmploye(),
                TypeDocumentEmploye.CONTRAT,
                "Contrat " + contrat.getNumero(),
                "contrat-" + contrat.getNumero() + ".pdf",
                "application/pdf",
                contenu,
                contrat.getId(),
                LocalDate.now(),
                null);
        contrat.setDocumentPdfId(document.id());
        contrats.save(contrat);

        return new ContratDtos.ContratEmbaucheDto(
                contrat.getId(),
                contrat.getNumero(),
                contrat.getIdEmploye(),
                contrat.getIdProposition(),
                contrat.getTypeContrat(),
                contrat.getDateDebut(),
                contrat.getDateFinPrevue(),
                contrat.getFinPeriodeEssai(),
                contrat.getSalaireBase(),
                contrat.getDevise(),
                contrat.getEtat(),
                contrat.getStatutExecution(),
                contrat.getDocumentPdfId(),
                affectation.getId(),
                onboarding.getId(),
                idsTaches);
    }

    @Transactional(readOnly = true)
    public ContratDtos.ContratDto contrat(Long id) {
        return ContratDtos.ContratDto.de(contrats.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Le contrat " + id + " n'existe pas.")));
    }

    @Transactional(readOnly = true)
    public List<ContratDtos.ContratDto> contratsDe(Long idEmploye) {
        return contrats.findByIdEmployeOrderByDateDebutDesc(idEmploye).stream()
                .map(ContratDtos.ContratDto::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ContratDtos.AffectationDto> affectationsDe(Long idEmploye) {
        return affectations.findByIdEmployeOrderByDateDebutDesc(idEmploye).stream()
                .map(ContratDtos.AffectationDto::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ContratDtos.TacheDto> tachesDe(Long idOnboarding) {
        return taches.findByOnboardingIdOrderById(idOnboarding).stream()
                .map(ContratDtos.TacheDto::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public ContratDtos.OnboardingDto onboardingDe(Long idEmploye) {
        Onboarding onboarding = onboardings.findFirstByIdEmploye(idEmploye)
                .orElseThrow(() -> BusinessException.notFound(
                        "Aucun onboarding pour l'employe " + idEmploye + "."));
        return ContratDtos.OnboardingDto.de(onboarding, taches.findByOnboardingIdOrderById(onboarding.getId()));
    }

    private List<OnboardingTache> creerTaches(Onboarding onboarding, LocalDate dateDebut) {
        List<OnboardingTache> resultat = new ArrayList<>();
        OnboardingTache dossier = tache(onboarding, "ADMIN_DOSSIER", "Constituer le dossier administratif",
                CategorieTache.ADMIN, "RH", -7, dateDebut, true, true, null);
        resultat.add(taches.save(dossier));
        OnboardingTache it = tache(onboarding, "IT_ACCES", "Creer les acces informatiques",
                CategorieTache.IT, "IT", -3, dateDebut, true, false, dossier.getId());
        resultat.add(taches.save(it));
        resultat.add(taches.save(tache(onboarding, "MATERIEL_POSTE", "Preparer le poste de travail",
                CategorieTache.MATERIEL, "IT", -2, dateDebut, true, false, dossier.getId())));
        resultat.add(taches.save(tache(onboarding, "ACCES_BADGE", "Remettre le badge et les acces",
                CategorieTache.ACCES, "RH", 0, dateDebut, true, false, it.getId())));
        resultat.add(taches.save(tache(onboarding, "PAIE_ENREGISTREMENT",
                "Enregistrer la paie et les avantages", CategorieTache.PAIE, "FINANCE", 0,
                dateDebut, true, false, dossier.getId())));
        resultat.add(taches.save(tache(onboarding, "SECURITE_FORMATION",
                "Former aux consignes de securite", CategorieTache.SECURITE, "RH", 0,
                dateDebut, true, false, null)));
        resultat.add(taches.save(tache(onboarding, "FORMATION_INTEGRATION",
                "Planifier la formation d'integration", CategorieTache.FORMATION, "Manager", 7,
                dateDebut, false, false, null)));
        return resultat;
    }

    private OnboardingTache tache(Onboarding onboarding, String code, String libelle,
                                  CategorieTache categorie, String responsable, int offsetJours,
                                  LocalDate dateDebut, boolean obligatoire, boolean bloquante,
                                  Long idDependDe) {
        OnboardingTache tache = new OnboardingTache();
        tache.setOnboarding(onboarding);
        tache.setCode(code);
        tache.setLibelle(libelle);
        tache.setCategorie(categorie);
        tache.setResponsable(responsable);
        tache.setEcheanceOffsetJours(offsetJours);
        tache.setEcheance(dateDebut.plusDays(offsetJours));
        tache.setObligatoire(obligatoire);
        tache.setBloquante(bloquante);
        tache.setIdDependDe(idDependDe);
        tache.setStatut(TacheStatut.A_FAIRE);
        return tache;
    }
}
