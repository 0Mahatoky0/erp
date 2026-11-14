package com.tovo.erp.besoin.service;

import java.time.LocalDate;

/**
 * Port permetant au module Besoin de connaitre l'occupation reelle d'un poste
 * sans dependre directement du module Contrat (RG-B1-06).
 */
public interface OccupationPosteProvider {

    /**
     * Nombre d'affectations actives ou planifiees sur un poste pour une periode donnee.
     */
    long affectationsActives(Long idPoste, LocalDate debut, LocalDate fin);
}
