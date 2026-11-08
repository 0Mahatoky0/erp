package com.tovo.erp.employe.dto;

import com.tovo.erp.employe.model.Employe;
import com.tovo.erp.employe.model.StatutEmploye;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Dossier salarie cree ou reactive par l'acceptation d'une proposition. */
public record EmployeEmbaucheDto(
        Long id,
        String matricule,
        String nom,
        String prenom,
        String email,
        String telephone,
        String cin,
        LocalDate dateNaissance,
        Long idPoste,
        String poste,
        Long idService,
        String service,
        Long idDepartement,
        String departement,
        StatutEmploye statut,
        LocalDate dateEmbauche,
        LocalDate datePremiereEmbauche,
        LocalDateTime dateCreation) {

    public static EmployeEmbaucheDto de(Employe employe) {
        return new EmployeEmbaucheDto(
                employe.getId(),
                employe.getMatricule(),
                employe.getNom(),
                employe.getPrenom(),
                employe.getEmail(),
                employe.getTelephone(),
                employe.getCin(),
                employe.getDateNaissance(),
                employe.getPoste() == null ? null : employe.getPoste().getId(),
                employe.getPoste() == null ? null : employe.getPoste().getIntitule(),
                employe.getPoste() == null || employe.getPoste().getService() == null
                        ? null : employe.getPoste().getService().getId(),
                employe.getPoste() == null || employe.getPoste().getService() == null
                        ? null : employe.getPoste().getService().getNom(),
                employe.getPoste() == null || employe.getPoste().getService() == null
                        || employe.getPoste().getService().getDepartement() == null
                        ? null : employe.getPoste().getService().getDepartement().getId(),
                employe.getPoste() == null || employe.getPoste().getService() == null
                        || employe.getPoste().getService().getDepartement() == null
                        ? null : employe.getPoste().getService().getDepartement().getNom(),
                employe.getStatut(),
                employe.getDateEmbauche(),
                employe.getDatePremiereEmbauche(),
                employe.getDateCreation());
    }
}
