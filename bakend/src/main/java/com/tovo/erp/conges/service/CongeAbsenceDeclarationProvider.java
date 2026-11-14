package com.tovo.erp.conges.service;

import com.tovo.erp.temps.service.AbsenceDeclarationProvider;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/** Permet au module {@code temps} de qualifier une absence sans cycle de dependances. */
@Service
public class CongeAbsenceDeclarationProvider implements AbsenceDeclarationProvider {

    private final CongeService conges;

    public CongeAbsenceDeclarationProvider(CongeService conges) {
        this.conges = conges;
    }

    @Override
    public void declarerAbsenceNonJustifiee(Long idEmploye, LocalDate date) {
        conges.declarerAbsenceNonJustifiee(idEmploye, date);
    }
}
