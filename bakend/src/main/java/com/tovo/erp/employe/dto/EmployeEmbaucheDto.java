package com.tovo.erp.employe.dto;

import com.tovo.erp.employe.model.Employe;
import com.tovo.erp.employe.model.PosteDetailVue;

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
        String statut,
        LocalDate dateEmbauche,
        LocalDate datePremiereEmbauche,
        LocalDateTime dateCreation) {

    public static EmployeEmbaucheDto de(Employe employe, PosteDetailVue poste) {
        return new EmployeEmbaucheDto(
                employe.getId(),
                employe.getMatricule(),
                employe.getNom(),
                employe.getPrenom(),
                employe.getEmail(),
                employe.getTelephone(),
                employe.getCin(),
                employe.getDateNaissance(),
                employe.getIdPoste(),
                poste == null ? null : poste.getIntitule(),
                poste == null ? null : poste.getIdService(),
                poste == null ? null : poste.getServiceNom(),
                poste == null ? null : poste.getIdDepartement(),
                poste == null ? null : poste.getDepartementNom(),
                employe.getStatut().name(),
                employe.getDateEmbauche(),
                employe.getDatePremiereEmbauche(),
                employe.getDateCreation());
    }
}
