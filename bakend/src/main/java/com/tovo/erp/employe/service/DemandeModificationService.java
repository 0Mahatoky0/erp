package com.tovo.erp.employe.service;

import com.tovo.erp.employe.dto.DemandeModificationDtos.CreationDemandeRequest;
import com.tovo.erp.employe.dto.DemandeModificationDtos.DemandeModificationDto;
import com.tovo.erp.employe.model.DemandeModification;
import com.tovo.erp.employe.model.Employe;
import com.tovo.erp.employe.model.StatutDemandeModification;
import com.tovo.erp.employe.repository.DemandeModificationRepository;
import com.tovo.erp.employe.repository.EmployeRepository;
import com.tovo.erp.shared.BusinessException;
import com.tovo.erp.shared.audit.JournalAuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Demandes de correction des donnees du dossier salarie. Une demande n'est
 * appliquee qu'apres validation RH ; chaque application est auditee.
 */
@Service
public class DemandeModificationService {

    private static final Set<String> CHAMPS_AUTORISES = Set.of(
            "nom", "prenom", "email", "telephone", "adresse", "nationalite",
            "situationFamiliale", "lieuNaissance");

    private final DemandeModificationRepository demandes;
    private final EmployeRepository employes;
    private final JournalAuditService audit;

    public DemandeModificationService(DemandeModificationRepository demandes,
                                      EmployeRepository employes,
                                      JournalAuditService audit) {
        this.demandes = demandes;
        this.employes = employes;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<DemandeModificationDto> lister(Long idEmploye) {
        exigerEmploye(idEmploye);
        return demandes.findByIdEmployeOrderByDateCreationDesc(idEmploye).stream()
                .map(DemandeModificationDto::de)
                .toList();
    }

    @Transactional
    public DemandeModificationDto creer(Long idEmploye, CreationDemandeRequest requete,
                                        Long idDemandeur) {
        Employe employe = exigerEmploye(idEmploye);
        String champ = normaliserChamp(requete.champ());
        if (!CHAMPS_AUTORISES.contains(champ)) {
            throw new BusinessException("CHAMP_NON_MODIFIABLE",
                    "Le champ « " + requete.champ() + " » ne peut pas etre modifie par demande.");
        }
        DemandeModification demande = new DemandeModification();
        demande.setIdEmploye(idEmploye);
        demande.setChamp(champ);
        demande.setAncienneValeur(lireChamp(employe, champ));
        demande.setNouvelleValeur(requete.nouvelleValeur().trim());
        demande.setMotif(requete.motif());
        demande.setIdDemandeur(idDemandeur);
        return DemandeModificationDto.de(demandes.save(demande));
    }

    @Transactional
    public DemandeModificationDto decider(Long idDemande, boolean approuver,
                                          String motifDecision, Long idValidateur) {
        DemandeModification demande = demandes.findById(idDemande)
                .orElseThrow(() -> BusinessException.notFound(
                        "La demande de modification " + idDemande + " n'existe pas."));
        if (demande.getStatut() != StatutDemandeModification.EN_ATTENTE) {
            throw new BusinessException("DEMANDE_DEJA_TRAITEE",
                    "Cette demande a deja ete traitee.");
        }
        Employe employe = exigerEmploye(demande.getIdEmploye());
        String avant = lireChamp(employe, demande.getChamp());
        if (approuver) {
            ecrireChamp(employe, demande.getChamp(), demande.getNouvelleValeur());
            employes.save(employe);
            demande.setStatut(StatutDemandeModification.APPROUVEE);
        } else {
            demande.setStatut(StatutDemandeModification.REFUSEE);
        }
        demande.setIdValidateur(idValidateur);
        demande.setMotifDecision(motifDecision);
        demande.setDateDecision(LocalDateTime.now());
        DemandeModification enregistree = demandes.save(demande);
        audit.enregistrer(approuver ? "MODIFICATION_DOSSIER" : "REFUS_MODIFICATION",
                "employe", employe.getId(), avant, lireChamp(employe, demande.getChamp()));
        return DemandeModificationDto.de(enregistree);
    }

    private String normaliserChamp(String champ) {
        return champ == null ? "" : champ.trim();
    }

    private String lireChamp(Employe employe, String champ) {
        return switch (champ) {
            case "nom" -> employe.getNom();
            case "prenom" -> employe.getPrenom();
            case "email" -> employe.getEmail();
            case "telephone" -> employe.getTelephone();
            case "adresse" -> employe.getAdresse();
            case "nationalite" -> employe.getNationalite();
            case "situationFamiliale" -> employe.getSituationFamiliale();
            case "lieuNaissance" -> employe.getLieuNaissance();
            default -> null;
        };
    }

    private void ecrireChamp(Employe employe, String champ, String valeur) {
        String nettoyee = valeur == null ? null : valeur.trim();
        switch (champ) {
            case "nom" -> employe.setNom(nettoyee);
            case "prenom" -> employe.setPrenom(nettoyee);
            case "email" -> employe.setEmail(nettoyee == null ? null : nettoyee.toLowerCase(Locale.ROOT));
            case "telephone" -> employe.setTelephone(nettoyee);
            case "adresse" -> employe.setAdresse(nettoyee);
            case "nationalite" -> employe.setNationalite(nettoyee);
            case "situationFamiliale" -> employe.setSituationFamiliale(nettoyee);
            case "lieuNaissance" -> employe.setLieuNaissance(nettoyee);
            default -> throw new BusinessException("CHAMP_NON_MODIFIABLE",
                    "Le champ « " + champ + " » ne peut pas etre modifie.");
        }
    }

    private Employe exigerEmploye(Long idEmploye) {
        return employes.findById(idEmploye)
                .orElseThrow(() -> BusinessException.notFound(
                        "L'employe " + idEmploye + " n'existe pas."));
    }
}
