package com.tovo.erp.besoin.dto;

import com.tovo.erp.besoin.model.Departement;
import com.tovo.erp.besoin.model.Service;

/** Referentiels exposes aux autres modules et a l'interface. */
public final class ReferentielDto {

    private ReferentielDto() {
    }

    public record DepartementListe(Long id, String nom) {
        public static DepartementListe de(Departement departement) {
            return new DepartementListe(departement.getId(), departement.getNom());
        }
    }

    public record ServiceListe(Long id, String nom, Long idDepartement, String departement) {
        public static ServiceListe de(Service service) {
            return new ServiceListe(
                    service.getId(),
                    service.getNom(),
                    service.getDepartement() == null ? null : service.getDepartement().getId(),
                    service.getDepartement() == null ? null : service.getDepartement().getNom());
        }
    }
}
