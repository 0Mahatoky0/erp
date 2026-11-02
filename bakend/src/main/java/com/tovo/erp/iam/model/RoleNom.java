package com.tovo.erp.iam.model;

/**
 * Roles applicatifs et responsabilites associees (RM-004).
 *
 * <p>Le role n'est jamais deductible de l'interface : le backend verifie le role
 * a chaque operation autorisee.
 */
public enum RoleNom {

    /** Cree et suit les demandes de recrutement de son perimetre. */
    MANAGER,

    /** Verifie la coherence du besoin et les informations du poste. */
    RH,

    /** Verifie la disponibilite du budget. */
    FINANCE,

    /** Prend la decision finale lorsque son approbation est requise. */
    DIRECTION
}
