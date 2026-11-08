package com.tovo.erp.employe.dto;

/** Contenu binaire d'une piece du dossier, pour le telechargement. */
public record DocumentEmbaucheFichier(String nomFichier, String typeMime, byte[] contenu) {
}
