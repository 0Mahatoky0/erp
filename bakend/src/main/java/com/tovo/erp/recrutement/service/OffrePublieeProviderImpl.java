package com.tovo.erp.recrutement.service;

import com.tovo.erp.besoin.service.OffrePublieeProvider;
import com.tovo.erp.recrutement.model.OffreStatut;
import com.tovo.erp.recrutement.repository.OffreEmploiRepository;
import org.springframework.stereotype.Service;

/** Implementation du port d'existence d'une offre publiee pour le module Besoin. */
@Service
public class OffrePublieeProviderImpl implements OffrePublieeProvider {

    private final OffreEmploiRepository offres;

    public OffrePublieeProviderImpl(OffreEmploiRepository offres) {
        this.offres = offres;
    }

    @Override
    public boolean existeOffrePubliee(Long idLigneDemande) {
        if (idLigneDemande == null) {
            return false;
        }
        return offres.findByIdDemandePoste(idLigneDemande).stream()
                .anyMatch(offre -> offre.getStatut() == OffreStatut.PUBLIEE);
    }
}
