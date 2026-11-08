package com.tovo.erp.embauche.service;

import com.tovo.erp.besoin.dto.DemandeDto;
import com.tovo.erp.besoin.dto.PosteDto;
import com.tovo.erp.besoin.service.DemandeService;
import com.tovo.erp.besoin.service.PosteService;
import com.tovo.erp.embauche.dto.PropositionDtos;
import com.tovo.erp.embauche.model.AuteurProposition;
import com.tovo.erp.embauche.model.HistoriqueProposition;
import com.tovo.erp.embauche.model.PropositionEmbauche;
import com.tovo.erp.embauche.model.PropositionStatut;
import com.tovo.erp.embauche.model.ReponseCandidat;
import com.tovo.erp.embauche.model.TypeReponseCandidat;
import com.tovo.erp.embauche.model.VersionProposition;
import com.tovo.erp.embauche.repository.HistoriquePropositionRepository;
import com.tovo.erp.embauche.repository.PropositionEmbaucheRepository;
import com.tovo.erp.embauche.repository.ReponseCandidatRepository;
import com.tovo.erp.embauche.repository.VersionPropositionRepository;
import com.tovo.erp.iam.model.RoleNom;
import com.tovo.erp.iam.model.Utilisateur;
import com.tovo.erp.recrutement.dto.CandidatureEmbaucheDto;
import com.tovo.erp.recrutement.model.CandidatureStatut;
import com.tovo.erp.recrutement.service.CandidatureService;
import com.tovo.erp.shared.BusinessException;
import com.tovo.erp.shared.reference.ReferenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Cycle de vie des propositions d'embauche (section 5.2 et 5.3).
 * <p>Les transitions sont controlees, chaque changement est journalise et
 * l'acceptation est deleguee a {@link EmbaucheService} pour rester atomique.
 */
@Service
public class PropositionService {

    private static final Logger log = LoggerFactory.getLogger(PropositionService.class);

    private final PropositionEmbaucheRepository propositions;
    private final VersionPropositionRepository versions;
    private final ReponseCandidatRepository reponses;
    private final HistoriquePropositionRepository historiques;
    private final ReferenceService references;
    private final CandidatureService candidatures;
    private final DemandeService demandes;
    private final PosteService postes;
    private final EmbaucheService embauche;
    private final EmbaucheProperties proprietes;

    public PropositionService(PropositionEmbaucheRepository propositions,
                              VersionPropositionRepository versions,
                              ReponseCandidatRepository reponses,
                              HistoriquePropositionRepository historiques,
                              ReferenceService references,
                              CandidatureService candidatures,
                              DemandeService demandes,
                              PosteService postes,
                              EmbaucheService embauche,
                              EmbaucheProperties proprietes) {
        this.propositions = propositions;
        this.versions = versions;
        this.reponses = reponses;
        this.historiques = historiques;
        this.references = references;
        this.candidatures = candidatures;
        this.demandes = demandes;
        this.postes = postes;
        this.embauche = embauche;
        this.proprietes = proprietes;
    }

    @Transactional
    public PropositionDtos.Detail creer(Utilisateur auteur, PropositionDtos.Creation demande) {
        CandidatureEmbaucheDto candidature = candidatures.detailsPourEmbauche(demande.idCandidature());
        if (candidature.statut() != CandidatureStatut.RETENUE) {
            throw new BusinessException("CANDIDATURE_NON_RETENUE",
                    "Une proposition ne peut etre creee que pour une candidature RETENUE (statut : "
                            + candidature.statut() + ").");
        }
        if (propositions.existsByIdCandidatureAndActiveTrue(demande.idCandidature())) {
            throw new BusinessException("PROPOSITION_ACTIVE_EXISTANTE",
                    "Une proposition active existe deja pour cette candidature.");
        }
        DemandeDto.LigneDetail ligne = ligneDe(candidature);
        controlerBesoinDisponible(ligne);
        PosteDto.Liste poste = resoudrePoste(demande, ligne);
        boolean derogation = controlerSalaire(demande, poste);

        PropositionEmbauche proposition = new PropositionEmbauche();
        proposition.setReference(references.suivante("PROPOSITION_EMBAUCHE", "PE", 4));
        proposition.setIdCandidature(demande.idCandidature());
        proposition.setVersion(1);
        proposition.setActive(true);
        proposition.setStatut(PropositionStatut.BROUILLON);
        proposition.setIdUtilisateurCreateur(auteur.getId());
        appliquer(proposition, demande, poste, derogation);
        propositions.save(proposition);
        enregistrerVersion(proposition, auteur.getId());
        journaliser(proposition, null, PropositionStatut.BROUILLON, AuteurProposition.UTILISATEUR,
                auteur.getId(), "Creation de la proposition.");
        return detail(proposition.getId());
    }

    @Transactional
    public PropositionDtos.Detail modifier(Long id, Utilisateur auteur, PropositionDtos.Creation demande) {
        PropositionEmbauche proposition = charger(id);
        controlerBrouillon(proposition);
        CandidatureEmbaucheDto candidature = candidatures.detailsPourEmbauche(proposition.getIdCandidature());
        DemandeDto.LigneDetail ligne = ligneDe(candidature);
        PosteDto.Liste poste = resoudrePoste(demande, ligne);
        boolean derogation = controlerSalaire(demande, poste);
        appliquer(proposition, demande, poste, derogation);
        proposition.setVersion(proposition.getVersion() + 1);
        propositions.save(proposition);
        enregistrerVersion(proposition, auteur.getId());
        return detail(proposition.getId());
    }

    @Transactional
    public PropositionDtos.Detail valider(Long id, Utilisateur auteur) {
        PropositionEmbauche proposition = charger(id);
        controlerBrouillon(proposition);
        if (proposition.isDerogationFinanceDirection()
                && !aLeRole(auteur, RoleNom.FINANCE, RoleNom.DIRECTION)) {
            throw BusinessException.forbidden(
                    "Une derogation salariale doit etre validee par la Finance ou la Direction.");
        }
        changerStatut(proposition, PropositionStatut.VALIDEE_INTERNE,
                AuteurProposition.UTILISATEUR, auteur.getId(), "Validation interne.");
        proposition.setIdUtilisateurValidateur(auteur.getId());
        propositions.save(proposition);
        return detail(id);
    }

    @Transactional
    public PropositionDtos.Detail envoyer(Long id, Utilisateur auteur) {
        PropositionEmbauche proposition = charger(id);
        if (proposition.getStatut() != PropositionStatut.VALIDEE_INTERNE) {
            throw new BusinessException("PROPOSITION_NON_VALIDEE",
                    "La proposition doit etre validee avant d'etre envoyee (statut : "
                            + proposition.getStatut() + ").");
        }
        LocalDateTime maintenant = LocalDateTime.now();
        proposition.setTokenAcceptation(UUID.randomUUID().toString().replace("-", ""));
        proposition.setDateEnvoi(maintenant);
        proposition.setDateExpiration(maintenant.plusDays(proprietes.validitePropositionJours()));
        changerStatut(proposition, PropositionStatut.ENVOYEE,
                AuteurProposition.UTILISATEUR, auteur.getId(),
                "Envoi au candidat, valable " + proprietes.validitePropositionJours() + " jours.");
        return detail(id);
    }

    @Transactional
    public PropositionDtos.Detail retirer(Long id, Utilisateur auteur, String motif) {
        PropositionEmbauche proposition = charger(id);
        Set<PropositionStatut> retirables = Set.of(PropositionStatut.BROUILLON,
                PropositionStatut.VALIDEE_INTERNE, PropositionStatut.ENVOYEE);
        if (!retirables.contains(proposition.getStatut())) {
            throw new BusinessException("PROPOSITION_NON_RETIRABLE",
                    "Cette proposition ne peut plus etre retiree (statut : "
                            + proposition.getStatut() + ").");
        }
        proposition.setActive(false);
        changerStatut(proposition, PropositionStatut.RETIREE,
                AuteurProposition.UTILISATEUR, auteur.getId(),
                motif == null || motif.isBlank() ? "Retrait par l'entreprise." : motif);
        return detail(id);
    }

    @Transactional
    public PropositionDtos.Detail prolonger(Long id, Utilisateur auteur, int jours) {
        PropositionEmbauche proposition = charger(id);
        if (proposition.getStatut() != PropositionStatut.ENVOYEE) {
            throw new BusinessException("PROPOSITION_NON_ENVOYEE",
                    "Seule une proposition envoyee peut etre prolongee (statut : "
                            + proposition.getStatut() + ").");
        }
        LocalDateTime base = proposition.getDateExpiration() != null
                && proposition.getDateExpiration().isAfter(LocalDateTime.now())
                ? proposition.getDateExpiration() : LocalDateTime.now();
        proposition.setDateExpiration(base.plusDays(jours));
        propositions.save(proposition);
        return detail(id);
    }

    /** Reponse saisie par un RH pour le compte du candidat (canal papier / email). */
    @Transactional
    public PropositionDtos.Detail repondrePourCandidat(Long id, Utilisateur auteur,
                                                        PropositionDtos.ReponseInterne reponse) {
        PropositionEmbauche proposition = charger(id);
        if (reponse.typeReponse() == TypeReponseCandidat.ACCEPTATION) {
            embauche.accepterInterne(id, auteur, reponse.preuve());
            return detail(id);
        }
        return refuserOuContreProposer(proposition, auteur.getId(), AuteurProposition.UTILISATEUR,
                reponse.typeReponse(), reponse.commentaire(), reponse.preuve(), null);
    }

    /** Refus direct du candidat via son lien securise. */
    @Transactional
    public PropositionDtos.Detail refuserPublique(String token, String motif, String ip) {
        PropositionEmbauche proposition = parToken(token);
        return refuserOuContreProposer(proposition, null, AuteurProposition.CANDIDAT,
                TypeReponseCandidat.REFUS, motif, null, ip);
    }

    /** Contre-proposition directe du candidat via son lien securise. */
    @Transactional
    public PropositionDtos.Detail contreProposerPublique(String token, String message, String ip) {
        PropositionEmbauche proposition = parToken(token);
        return refuserOuContreProposer(proposition, null, AuteurProposition.CANDIDAT,
                TypeReponseCandidat.CONTRE_PROPOSITION, message, null, ip);
    }

    @Transactional(readOnly = true)
    public List<PropositionDtos.Resume> lister(PropositionStatut statut) {
        List<PropositionEmbauche> liste = statut == null
                ? propositions.findAllByOrderByDateCreationDesc()
                : propositions.findByStatutOrderByDateCreationDesc(statut);
        if (liste.isEmpty()) {
            return List.of();
        }
        Map<Long, CandidatureEmbaucheDto> candidaturesParId = candidatures.resumesParIds(
                liste.stream().map(PropositionEmbauche::getIdCandidature).collect(Collectors.toSet()));
        Map<Long, String> postesParId = postes.nomsParIds(
                liste.stream().map(PropositionEmbauche::getIdPoste)
                        .filter(id -> id != null).collect(Collectors.toSet()));
        return liste.stream()
                .map(p -> PropositionDtos.versResume(p,
                        candidaturesParId.get(p.getIdCandidature()),
                        postesParId.get(p.getIdPoste())))
                .toList();
    }

    @Transactional(readOnly = true)
    public PropositionDtos.Detail detail(Long id) {
        return construireDetail(charger(id));
    }

    @Transactional(readOnly = true)
    public PropositionDtos.VuePublique vuePublique(String token) {
        PropositionEmbauche proposition = parToken(token);
        CandidatureEmbaucheDto candidature = candidatures.detailsPourEmbauche(proposition.getIdCandidature());
        String poste = proposition.getIdPoste() == null ? null
                : postes.trouver(proposition.getIdPoste()).intitule();
        boolean possible = proposition.getStatut() == PropositionStatut.ENVOYEE
                && (proposition.getDateExpiration() == null
                || proposition.getDateExpiration().isAfter(LocalDateTime.now()));
        return new PropositionDtos.VuePublique(
                proposition.getReference(),
                proposition.getStatut(),
                candidature.prenom(),
                candidature.nom(),
                poste,
                proposition.getTypeContrat(),
                proposition.getDateDebutPrevue(),
                proposition.getSalaireBrutMensuel(),
                proposition.getDevise(),
                proposition.getLieuTravail(),
                proposition.getPrimesAvantages(),
                proposition.getHoraires(),
                proposition.getDateExpiration(),
                possible);
    }

    /** Job d'expiration : passe les propositions echues en EXPIREE. */
    @Scheduled(fixedDelayString = "${erp.embauche.verification-expiration-ms:3600000}")
    @Transactional
    public void expirerPropositions() {
        List<PropositionEmbauche> echues = propositions
                .findByActiveTrueAndStatutAndDateExpirationBefore(
                        PropositionStatut.ENVOYEE, LocalDateTime.now());
        for (PropositionEmbauche proposition : echues) {
            proposition.setStatut(PropositionStatut.EXPIREE);
            proposition.setActive(false);
            propositions.save(proposition);
            journaliser(proposition, PropositionStatut.ENVOYEE, PropositionStatut.EXPIREE,
                    AuteurProposition.SYSTEME, null, "Expiration automatique.");
            candidatures.marquerExpiree(proposition.getIdCandidature());
            log.info("Proposition {} expiree.", proposition.getReference());
        }
    }

    private PropositionDtos.Detail refuserOuContreProposer(PropositionEmbauche proposition,
                                                            Long idUtilisateur,
                                                            AuteurProposition auteur,
                                                            TypeReponseCandidat type,
                                                            String commentaire,
                                                            String preuve,
                                                            String ip) {
        if (proposition.getStatut() != PropositionStatut.ENVOYEE) {
            throw new BusinessException("PROPOSITION_NON_ENVOYEE",
                    "Cette proposition n'est pas en attente de reponse (statut : "
                            + proposition.getStatut() + ").");
        }
        LocalDateTime maintenant = LocalDateTime.now();
        if (proposition.getDateExpiration() != null
                && proposition.getDateExpiration().isBefore(maintenant)) {
            throw new BusinessException("PROPOSITION_EXPIREE",
                    "Cette proposition a expire le " + proposition.getDateExpiration() + ".");
        }
        if (type == TypeReponseCandidat.REFUS) {
            proposition.setStatut(PropositionStatut.REFUSEE);
            proposition.setDateReponse(maintenant);
            proposition.setMotifRefus(commentaire);
            proposition.setPreuveReponse(preuve);
            proposition.setActive(false);
            propositions.save(proposition);
            enregistrerReponse(proposition, type, commentaire, preuve, ip);
            journaliser(proposition, PropositionStatut.ENVOYEE, PropositionStatut.REFUSEE,
                    auteur, idUtilisateur, "Refus du candidat.");
            candidatures.marquerRefuseeParCandidat(proposition.getIdCandidature(), idUtilisateur,
                    commentaire);
            return construireDetail(proposition);
        }
        if (type == TypeReponseCandidat.CONTRE_PROPOSITION) {
            proposition.setStatut(PropositionStatut.CONTRE_PROPOSEE);
            proposition.setDateReponse(maintenant);
            proposition.setMotifRefus(commentaire);
            proposition.setActive(false);
            propositions.save(proposition);
            propositions.flush();
            enregistrerReponse(proposition, type, commentaire, preuve, ip);
            journaliser(proposition, PropositionStatut.ENVOYEE, PropositionStatut.CONTRE_PROPOSEE,
                    auteur, idUtilisateur, "Contre-proposition du candidat.");

            PropositionEmbauche nouvelle = copier(proposition);
            nouvelle.setReference(references.suivante("PROPOSITION_EMBAUCHE", "PE", 4));
            nouvelle.setVersion(proposition.getVersion() + 1);
            nouvelle.setActive(true);
            nouvelle.setStatut(PropositionStatut.BROUILLON);
            nouvelle.setTokenAcceptation(null);
            nouvelle.setDateEnvoi(null);
            nouvelle.setDateExpiration(null);
            nouvelle.setDateReponse(null);
            nouvelle.setMotifRefus(null);
            nouvelle.setPreuveReponse(null);
            nouvelle.setIpReponse(null);
            nouvelle.setEmpreinteReponse(null);
            nouvelle.setIdUtilisateurCreateur(idUtilisateur);
            nouvelle.setIdUtilisateurValidateur(null);
            nouvelle.setIdEmployeCree(null);
            nouvelle.setIdContratCree(null);
            nouvelle.setIdAffectationCreee(null);
            nouvelle.setIdOnboardingCree(null);
            nouvelle.setIdDocumentPdf(null);
            propositions.save(nouvelle);
            enregistrerVersion(nouvelle, idUtilisateur);
            journaliser(nouvelle, null, PropositionStatut.BROUILLON, auteur, idUtilisateur,
                    "Nouvelle version apres contre-proposition.");

            proposition.setStatut(PropositionStatut.RETIREE);
            propositions.save(proposition);
            journaliser(proposition, PropositionStatut.CONTRE_PROPOSEE, PropositionStatut.RETIREE,
                    auteur, idUtilisateur, "Remplacee par la version " + nouvelle.getVersion() + ".");
            return construireDetail(nouvelle);
        }
        throw new BusinessException("REPONSE_INVALIDE", "Type de reponse non pris en charge.");
    }

    private PropositionEmbauche copier(PropositionEmbauche source) {
        PropositionEmbauche cible = new PropositionEmbauche();
        cible.setIdCandidature(source.getIdCandidature());
        cible.setIdPoste(source.getIdPoste());
        cible.setIdDepartement(source.getIdDepartement());
        cible.setIdService(source.getIdService());
        cible.setIdManager(source.getIdManager());
        cible.setTypeContrat(source.getTypeContrat());
        cible.setDureeMois(source.getDureeMois());
        cible.setDateDebutPrevue(source.getDateDebutPrevue());
        cible.setPeriodeEssaiMois(source.getPeriodeEssaiMois());
        cible.setSalaireBrutMensuel(source.getSalaireBrutMensuel());
        cible.setDevise(source.getDevise());
        cible.setPrimesAvantages(source.getPrimesAvantages());
        cible.setHoraires(source.getHoraires());
        cible.setLieuTravail(source.getLieuTravail());
        cible.setDerogationFinanceDirection(source.isDerogationFinanceDirection());
        return cible;
    }

    private PropositionDtos.Detail construireDetail(PropositionEmbauche proposition) {
        CandidatureEmbaucheDto candidature = candidatures.detailsPourEmbauche(proposition.getIdCandidature());
        PosteDto.Liste poste = proposition.getIdPoste() == null ? null
                : postes.trouver(proposition.getIdPoste());
        return new PropositionDtos.Detail(
                proposition.getId(),
                proposition.getReference(),
                proposition.getVersion(),
                proposition.isActive(),
                proposition.getStatut(),
                proposition.getIdCandidature(),
                candidature.idCandidat(),
                candidature.nom(),
                candidature.prenom(),
                candidature.email(),
                candidature.telephone(),
                candidature.adresse(),
                candidature.cin(),
                candidature.dateNaissance(),
                candidature.idOffre(),
                candidature.referenceOffre(),
                candidature.titreOffre(),
                candidature.idDemandePoste(),
                proposition.getIdPoste(),
                poste == null ? null : poste.intitule(),
                proposition.getIdDepartement(),
                proposition.getIdService(),
                poste == null ? null : poste.service(),
                proposition.getIdManager(),
                proposition.getTypeContrat(),
                proposition.getDureeMois(),
                proposition.getDateDebutPrevue(),
                proposition.getPeriodeEssaiMois(),
                proposition.getSalaireBrutMensuel(),
                proposition.getDevise(),
                proposition.getPrimesAvantages(),
                proposition.getHoraires(),
                proposition.getLieuTravail(),
                proposition.getDateEnvoi(),
                proposition.getDateExpiration(),
                proposition.getTokenAcceptation(),
                proposition.getDateReponse(),
                proposition.getMotifRefus(),
                proposition.getPreuveReponse(),
                proposition.isDerogationFinanceDirection(),
                proposition.getIdUtilisateurCreateur(),
                proposition.getIdUtilisateurValidateur(),
                proposition.getIdEmployeCree(),
                proposition.getIdContratCree(),
                proposition.getIdAffectationCreee(),
                proposition.getIdOnboardingCree(),
                proposition.getIdDocumentPdf(),
                proposition.getDateCreation(),
                proposition.getDateModification(),
                historiques.findByPropositionIdOrderByDateActionAsc(proposition.getId()).stream()
                        .map(PropositionDtos.HistoriqueDto::de).toList(),
                versions.findByPropositionIdOrderByNumeroVersionDesc(proposition.getId()).stream()
                        .map(PropositionDtos.VersionDto::de).toList(),
                reponses.findByPropositionIdOrderByDateReponseDesc(proposition.getId()).stream()
                        .map(PropositionDtos.ReponseDto::de).toList());
    }

    private void appliquer(PropositionEmbauche proposition, PropositionDtos.Creation demande,
                           PosteDto.Liste poste, boolean derogation) {
        controlerDateDebut(demande.dateDebutPrevue());
        if (demande.typeContrat() == com.tovo.erp.embauche.model.TypeContratEmbauche.CDD
                && (demande.dureeMois() == null || demande.dureeMois() <= 0)) {
            throw new BusinessException("DUREE_CDD_INVALIDE",
                    "Un CDD doit preciser une duree en mois strictement positive.");
        }
        proposition.setIdPoste(poste.id());
        proposition.setIdService(poste.idService());
        proposition.setIdDepartement(poste.idDepartement());
        proposition.setIdManager(demande.idManager());
        proposition.setTypeContrat(demande.typeContrat());
        proposition.setDureeMois(demande.dureeMois());
        proposition.setDateDebutPrevue(demande.dateDebutPrevue());
        proposition.setPeriodeEssaiMois(demande.periodeEssaiMois() == null
                ? 1 : demande.periodeEssaiMois());
        proposition.setSalaireBrutMensuel(demande.salaireBrutMensuel());
        proposition.setDevise(demande.devise() == null || demande.devise().isBlank()
                ? "MGA" : demande.devise());
        proposition.setPrimesAvantages(demande.primesAvantages());
        proposition.setHoraires(demande.horaires());
        proposition.setLieuTravail(demande.lieuTravail());
        proposition.setDerogationFinanceDirection(derogation);
    }

    private DemandeDto.LigneDetail ligneDe(CandidatureEmbaucheDto candidature) {
        if (candidature.idDemandePoste() == null) {
            throw new BusinessException("LIGNE_BESOIN_INTROUVABLE",
                    "La candidature n'est rattachee a aucune ligne de besoin.");
        }
        return demandes.detailLignePourEmbauche(candidature.idDemandePoste());
    }

    private void controlerBesoinDisponible(DemandeDto.LigneDetail ligne) {
        if (ligne.effectif() != null && ligne.nombrePourvus() != null
                && ligne.nombrePourvus() >= ligne.effectif()) {
            throw new BusinessException("BESOIN_POURVU",
                    "Tous les postes de cette ligne de besoin sont deja pourvus.");
        }
    }

    private PosteDto.Liste resoudrePoste(PropositionDtos.Creation demande, DemandeDto.LigneDetail ligne) {
        Long idPoste = demande.idPoste() != null ? demande.idPoste() : ligne.idPoste();
        if (idPoste == null) {
            throw new BusinessException("POSTE_OBLIGATOIRE",
                    "Une proposition doit viser un poste du catalogue.");
        }
        PosteDto.Liste poste = postes.trouver(idPoste);
        if (!poste.actif()) {
            throw new BusinessException("POSTE_INACTIF",
                    "Le poste " + poste.intitule() + " est desactive.");
        }
        return poste;
    }

    /**
     * Verifie le salaire : minimum legal toujours exige, fourchette du poste
     * controlee sauf derogation explicite Finance / Direction (RG-B3-11).
     */
    private boolean controlerSalaire(PropositionDtos.Creation demande, PosteDto.Liste poste) {
        BigDecimal salaire = demande.salaireBrutMensuel();
        if (salaire == null || salaire.compareTo(proprietes.salaireMinimumLegal()) < 0) {
            throw new BusinessException("SALAIRE_SOUS_MINIMUM",
                    "Le salaire propose ne peut pas etre inferieur au minimum legal de "
                            + proprietes.salaireMinimumLegal() + ".");
        }
        boolean horsFourchette = (poste.salaireMin() != null
                && salaire.compareTo(poste.salaireMin()) < 0)
                || (poste.salaireMax() != null && salaire.compareTo(poste.salaireMax()) > 0);
        if (horsFourchette && !Boolean.TRUE.equals(demande.derogationFinanceDirection())) {
            throw new BusinessException("SALAIRE_HORS_FOURCHETTE",
                    "Le salaire est hors de la fourchette du poste ("
                            + poste.salaireMin() + " - " + poste.salaireMax()
                            + "). Une derogation Finance / Direction est obligatoire.");
        }
        return horsFourchette || Boolean.TRUE.equals(demande.derogationFinanceDirection());
    }

    private void controlerDateDebut(LocalDate dateDebut) {
        if (dateDebut == null) {
            throw new BusinessException("DATE_DEBUT_INVALIDE", "La date de debut est obligatoire.");
        }
        LocalDate minimum = LocalDate.now().plusDays(proprietes.delaiMinimumJours());
        if (dateDebut.isBefore(minimum)) {
            throw new BusinessException("DATE_DEBUT_INVALIDE",
                    "La date de debut doit etre au moins " + proprietes.delaiMinimumJours()
                            + " jours apres aujourd'hui (minimum : " + minimum + ").");
        }
    }

    private void controlerBrouillon(PropositionEmbauche proposition) {
        if (!proposition.estBrouillon()) {
            throw new BusinessException("PROPOSITION_NON_MODIFIABLE",
                    "Seule une proposition BROUILLON peut etre modifiee (statut : "
                            + proposition.getStatut() + ").");
        }
    }

    private void changerStatut(PropositionEmbauche proposition, PropositionStatut nouveau,
                               AuteurProposition auteur, Long idUtilisateur, String motif) {
        PropositionStatut ancien = proposition.getStatut();
        proposition.setStatut(nouveau);
        propositions.save(proposition);
        journaliser(proposition, ancien, nouveau, auteur, idUtilisateur, motif);
    }

    private void enregistrerVersion(PropositionEmbauche proposition, Long idUtilisateur) {
        VersionProposition version = new VersionProposition();
        version.setProposition(proposition);
        version.setNumeroVersion(proposition.getVersion());
        version.setContenu(contenu(proposition));
        version.setIdUtilisateur(idUtilisateur);
        versions.save(version);
    }

    private void enregistrerReponse(PropositionEmbauche proposition, TypeReponseCandidat type,
                                    String commentaire, String preuve, String ip) {
        ReponseCandidat reponse = new ReponseCandidat();
        reponse.setProposition(proposition);
        reponse.setTypeReponse(type);
        reponse.setCommentaire(commentaire);
        reponse.setPreuve(preuve);
        reponse.setIp(ip);
        reponses.save(reponse);
    }

    private void journaliser(PropositionEmbauche proposition, PropositionStatut ancien,
                             PropositionStatut nouveau, AuteurProposition auteur, Long idUtilisateur,
                             String motif) {
        if (ancien == nouveau) {
            return;
        }
        HistoriqueProposition historique = new HistoriqueProposition();
        historique.setProposition(proposition);
        historique.setAuteurType(auteur);
        historique.setIdUtilisateur(idUtilisateur);
        historique.setAncienStatut(ancien);
        historique.setNouveauStatut(nouveau);
        historique.setMotif(motif);
        historiques.save(historique);
    }

    private String contenu(PropositionEmbauche p) {
        return "reference=" + p.getReference()
                + "\nversion=" + p.getVersion()
                + "\nidCandidature=" + p.getIdCandidature()
                + "\nposte=" + p.getIdPoste()
                + "\nservice=" + p.getIdService()
                + "\ndepartement=" + p.getIdDepartement()
                + "\nmanager=" + p.getIdManager()
                + "\ntypeContrat=" + p.getTypeContrat()
                + "\ndureeMois=" + p.getDureeMois()
                + "\ndateDebut=" + p.getDateDebutPrevue()
                + "\nperiodeEssaiMois=" + p.getPeriodeEssaiMois()
                + "\nsalaire=" + p.getSalaireBrutMensuel() + " " + p.getDevise()
                + "\nprimes=" + p.getPrimesAvantages()
                + "\nhoraires=" + p.getHoraires()
                + "\nlieu=" + p.getLieuTravail()
                + "\nderogation=" + p.isDerogationFinanceDirection();
    }

    private boolean aLeRole(Utilisateur utilisateur, RoleNom... roles) {
        for (RoleNom role : roles) {
            if (role.name().equals(utilisateur.getRole().getNom())) {
                return true;
            }
        }
        return false;
    }

    private PropositionEmbauche charger(Long id) {
        return propositions.findById(id)
                .orElseThrow(() -> BusinessException.notFound(
                        "La proposition " + id + " n'existe pas."));
    }

    private PropositionEmbauche parToken(String token) {
        return propositions.findByTokenAcceptation(token)
                .orElseThrow(() -> BusinessException.notFound(
                        "Ce lien d'acceptation est invalide ou a deja ete utilise."));
    }
}
