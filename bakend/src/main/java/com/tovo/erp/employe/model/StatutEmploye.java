package com.tovo.erp.employe.model;

/** Situation administrative d'un employe (RM-057). */
public enum StatutEmploye {

    /** L'employe est en poste. */
    ACTIF,

    /** L'employe n'occupe plus de poste mais reste un employe de l'entreprise. */
    INACTIF,

    /** L'employe a quitté l'entreprise. */
    PARTI
}
