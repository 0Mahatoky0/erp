package com.tovo.erp.embauche.service;

import com.tovo.erp.iam.model.RoleNom;
import com.tovo.erp.iam.model.Utilisateur;
import com.tovo.erp.embauche.dto.EmbaucheRequests;
import com.tovo.erp.embauche.dto.*;
import com.tovo.erp.embauche.model.*;
import com.tovo.erp.embauche.repository.*;
import com.tovo.erp.recrutement.dto.CandidatDto;
import com.tovo.erp.recrutement.service.CandidatureService;
import com.tovo.erp.employe.model.Employe;
import com.tovo.erp.employe.service.EmployeService;
import com.tovo.erp.shared.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ContratEmbaucheService {
    private final ContratEmbaucheRepository contrats;
    private final PropositionEmbaucheRepository propositions;
    private final CandidatureService candidatures;
    private final EmployeService employes;

    public ContratEmbaucheService(ContratEmbaucheRepository contrats,
                                  PropositionEmbaucheRepository propositions,
                                  CandidatureService candidatures,
                                  EmployeService employes) {
        this.contrats = contrats;
        this.propositions = propositions;
        this.candidatures = candidatures;
        this.employes = employes;
    }

    @Transactional
    public ContratEmbaucheDto creer(Long idProposition, Utilisateur utilisateur) {
        exigerRhOuDirection(utilisateur);
        PropositionEmbauche proposition = propositions.findById(idProposition)
                .orElseThrow(() -> BusinessException.notFound("Proposition introuvable."));
        if (proposition.getStatut() != PropositionStatut.ACCEPTEE) {
            throw new BusinessException("PROPOSITION_NON_ACCEPTEE",
                    "Un contrat ne peut être proposé qu'après l'acceptation de la proposition.");
        }
        if (contrats.existsByPropositionIdPropositionEmbauche(idProposition)) {
            throw new BusinessException("CONTRAT_EXISTANT", "Un contrat existe déjà pour cette proposition.");
        }
        ContratEmbauche contrat = new ContratEmbauche();
        contrat.setProposition(proposition);
        contrat.setIdCandidat(proposition.getIdCandidat());
        contrat.setTypeContrat(proposition.getTypeContrat());
        contrat.setSalaire(proposition.getSalairePropose());
        contrat.setLieuTravail(proposition.getLieuTravail());
        contrat.setDateDebut(proposition.getDateDebut());
        contrat.setDateFin(proposition.getDateFin());
        return ContratEmbaucheDto.de(contrats.save(contrat));
    }

    @Transactional(readOnly = true)
    public List<ContratEmbaucheDto> lister(Utilisateur utilisateur) {
        List<ContratEmbauche> resultat;
        if (estCandidat(utilisateur)) {
            Long idCandidat = candidatures.candidatDuCompte(utilisateur.getId()).idCandidat();
            resultat = contrats.findByIdCandidatOrderByDateCreationDesc(idCandidat);
        } else {
            resultat = contrats.findAllByOrderByDateCreationDesc();
        }
        return resultat.stream().map(ContratEmbaucheDto::de).toList();
    }

    @Transactional(readOnly = true)
    public ContratEmbaucheDto charger(Long id, Utilisateur utilisateur) {
        ContratEmbauche contrat = chargerEntite(id);
        verifierAcces(contrat, utilisateur);
        return ContratEmbaucheDto.de(contrat);
    }

    @Transactional
    public ContratEmbaucheDto repondre(Long id, EmbaucheRequests.ReponseContratRequest request,
                                       Utilisateur utilisateur) {
        ContratEmbauche contrat = chargerEntite(id);
        verifierAcces(contrat, utilisateur);
        if (!estCandidat(utilisateur)) {
            throw BusinessException.forbidden("Seul le candidat peut répondre à ce contrat.");
        }
        if (contrat.getStatut() != ContratStatut.PROPOSE) {
            throw new BusinessException("CONTRAT_TERMINE", "Ce contrat a déjà reçu une décision.");
        }
        contrat.setStatut(request.decision() == DecisionContrat.ACCEPTE
                ? ContratStatut.ACCEPTE : ContratStatut.REFUSE);
        contrat.setDateReponse(LocalDateTime.now());
        return ContratEmbaucheDto.de(contrats.save(contrat));
    }

    @Transactional
    public ContratEmbaucheDto valider(Long id, Utilisateur utilisateur) {
        exigerRhOuDirection(utilisateur);
        ContratEmbauche contrat = chargerEntite(id);
        if (contrat.getStatut() != ContratStatut.ACCEPTE) {
            throw new BusinessException("CONTRAT_NON_ACCEPTE",
                    "Seul un contrat accepté par le candidat peut être validé.");
        }
        CandidatDto candidat = candidatures.trouverDto(contrat.getProposition().getIdCandidature()).candidat();
        Employe employe = employes.creerDepuisCandidat(candidat, contrat.getDateDebut().toLocalDate());
        contrat.setIdEmploye(employe.getId());
        contrat.setStatut(ContratStatut.VALIDE);
        contrat.setDateValidation(LocalDateTime.now());
        return ContratEmbaucheDto.de(contrats.save(contrat));
    }

    private ContratEmbauche chargerEntite(Long id) {
        return contrats.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Contrat introuvable."));
    }

    private void verifierAcces(ContratEmbauche contrat, Utilisateur utilisateur) {
        if (estCandidat(utilisateur)
                && !candidatures.candidatDuCompte(utilisateur.getId()).idCandidat()
                .equals(contrat.getIdCandidat())) {
            throw BusinessException.forbidden("Ce contrat ne vous est pas destiné.");
        }
    }

    private void exigerRhOuDirection(Utilisateur utilisateur) {
        String role = utilisateur.getRole().getNom();
        if (!RoleNom.RH.name().equals(role) && !RoleNom.DIRECTION.name().equals(role)) {
            throw BusinessException.forbidden("Seul le RH ou la direction peut effectuer cette action.");
        }
    }

    private boolean estCandidat(Utilisateur utilisateur) {
        return RoleNom.CANDIDAT.name().equals(utilisateur.getRole().getNom());
    }
}