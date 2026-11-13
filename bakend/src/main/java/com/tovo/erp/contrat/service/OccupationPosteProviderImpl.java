package com.tovo.erp.contrat.service;

import com.tovo.erp.besoin.service.OccupationPosteProvider;
import com.tovo.erp.contrat.model.AffectationStatut;
import com.tovo.erp.contrat.repository.AffectationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/** Implementation du port d'occupation des postes pour le module Besoin. */
@Service
public class OccupationPosteProviderImpl implements OccupationPosteProvider {

    private final AffectationRepository affectations;

    public OccupationPosteProviderImpl(AffectationRepository affectations) {
        this.affectations = affectations;
    }

    @Override
    public long affectationsActives(Long idPoste, LocalDate debut, LocalDate fin) {
        if (idPoste == null) {
            return 0;
        }
        return affectations.compterAffectationsSurPeriode(idPoste, debut, fin,
                List.of(AffectationStatut.PLANIFIEE, AffectationStatut.ACTIVE));
    }
}
