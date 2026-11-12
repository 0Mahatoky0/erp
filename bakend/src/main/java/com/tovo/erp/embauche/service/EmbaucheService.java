package com.tovo.erp.embauche.service;

import com.tovo.erp.besoin.service.DemandeService;
import com.tovo.erp.contrat.dto.ContratDtos;
import com.tovo.erp.contrat.service.ContratService;
import com.tovo.erp.embauche.dto.PropositionDtos;
import com.tovo.erp.embauche.model.AuteurProposition;
import com.tovo.erp.embauche.model.HistoriqueProposition;
import com.tovo.erp.embauche.model.PropositionEmbauche;
import com.tovo.erp.embauche.model.PropositionStatut;
import com.tovo.erp.embauche.model.ReponseCandidat;
import com.tovo.erp.embauche.model.TypeReponseCandidat;
import com.tovo.erp.embauche.repository.HistoriquePropositionRepository;
import com.tovo.erp.embauche.repository.PropositionEmbaucheRepository;
import com.tovo.erp.embauche.repository.ReponseCandidatRepository;
import com.tovo.erp.employe.dto.DossierEmbaucheRequest;
import com.tovo.erp.employe.dto.EmployeEmbaucheDto;
import com.tovo.erp.employe.service.EmployeService;
import com.tovo.erp.shared.UtilisateurConnecte;
import com.tovo.erp.recrutement.dto.CandidatureEmbaucheDto;
import com.tovo.erp.recrutement.service.CandidatureService;
import com.tovo.erp.shared.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Orchestration atomique et idempotente de l'acceptation d'une proposition
 * (section 5.4, etapes 4 a 9). Le dossier salarie, le contrat, l'affectation,
 * l'onboarding et la mise a jour de la candidature sont crees dans la meme
 * transaction : aucune embauche partielle n'est possible.
 */
@Service
public class EmbaucheService {

    private final PropositionEmbaucheRepository propositions;
    private final ReponseCandidatRepository reponses;
    private final HistoriquePropositionRepository historiques;
    private final CandidatureService candidatures;
    private final DemandeService demandes;
    private final EmployeService employes;
    private final ContratService contrats;

    public EmbaucheService(PropositionEmbaucheRepository propositions,
                           ReponseCandidatRepository reponses,
                           HistoriquePropositionRepository historiques,
                           CandidatureService candidatures,
                           DemandeService demandes,
                           EmployeService employes,
                           ContratService contrats) {
        this.propositions = propositions;
        this.reponses = reponses;
        this.historiques = historiques;
        this.candidatures = candidatures;
        this.demandes = demandes;
        this.employes = employes;
        this.contrats = contrats;
    }

    /** Acceptation saisie par un RH (canal papier ou email avec preuve). */
    @Transactional
    public PropositionDtos.ResultatAcceptation accepterInterne(Long idProposition,
                                                               UtilisateurConnecte auteur,
                                                               String preuve) {
        PropositionEmbauche proposition = propositions.findById(idProposition)
                .orElseThrow(() -> BusinessException.notFound(
                        "La proposition " + idProposition + " n'existe pas."));
        return accepter(proposition, auteur.getId(), AuteurProposition.UTILISATEUR,
                preuve, null, null);
    }

    /** Acceptation directe du candidat via le lien securise recu par email. */
    @Transactional
    public PropositionDtos.ResultatAcceptation accepterPublique(String token, String ip, String empreinte) {
        PropositionEmbauche proposition = propositions.findByTokenAcceptation(token)
                .orElseThrow(() -> BusinessException.notFound(
                        "Ce lien d'acceptation est invalide ou a deja ete utilise."));
        return accepter(proposition, null, AuteurProposition.CANDIDAT, null, ip, empreinte);
    }

    private PropositionDtos.ResultatAcceptation accepter(PropositionEmbauche proposition,
                                                          Long idUtilisateur,
                                                          AuteurProposition auteurType,
                                                          String preuve,
                                                          String ip,
                                                          String empreinte) {
        if (proposition.getStatut() == PropositionStatut.ACCEPTEE) {
            return resultatExistant(proposition);
        }
        if (proposition.getStatut() != PropositionStatut.ENVOYEE) {
            throw new BusinessException("PROPOSITION_NON_ENVOYEE",
                    "Cette proposition n'est pas en attente de reponse (statut : "
                            + proposition.getStatut() + ").");
        }
        LocalDateTime maintenant = LocalDateTime.now();
        if (proposition.getDateExpiration() != null
                && proposition.getDateExpiration().isBefore(maintenant)) {
            proposition.setStatut(PropositionStatut.EXPIREE);
            proposition.setActive(false);
            propositions.save(proposition);
            journaliser(proposition, PropositionStatut.ENVOYEE, PropositionStatut.EXPIREE,
                    AuteurProposition.SYSTEME, null, "Proposition expiree.");
            candidatures.marquerExpiree(proposition.getIdCandidature());
            throw new BusinessException("PROPOSITION_EXPIREE",
                    "Cette proposition a expire le " + proposition.getDateExpiration() + ".");
        }

        CandidatureEmbaucheDto candidature = candidatures.detailsPourEmbauche(proposition.getIdCandidature());
        if (!candidature.estEmbauchable()) {
            throw new BusinessException("CANDIDATURE_NON_RETENUE",
                    "La candidature n'est plus dans un etat permettant l'embauche (statut : "
                            + candidature.statut() + ").");
        }
        if (candidature.idDemandePoste() != null) {
            var ligne = demandes.detailLignePourEmbauche(candidature.idDemandePoste());
            if (ligne.effectif() != null && ligne.nombrePourvus() != null
                    && ligne.nombrePourvus() >= ligne.effectif()) {
                throw new BusinessException("BESOIN_POURVU",
                        "Tous les postes de cette ligne de besoin sont deja pourvus.");
            }
        }

        EmployeEmbaucheDto salarie = employes.creerOuReactiverDossier(
                new DossierEmbaucheRequest(
                        candidature.nom(),
                        candidature.prenom(),
                        candidature.email(),
                        candidature.telephone(),
                        candidature.cin(),
                        candidature.dateNaissance(),
                        null,
                        candidature.adresse(),
                        null,
                        null,
                        null),
                candidature.idCandidat(),
                proposition.getIdPoste(),
                proposition.getDateDebutPrevue());

        LocalDate dateFinPrevue = proposition.getDureeMois() == null ? null
                : proposition.getDateDebutPrevue().plusMonths(proposition.getDureeMois())
                        .minusDays(1);
        ContratDtos.ContratEmbaucheDto contrat = contrats.creerContratEmbauche(
                new ContratDtos.CreationContratRequest(
                        salarie.id(),
                        proposition.getId(),
                        proposition.getTypeContrat().name(),
                        proposition.getDateDebutPrevue(),
                        dateFinPrevue,
                        proposition.getSalaireBrutMensuel(),
                        proposition.getDevise(),
                        proposition.getPrimesAvantages(),
                        proposition.getHoraires(),
                        proposition.getLieuTravail(),
                        proposition.getMotifCdd(),
                        proposition.getIdPoste(),
                        proposition.getIdDepartement(),
                        proposition.getIdService(),
                        proposition.getIdManager(),
                        proposition.getPeriodeEssaiMois(),
                        idUtilisateur == null ? proposition.getIdUtilisateurCreateur() : idUtilisateur));

        if (candidature.idDemandePoste() != null) {
            demandes.enregistrerEmbauche(candidature.idDemandePoste());
        }
        candidatures.marquerEmbaucheePar(proposition.getIdCandidature(),
                auteurType == AuteurProposition.CANDIDAT ? "CANDIDAT" : "UTILISATEUR",
                idUtilisateur,
                "Acceptation de la proposition " + proposition.getReference());

        proposition.setStatut(PropositionStatut.ACCEPTEE);
        proposition.setDateReponse(maintenant);
        proposition.setActive(false);
        proposition.setIdEmployeCree(salarie.id());
        proposition.setIdContratCree(contrat.idContrat());
        proposition.setIdAffectationCreee(contrat.idAffectation());
        proposition.setIdOnboardingCree(contrat.idOnboarding());
        proposition.setIdDocumentPdf(contrat.documentPdfId());
        if (auteurType == AuteurProposition.CANDIDAT) {
            proposition.setIpReponse(ip);
            proposition.setEmpreinteReponse(empreinte);
        } else {
            proposition.setPreuveReponse(preuve);
        }
        propositions.save(proposition);

        ReponseCandidat reponse = new ReponseCandidat();
        reponse.setProposition(proposition);
        reponse.setTypeReponse(TypeReponseCandidat.ACCEPTATION);
        reponse.setCommentaire("Acceptation de la proposition.");
        reponse.setIp(ip);
        reponse.setEmpreinte(empreinte);
        reponse.setPreuve(preuve);
        reponses.save(reponse);
        journaliser(proposition, PropositionStatut.ENVOYEE, PropositionStatut.ACCEPTEE,
                auteurType, idUtilisateur, "Proposition acceptee.");

        return new PropositionDtos.ResultatAcceptation(
                proposition.getId(),
                proposition.getReference(),
                proposition.getStatut(),
                salarie.id(),
                salarie.matricule(),
                contrat.idContrat(),
                contrat.numero(),
                proposition.getTypeContrat(),
                proposition.getDateDebutPrevue(),
                dateFinPrevue,
                proposition.getSalaireBrutMensuel(),
                proposition.getDevise(),
                contrat.idAffectation(),
                contrat.idOnboarding(),
                contrat.idTaches(),
                contrat.documentPdfId());
    }

    private PropositionDtos.ResultatAcceptation resultatExistant(PropositionEmbauche proposition) {
        ContratDtos.ContratDto contrat = proposition.getIdContratCree() == null ? null
                : contrats.contrat(proposition.getIdContratCree());
        EmployeEmbaucheDto salarie = proposition.getIdEmployeCree() == null ? null
                : employes.dossier(proposition.getIdEmployeCree());
        List<Long> idTaches = proposition.getIdOnboardingCree() == null ? List.of()
                : contrats.tachesDe(proposition.getIdOnboardingCree()).stream()
                        .map(ContratDtos.TacheDto::id)
                        .toList();
        return new PropositionDtos.ResultatAcceptation(
                proposition.getId(),
                proposition.getReference(),
                proposition.getStatut(),
                proposition.getIdEmployeCree(),
                salarie == null ? null : salarie.matricule(),
                proposition.getIdContratCree(),
                contrat == null ? null : contrat.numero(),
                proposition.getTypeContrat(),
                proposition.getDateDebutPrevue(),
                contrat == null ? null : contrat.dateFinPrevue(),
                proposition.getSalaireBrutMensuel(),
                proposition.getDevise(),
                proposition.getIdAffectationCreee(),
                proposition.getIdOnboardingCree(),
                idTaches,
                proposition.getIdDocumentPdf());
    }

    private void journaliser(PropositionEmbauche proposition, PropositionStatut ancien,
                             PropositionStatut nouveau, AuteurProposition auteur, Long idUtilisateur,
                             String motif) {
        HistoriqueProposition historique = new HistoriqueProposition();
        historique.setProposition(proposition);
        historique.setAuteurType(auteur);
        historique.setIdUtilisateur(idUtilisateur);
        historique.setAncienStatut(ancien);
        historique.setNouveauStatut(nouveau);
        historique.setMotif(motif);
        historiques.save(historique);
    }
}
