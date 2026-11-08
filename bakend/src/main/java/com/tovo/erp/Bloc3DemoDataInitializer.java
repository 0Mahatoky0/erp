package com.tovo.erp;

import com.tovo.erp.besoin.model.Decision;
import com.tovo.erp.besoin.model.DemandeRecrutement;
import com.tovo.erp.besoin.model.DemandeRecrutementPoste;
import com.tovo.erp.besoin.model.EtapeValidation;
import com.tovo.erp.besoin.model.Poste;
import com.tovo.erp.besoin.model.Service;
import com.tovo.erp.besoin.model.StatutDemande;
import com.tovo.erp.besoin.model.StatutLigne;
import com.tovo.erp.besoin.model.TypeBesoin;
import com.tovo.erp.besoin.model.Validation;
import com.tovo.erp.besoin.repository.DemandeRecrutementPosteRepository;
import com.tovo.erp.besoin.repository.DemandeRecrutementRepository;
import com.tovo.erp.besoin.repository.PosteRepository;
import com.tovo.erp.besoin.repository.ServiceRepository;
import com.tovo.erp.besoin.repository.ValidationRepository;
import com.tovo.erp.embauche.dto.PropositionDtos;
import com.tovo.erp.embauche.model.PropositionEmbauche;
import com.tovo.erp.embauche.model.TypeContratEmbauche;
import com.tovo.erp.embauche.model.TypeReponseCandidat;
import com.tovo.erp.embauche.repository.PropositionEmbaucheRepository;
import com.tovo.erp.embauche.service.PropositionService;
import com.tovo.erp.iam.model.Utilisateur;
import com.tovo.erp.iam.repository.UtilisateurRepository;
import com.tovo.erp.recrutement.model.Candidat;
import com.tovo.erp.recrutement.model.Candidature;
import com.tovo.erp.recrutement.model.CandidatureStatut;
import com.tovo.erp.recrutement.model.OffreEmploi;
import com.tovo.erp.recrutement.model.OffreStatut;
import com.tovo.erp.recrutement.model.StatutCandidat;
import com.tovo.erp.recrutement.repository.CandidatRepository;
import com.tovo.erp.recrutement.repository.CandidatureRepository;
import com.tovo.erp.recrutement.repository.OffreEmploiRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Order(2)
@ConditionalOnProperty(name = "erp.demo.bloc3.enabled", havingValue = "true", matchIfMissing = true)
public class Bloc3DemoDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(Bloc3DemoDataInitializer.class);
    private static final String EMAIL_DEMO = "anjara.rakoto@example.mg";
    private static final LocalDate DATE_DEBUT = LocalDate.now().plusDays(30);

    private final PropositionEmbaucheRepository propositions;
    private final PropositionService propositionService;
    private final UtilisateurRepository utilisateurs;
    private final ServiceRepository services;
    private final PosteRepository postes;
    private final DemandeRecrutementRepository demandes;
    private final DemandeRecrutementPosteRepository lignes;
    private final ValidationRepository validations;
    private final OffreEmploiRepository offres;
    private final CandidatRepository candidats;
    private final CandidatureRepository candidatures;

    public Bloc3DemoDataInitializer(PropositionEmbaucheRepository propositions,
                                    PropositionService propositionService,
                                    UtilisateurRepository utilisateurs,
                                    ServiceRepository services,
                                    PosteRepository postes,
                                    DemandeRecrutementRepository demandes,
                                    DemandeRecrutementPosteRepository lignes,
                                    ValidationRepository validations,
                                    OffreEmploiRepository offres,
                                    CandidatRepository candidats,
                                    CandidatureRepository candidatures) {
        this.propositions = propositions;
        this.propositionService = propositionService;
        this.utilisateurs = utilisateurs;
        this.services = services;
        this.postes = postes;
        this.demandes = demandes;
        this.lignes = lignes;
        this.validations = validations;
        this.offres = offres;
        this.candidats = candidats;
        this.candidatures = candidatures;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (propositions.count() > 0) {
            return;
        }

        Utilisateur rh = utilisateur("rh@tovo.mg");
        Utilisateur manager = utilisateur("manager@tovo.mg");
        Utilisateur finance = utilisateur("finance@tovo.mg");
        Utilisateur direction = utilisateur("direction@tovo.mg");
        if (rh == null || manager == null || finance == null || direction == null) {
            log.info("Donnees de base absentes : le jeu de demonstration Bloc 3 est ignore.");
            return;
        }
        if (candidats.findByEmailIgnoreCase(EMAIL_DEMO).isPresent()) {
            return;
        }

        List<Scenario> scenarios = List.of(
                new Scenario("Rakoto", "Anjara", EMAIL_DEMO, "1012345678",
                        LocalDate.of(1995, 5, 12), "Ventes", "Chargé de clientèle",
                        TypeContratEmbauche.CDI, Action.BROUILLON,
                        new BigDecimal("1100000"), "01"),
                new Scenario("Raveloson", "Miora", "miora.raveloson@example.mg", "2012345678",
                        LocalDate.of(1993, 8, 21), "Ventes", "Chargé de clientèle",
                        TypeContratEmbauche.CDI, Action.VALIDEE,
                        new BigDecimal("1050000"), "02"),
                new Scenario("Randriamalala", "Tojo", "tojo.randriamalala@example.mg", "3012345678",
                        LocalDate.of(1991, 3, 4), "Ventes", "Chargé de clientèle",
                        TypeContratEmbauche.CDD, Action.ENVOYEE,
                        new BigDecimal("1200000"), "03"),
                new Scenario("Razafindrakoto", "Lova", "lova.razafindrakoto@example.mg", "4012345678",
                        LocalDate.of(1990, 11, 17), "Atelier", "Chef d'atelier",
                        TypeContratEmbauche.CDI, Action.ACCEPTEE,
                        new BigDecimal("1300000"), "04"),
                new Scenario("Andriamihaja", "Fara", "fara.andriamihaja@example.mg", "5012345678",
                        LocalDate.of(1996, 2, 9), "Ressources Humaines", "Chargé de recrutement",
                        TypeContratEmbauche.CDI, Action.REFUSEE,
                        new BigDecimal("1000000"), "05"),
                new Scenario("Rasolofoson", "Hery", "hery.rasolofoson@example.mg", "6012345678",
                        LocalDate.of(1994, 7, 30), "Atelier", "Chef d'atelier",
                        TypeContratEmbauche.CDI, Action.CONTRE,
                        new BigDecimal("1450000"), "06"),
                new Scenario("Rakotomalala", "Voahangy", "voahangy.rakotomalala@example.mg", "7012345678",
                        LocalDate.of(1989, 12, 2), "Ressources Humaines", "Chargé de recrutement",
                        TypeContratEmbauche.CDI, Action.EXPIREE,
                        new BigDecimal("1000000"), "07"),
                new Scenario("Ramanantsoa", "Sitraka", "sitraka.ramanantsoa@example.mg", "8012345678",
                        LocalDate.of(1992, 6, 25), "Ventes", "Chargé de clientèle",
                        TypeContratEmbauche.CDI, Action.RETIREE,
                        new BigDecimal("1150000"), "08")
        );

        int crees = 0;
        for (Scenario scenario : scenarios) {
            if (creer(scenario, rh, manager, finance, direction)) {
                crees++;
            }
        }
        log.info("[ERP] Donnees de demonstration Bloc 3 creees : {} scenarios.", crees);
    }

    private boolean creer(Scenario scenario,
                          Utilisateur rh,
                          Utilisateur manager,
                          Utilisateur finance,
                          Utilisateur direction) {
        Service service = service(scenario.service());
        if (service == null) {
            return false;
        }
        Poste poste = postes.findByServiceIdAndIntituleIgnoreCase(service.getId(), scenario.poste())
                .orElse(null);
        if (poste == null) {
            return false;
        }

        LocalDateTime maintenant = LocalDateTime.now();
        DemandeRecrutement demande = new DemandeRecrutement();
        demande.setUtilisateur(manager);
        demande.setMotif("Renforcement de l'equipe " + service.getNom());
        demande.setDateSouhaitee(DATE_DEBUT.plusDays(15));
        demande.setStatut(StatutDemande.FINALISEE);
        demande.setDateCreation(maintenant.minusDays(30));
        demande.setDateModification(maintenant.minusDays(20));
        demande.setDateSoumission(maintenant.minusDays(25));
        demandes.save(demande);

        DemandeRecrutementPoste ligne = new DemandeRecrutementPoste();
        ligne.setDemande(demande);
        ligne.setService(service);
        ligne.setPoste(poste);
        ligne.setTypeBesoin(TypeBesoin.CREATION);
        ligne.setIntituleDemande(scenario.poste());
        ligne.setDescriptionPoste("Poste de " + scenario.poste() + " au sein du service "
                + service.getNom() + ".");
        ligne.setProfilRecherche("Experience confirmee, autonomie et sens du service.");
        ligne.setSalaireMinEstime(poste.getSalaireMin());
        ligne.setSalaireMaxEstime(poste.getSalaireMax());
        ligne.setEffectif(1);
        ligne.setNombrePourvus(0);
        ligne.setBudgetEstime(scenario.salaire());
        ligne.setStatut(StatutLigne.ACCEPTEE);
        ligne.setDateCreation(maintenant.minusDays(30));
        ligne.setDateModification(maintenant.minusDays(20));
        ligne.setDateSoumission(maintenant.minusDays(25));
        ligne.setEpisode(1);
        ligne.setEtapeCourante(EtapeValidation.DIRECTION);
        lignes.save(ligne);

        validation(ligne, rh, EtapeValidation.RH, maintenant.minusDays(24));
        validation(ligne, finance, EtapeValidation.FINANCE, maintenant.minusDays(23));
        validation(ligne, direction, EtapeValidation.DIRECTION, maintenant.minusDays(22));

        OffreEmploi offre = new OffreEmploi();
        offre.setIdDemandePoste(ligne.getId());
        offre.setIdUtilisateurCreateur(rh.getId());
        offre.setReference("OF-DEMO-" + scenario.suffixe());
        offre.setTitre(scenario.poste());
        offre.setDescription("Offre de demonstration pour le poste de " + scenario.poste() + ".");
        offre.setProfilRecherche("Profil correspondant au besoin valide par le circuit.");
        offre.setLieuTravail("Antananarivo");
        offre.setTypeContrat(scenario.type().name());
        offre.setEffectifRecherche(1);
        offre.setSalaireMin(poste.getSalaireMin());
        offre.setSalaireMax(poste.getSalaireMax());
        offre.setModeSelection("Entretien RH puis entretien manager.");
        offre.setStatut(OffreStatut.PUBLIEE);
        offre.setDatePublication(maintenant.minusDays(21));
        offre.setDateLimiteCandidature(maintenant.plusDays(20));
        offres.save(offre);

        Candidat candidat = new Candidat();
        candidat.setNom(scenario.nom());
        candidat.setPrenom(scenario.prenom());
        candidat.setEmail(scenario.email());
        candidat.setTelephone("034 " + scenario.suffixe() + " 000 00");
        candidat.setAdresse("Lot II M 12, Antananarivo");
        candidat.setCin(scenario.cin());
        candidat.setDateNaissance(scenario.naissance());
        candidat.setStatut(StatutCandidat.CANDIDAT);
        candidats.save(candidat);

        Candidature candidature = new Candidature();
        candidature.setOffre(offre);
        candidature.setCandidat(candidat);
        candidature.setMessageCandidat("Candidature de demonstration.");
        candidature.setSource("Site carriere");
        candidature.setStatut(CandidatureStatut.RETENUE);
        candidatures.save(candidature);

        PropositionDtos.Creation creation = new PropositionDtos.Creation(
                candidature.getIdCandidature(),
                poste.getId(),
                null,
                null,
                manager.getId(),
                scenario.type(),
                scenario.type() == TypeContratEmbauche.CDD ? 12 : null,
                DATE_DEBUT,
                3,
                scenario.salaire(),
                "MGA",
                "Prime de transport et assurance sante.",
                "Lundi au vendredi, 08h00 - 17h00",
                "Antananarivo",
                false);
        PropositionDtos.Detail proposition = propositionService.creer(rh, creation);

        switch (scenario.action()) {
            case BROUILLON -> {
            }
            case VALIDEE -> propositionService.valider(proposition.id(), rh);
            case ENVOYEE -> {
                propositionService.valider(proposition.id(), rh);
                propositionService.envoyer(proposition.id(), rh);
            }
            case ACCEPTEE -> {
                propositionService.valider(proposition.id(), rh);
                propositionService.envoyer(proposition.id(), rh);
                propositionService.repondrePourCandidat(proposition.id(), rh,
                        new PropositionDtos.ReponseInterne(
                                TypeReponseCandidat.ACCEPTATION,
                                "Acceptation du candidat.",
                                "Courriel de confirmation archive."));
            }
            case REFUSEE -> {
                propositionService.valider(proposition.id(), rh);
                PropositionDtos.Detail envoyee = propositionService.envoyer(proposition.id(), rh);
                propositionService.refuserPublique(envoyee.tokenAcceptation(),
                        "Proposition moins attractive que l'offre actuelle.", "127.0.0.1");
            }
            case CONTRE -> {
                propositionService.valider(proposition.id(), rh);
                PropositionDtos.Detail envoyee = propositionService.envoyer(proposition.id(), rh);
                propositionService.contreProposerPublique(envoyee.tokenAcceptation(),
                        "Souhaite un salaire de 1 500 000 MGA et une prime de transport.",
                        "127.0.0.1");
            }
            case EXPIREE -> {
                propositionService.valider(proposition.id(), rh);
                propositionService.envoyer(proposition.id(), rh);
                PropositionEmbauche entite = propositions.findById(proposition.id()).orElseThrow();
                entite.setDateExpiration(maintenant.minusDays(1));
                propositions.save(entite);
                propositions.flush();
                propositionService.expirerPropositions();
            }
            case RETIREE -> {
                propositionService.valider(proposition.id(), rh);
                propositionService.retirer(proposition.id(), rh,
                        "Poste finalement pourvu par mobilite interne.");
            }
        }
        return true;
    }

    private void validation(DemandeRecrutementPoste ligne, Utilisateur utilisateur,
                            EtapeValidation etape, LocalDateTime date) {
        Validation validation = new Validation();
        validation.setDemandePoste(ligne);
        validation.setUtilisateur(utilisateur);
        validation.setEtape(etape);
        validation.setDecision(Decision.APPROUVE);
        validation.setCommentaire("Validation de demonstration.");
        validation.setDateAction(date);
        validation.setEpisode(1);
        validations.save(validation);
    }

    private Utilisateur utilisateur(String email) {
        return utilisateurs.findByEmailIgnoreCase(email).orElse(null);
    }

    private Service service(String nom) {
        return services.findAll().stream()
                .filter(service -> service.getNom().equalsIgnoreCase(nom))
                .findFirst()
                .orElse(null);
    }

    private enum Action {
        BROUILLON, VALIDEE, ENVOYEE, ACCEPTEE, REFUSEE, CONTRE, EXPIREE, RETIREE
    }

    private record Scenario(String nom,
                            String prenom,
                            String email,
                            String cin,
                            LocalDate naissance,
                            String service,
                            String poste,
                            TypeContratEmbauche type,
                            Action action,
                            BigDecimal salaire,
                            String suffixe) {
    }
}
