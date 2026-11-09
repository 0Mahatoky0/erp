package com.tovo.erp.embauche.service;

import com.tovo.erp.contrat.dto.ContratDtos;
import com.tovo.erp.contrat.service.ContratService;
import com.tovo.erp.employe.dto.EmployeEmbaucheDto;
import com.tovo.erp.employe.service.EmployeService;
import com.tovo.erp.recrutement.service.CandidatureService;
import com.tovo.erp.shared.BusinessException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orchestration de l'espace candidat : le module contrat ne connait ni les
 * candidats ni les dossiers salaries, il ne manipule que des identifiants.
 */
@Service
public class ContratCandidatService {

    private final ContratService contrats;
    private final CandidatureService candidatures;
    private final EmployeService employes;

    public ContratCandidatService(ContratService contrats,
                                  CandidatureService candidatures,
                                  EmployeService employes) {
        this.contrats = contrats;
        this.candidatures = candidatures;
        this.employes = employes;
    }

    public List<ContratDtos.ContratDto> mesContrats(Long idUtilisateur) {
        Long idCandidat = candidatures.idCandidatParUtilisateur(idUtilisateur);
        if (idCandidat == null) {
            return List.of();
        }
        return employes.parIdCandidat(idCandidat)
                .map(EmployeEmbaucheDto::id)
                .map(contrats::contratsDe)
                .orElseGet(List::of);
    }

    public ContratDtos.ContratDto monContrat(Long idContrat, Long idUtilisateur) {
        Long idEmploye = idEmployeDuCandidat(idUtilisateur);
        ContratDtos.ContratDto contrat = contrats.contrat(idContrat);
        if (!idEmploye.equals(contrat.idEmploye())) {
            throw BusinessException.forbidden(
                    "Ce contrat n'appartient pas au candidat connecte.");
        }
        return contrat;
    }

    public ContratDtos.ContratDto signer(Long idContrat, Long idUtilisateur,
                                         ContratDtos.SignatureSalarieRequest demande,
                                         String ip) {
        Long idEmploye = idEmployeDuCandidat(idUtilisateur);
        return contrats.signerSalarie(idContrat, idEmploye, demande, ip);
    }

    private Long idEmployeDuCandidat(Long idUtilisateur) {
        Long idCandidat = candidatures.idCandidatParUtilisateur(idUtilisateur);
        if (idCandidat == null) {
            throw BusinessException.forbidden("Aucun dossier candidat n'est rattache a ce compte.");
        }
        return employes.parIdCandidat(idCandidat)
                .map(EmployeEmbaucheDto::id)
                .orElseThrow(() -> BusinessException.forbidden(
                        "Aucun dossier salarie n'est rattache a ce compte."));
    }
}
