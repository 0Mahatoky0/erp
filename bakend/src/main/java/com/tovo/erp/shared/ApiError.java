package com.tovo.erp.common;

/**
 * Erreur renvoyee au client. Le format est stable : le frontend peut
 * afficher {@code message} tel quel et se servir de {@code code} pour le tri.
 */
public record ApiError(String code, String message, int status, String chemin) {
}
