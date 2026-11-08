package com.tovo.erp.embauche.service;

import com.tovo.erp.iam.model.RoleNom;
import com.tovo.erp.iam.model.Utilisateur;
import com.tovo.erp.embauche.dto.EmbaucheRequests;
import com.tovo.erp.embauche.dto.*;
import com.tovo.erp.embauche.model.*;
import com.tovo.erp.embauche.repository.*;
import com.tovo.erp.recrutement.dto.CandidatDto;
import com.tovo.erp.recrutement.dto.CandidatureDto;
import com.tovo.erp.recrutement.model.CandidatureStatut;
import com.tovo.erp.recrutement.service.CandidatureService;
import com.tovo.erp.shared.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class PropositionEmbaucheService {
    private final PropositionEmbaucheRepository propositions;
    private final ReponsePropositionRepository reponses;
    private final CandidatureService candidatures;

    public PropositionEmbaucheService(PropositionEmbaucheRepository propositions,
                                      ReponsePropositionRepository reponses,
                                      CandidatureService candidatures) {
        this.propositions = propositions;
        this.reponses = reponses;
        this.candidatures = candidatures;
    }

    @Transactional
    public PropositionEmbaucheDto creer(EmbaucheRequests.PropositionRequest request) {
        CandidatureDto candidature = candidatures.trouverDto(request.idCandidature());
        if (candidature.statut() != CandidatureStatut.RETENUE) {
            throw new BusinessException("CANDIDATURE_NON_RETENUE",
                    "Une proposition ne peut être créée que pour une candidature retenue.");
        }
        if (propositions.existsByIdCandidature(candidature.idCandidature())) {
            throw new BusinessException("PROPOSITION_EXISTANTE",
                    "Une proposition existe déjà pour cette candidature.");
        }
        PropositionEmbauche proposition = new PropositionEmbauche();
        proposition.setIdCandidature(candidature.idCandidature());
        proposition.setIdCandidat(candidature.candidat().idCandidat());
        proposition.setIdOffre(candidature.offre().idOffre());
        appliquer(proposition, request);
        PropositionEmbauche saved = propositions.save(proposition);
        return convertir(saved, candidature);
    }

    @Transactional(readOnly = true)
    public List<PropositionEmbaucheDto> lister(Utilisateur utilisateur) {
        List<PropositionEmbauche> resultat;
        if (estCandidat(utilisateur)) {
            CandidatDto candidat = candidatures.candidatDuCompte(utilisateur.getId());
            resultat = propositions.findByIdCandidatOrderByDatePropositionDesc(candidat.idCandidat());
        } else {
            resultat = propositions.findAllByOrderByDatePropositionDesc();
        }
        Map<Long, CandidatureDto> candidaturesParId = candidatures.trouverDtos(
                resultat.stream().map(PropositionEmbauche::getIdCandidature).toList());
        return resultat.stream().map(proposition ->
                convertir(proposition, candidaturesParId.get(proposition.getIdCandidature()))).toList();
    }

    @Transactional(readOnly = true)
    public PropositionEmbaucheDto charger(Long id, Utilisateur utilisateur) {
        PropositionEmbauche proposition = chargerEntite(id);
        verifierAcces(proposition, utilisateur);
        return convertir(proposition, candidatures.trouverDto(proposition.getIdCandidature()));
    }

    @Transactional
    public ReponsePropositionDto repondre(Long id, EmbaucheRequests.ReponsePropositionRequest request,
                                          Utilisateur utilisateur) {
        PropositionEmbauche proposition = chargerEntite(id);
        verifierAcces(proposition, utilisateur);
        if (!estCandidat(utilisateur)) {
            throw BusinessException.forbidden("Seul le candidat peut répondre à cette proposition.");
        }
        if (proposition.getStatut() != PropositionStatut.ENVOYEE
                && proposition.getStatut() != PropositionStatut.AJUSTEMENT_DEMANDE) {
            throw new BusinessException("PROPOSITION_TERMINEE",
                    "Cette proposition a déjà reçu une décision finale.");
        }
        CandidatDto candidat = candidatures.candidatDuCompte(utilisateur.getId());
        ReponseProposition reponse = new ReponseProposition();
        reponse.setProposition(proposition);
        reponse.setIdCandidat(candidat.idCandidat());
        reponse.setDecision(request.decision());
        reponse.setSalaireSouhaite(request.salaireSouhaite());
        reponse.setLieuSouhaite(request.lieuSouhaite());
        reponse.setTypeContratSouhaite(request.typeContratSouhaite());
        reponse.setDateDebutSouhaitee(request.dateDebutSouhaitee());
        reponse.setDateFinSouhaitee(request.dateFinSouhaitee());
        reponse.setCommentaire(request.commentaire());
        proposition.setStatut(switch (request.decision()) {
            case ACCEPTEE -> PropositionStatut.ACCEPTEE;
            case REFUSEE -> PropositionStatut.REFUSEE;
            case AJUSTEMENT_DEMANDE -> PropositionStatut.AJUSTEMENT_DEMANDE;
        });
        propositions.save(proposition);
        return ReponsePropositionDto.de(reponses.save(reponse));
    }

    @Transactional(readOnly = true)
    public List<ReponsePropositionDto> reponses(Long id, Utilisateur utilisateur) {
        PropositionEmbauche proposition = chargerEntite(id);
        verifierAcces(proposition, utilisateur);
        return reponses.findByPropositionIdPropositionEmbaucheOrderByDateReponseDesc(id)
                .stream().map(ReponsePropositionDto::de).toList();
    }

    private PropositionEmbauche chargerEntite(Long id) {
        return propositions.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Proposition introuvable."));
    }

    private PropositionEmbaucheDto convertir(PropositionEmbauche proposition, CandidatureDto candidature) {
        return PropositionEmbaucheDto.de(proposition, candidature);
    }

    private void appliquer(PropositionEmbauche proposition, EmbaucheRequests.PropositionRequest request) {
        proposition.setTypeContrat(request.typeContrat().trim().toUpperCase());
        proposition.setSalairePropose(request.salairePropose());
        proposition.setLieuTravail(request.lieuTravail());
        proposition.setDateDebut(request.dateDebut());
        proposition.setDateFin(request.dateFin());
        if ("CDD".equals(proposition.getTypeContrat()) && request.dateFin() == null) {
            throw new BusinessException("DATE_FIN_REQUISE", "La date de fin est obligatoire pour un CDD.");
        }
        if (request.dateFin() != null && request.dateFin().isBefore(request.dateDebut())) {
            throw new BusinessException("DATES_INVALIDES",
                    "La date de fin doit être postérieure ou égale à la date de début.");
        }
    }

    private void verifierAcces(PropositionEmbauche proposition, Utilisateur utilisateur) {
        if (estCandidat(utilisateur)
                && !candidatures.candidatDuCompte(utilisateur.getId()).idCandidat()
                .equals(proposition.getIdCandidat())) {
            throw BusinessException.forbidden("Cette proposition ne vous est pas destinée.");
        }
    }

    private boolean estCandidat(Utilisateur utilisateur) {
        return RoleNom.CANDIDAT.name().equals(utilisateur.getRole().getNom());
    }
}