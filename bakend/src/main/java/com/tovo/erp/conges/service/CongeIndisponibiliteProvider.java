package com.tovo.erp.conges.service;

import com.tovo.erp.temps.service.IndisponibiliteProvider;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/** Expose les conges valides au module {@code temps} sans inverser les dependances. */
@Service
public class CongeIndisponibiliteProvider implements IndisponibiliteProvider {

    private final CongeService conges;

    public CongeIndisponibiliteProvider(CongeService conges) {
        this.conges = conges;
    }

    @Override
    public boolean estIndisponible(Long idEmploye, LocalDate date) {
        return conges.estEnCongeValide(idEmploye, date);
    }
}
