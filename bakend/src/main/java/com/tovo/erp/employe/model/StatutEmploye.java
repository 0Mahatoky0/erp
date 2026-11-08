package com.tovo.erp.employe.model;

/** Situation administrative d'un employe (RM-057, section 5.4 du cahier). */
public enum StatutEmploye {

    /** Dossier cree a l'acceptation, en attente de la signature du contrat (RG-B3-06). */
    PRE_EMBAUCHE,

    /** L'employe est en poste. */
    ACTIF,

    /** L'employe n'occupe plus de poste mais reste un employe de l'entreprise. */
    INACTIF,

    /** L'employe a quitte l'entreprise. */
    PARTI,

    /** Le candidat a accepte puis ne s'est pas presente a la prise de poste. */
    NON_PRIS_POSTE
}
