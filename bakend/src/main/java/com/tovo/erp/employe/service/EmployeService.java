package com.tovo.erp.employe.service;

import com.tovo.erp.shared.Recherche;
import com.tovo.erp.employe.model.StatutEmploye;
import com.tovo.erp.employe.model.Employe;
import com.tovo.erp.employe.repository.EmployeRepository;
import com.tovo.erp.iam.dto.EmployeDto;
import com.tovo.erp.recrutement.dto.CandidatDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.time.LocalDate;

/**
 * Consultation des employes (RM-057 a RM-060).
 *
 * <p>Un employe n'est jamais supprime : son historique reste rattache a la demande.
 */
@Service
public class EmployeService {

    private final EmployeRepository employeRepository;

    public EmployeService(EmployeRepository employeRepository) {
        this.employeRepository = employeRepository;
    }

    /** Liste des employes filtres par statut, service et recherche textuelle. */
    @Transactional(readOnly = true)
    public List<EmployeDto.Liste> lister(StatutEmploye statut, Long idService, String recherche) {
        return employeRepository.rechercher(statut, idService, Recherche.motif(recherche)).stream()
                .map(EmployeDto.Liste::de)
                .toList();
    }

    @Transactional
    public Employe creerDepuisCandidat(CandidatDto candidat, LocalDate dateEmbauche) {
        if (employeRepository.existsByEmailIgnoreCase(candidat.email())) {
            throw new IllegalStateException("Un employe existe déjà pour cette adresse e-mail.");
        }
        Employe employe = new Employe();
        employe.setMatricule(genererMatricule());
        employe.setNom(candidat.nom());
        employe.setPrenom(candidat.prenom());
        employe.setEmail(candidat.email());
        employe.setTelephone(candidat.telephone());
        employe.setDateEmbauche(dateEmbauche);
        employe.setStatut(StatutEmploye.ACTIF);
        return employeRepository.save(employe);
    }

    private String genererMatricule() {
        String matricule;
        do {
            matricule = "EMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (employeRepository.existsByMatricule(matricule));
        return matricule;
    }
}
