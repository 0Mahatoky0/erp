package com.tovo.erp.common;

import java.util.Locale;

/**
 * Construction du motif utilise par les recherches de type "contient".
 *
 * <p>Le motif est prepare en Java : cela evite les conversions de type ambigues
 * rencontrees avec une concatenation SQL et permet d'echapper les jokers
 * {@code %} et {@code _} saisis par l'utilisateur.
 */
public final class Recherche {

    private Recherche() {
    }

    /** Retourne null si la recherche est vide, sinon le motif echappe et en minuscules. */
    public static String motif(String recherche) {
        if (recherche == null || recherche.isBlank()) {
            return null;
        }
        String terme = recherche.trim().toLowerCase(Locale.ROOT);
        String echappe = terme.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + echappe + "%";
    }
}
