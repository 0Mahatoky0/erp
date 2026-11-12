package com.tovo.erp.temps.service;

import java.time.LocalDate;

/**
 * Point d'extension pour les indisponibilites d'un salarie (conges, absences).
 * <p>Le module {@code temps} ne connait pas {@code conges} : il interroge ce
 * contrat. L'implementation est fournie par le module {@code conges}, qui est
 * autorise a dependre de {@code temps} (ARCHITECTURE_BAKEND.md, section 5).
 */
public interface IndisponibiliteProvider {

    boolean estIndisponible(Long idEmploye, LocalDate date);
}
