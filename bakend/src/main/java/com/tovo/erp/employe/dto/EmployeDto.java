package com.tovo.erp.employe.dto;

import com.tovo.erp.employe.model.Employe;
import com.tovo.erp.employe.model.PosteDetailVue;

import java.time.LocalDate;

/** Objets de transfert des employes (RM-057 a RM-060). */
public final class EmployeDto {

    private EmployeDto() {
    }

    /** Fiche employe, utilisable pour selectionner la personne remplacee (RM-025). */
    public record Liste(Long id, String matricule, String nom, String prenom, String email,
                        String telephone, LocalDate dateEmbauche,
                        Long idPoste, String poste, Long idService, String service,
                        String statut) {

        public static Liste de(Employe employe, PosteDetailVue poste) {
            return new Liste(
                    employe.getId(),
                    employe.getMatricule(),
                    employe.getNom(),
                    employe.getPrenom(),
                    employe.getEmail(),
                    employe.getTelephone(),
                    employe.getDateEmbauche(),
                    employe.getIdPoste(),
                    poste == null ? null : poste.getIntitule(),
                    poste == null ? null : poste.getIdService(),
                    poste == null ? null : poste.getServiceNom(),
                    employe.getStatut().name());
        }
    }

    /** Reference minimale d'un salarie actif, pour les modules qui calculent des variables. */
    public record Actif(Long id, String nom, String prenom, Long idService, LocalDate dateEmbauche) {

        public static Actif de(Employe employe) {
            return new Actif(employe.getId(), employe.getNom(), employe.getPrenom(), null,
                    employe.getDateEmbauche());
        }

        public static Actif de(Employe employe, PosteDetailVue poste) {
            return new Actif(employe.getId(), employe.getNom(), employe.getPrenom(),
                    poste == null ? null : poste.getIdService(), employe.getDateEmbauche());
        }
    }
}
