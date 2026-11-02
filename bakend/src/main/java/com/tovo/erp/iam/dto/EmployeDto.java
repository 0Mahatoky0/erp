package com.tovo.erp.iam.dto;

import com.tovo.erp.employe.model.Employe;
import com.tovo.erp.employe.model.StatutEmploye;

import java.time.LocalDate;

/** Objets de transfert des employes (RM-057 a RM-060). */
public final class EmployeDto {

    private EmployeDto() {
    }

    /** Fiche employe, utilisable pour selectionner la personne remplacee (RM-025). */
    public record Liste(Long id, String matricule, String nom, String prenom, String email,
                        String telephone, LocalDate dateEmbauche,
                        Long idPoste, String poste, Long idService, String service,
                        StatutEmploye statut) {

        public static Liste de(Employe employe) {
            return new Liste(
                    employe.getId(),
                    employe.getMatricule(),
                    employe.getNom(),
                    employe.getPrenom(),
                    employe.getEmail(),
                    employe.getTelephone(),
                    employe.getDateEmbauche(),
                    employe.getPoste() == null ? null : employe.getPoste().getId(),
                    employe.getPoste() == null ? null : employe.getPoste().getIntitule(),
                    employe.getPoste() == null || employe.getPoste().getService() == null
                            ? null : employe.getPoste().getService().getId(),
                    employe.getPoste() == null || employe.getPoste().getService() == null
                            ? null : employe.getPoste().getService().getNom(),
                    employe.getStatut());
        }
    }
}
