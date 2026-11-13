package com.tovo.erp.besoin.service;

/**
 * Port permetant au module Besoin de savoir si une ligne a donne lieu a une offre
 * publiee, sans dependre directement du module Recrutement (RG-B1-14).
 */
public interface OffrePublieeProvider {

    boolean existeOffrePubliee(Long idLigneDemande);
}
