package com.tovo.erp.temps.service;

import java.time.LocalDate;

/**
 * Point d'extension pour qualifier une absence constatee (RG-B6-16). Le module
 * {@code conges} fournit l'implementation afin de ne pas inverser les dependances.
 */
public interface AbsenceDeclarationProvider {

    void declarerAbsenceNonJustifiee(Long idEmploye, LocalDate date);
}
