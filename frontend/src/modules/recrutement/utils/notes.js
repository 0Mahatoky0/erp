/**
 * Libellés et formatage des évaluations de candidats (dossier, entretien, test).
 */

export const TYPES_EVALUATION = [
  { valeur: 'DOSSIER', libelle: 'Dossier' },
  { valeur: 'ENTRETIEN', libelle: 'Entretien' },
  { valeur: 'TEST', libelle: 'Test' },
]

/** Valeurs acceptées par la base (contrainte chk_b2_evaluation_appreciation). */
export const APPRECIATIONS = [
  { valeur: 'FAVORABLE', libelle: 'Favorable', severite: 'success' },
  { valeur: 'RESERVE', libelle: 'Réservée', severite: 'warn' },
  { valeur: 'DEFAVORABLE', libelle: 'Défavorable', severite: 'danger' },
]

export function libelleTypeEvaluation(valeur) {
  return TYPES_EVALUATION.find((type) => type.valeur === valeur)?.libelle || valeur || '—'
}

export function appreciation(valeur) {
  return (
    APPRECIATIONS.find((item) => item.valeur === valeur) || {
      valeur,
      libelle: valeur || '—',
      severite: 'secondary',
    }
  )
}

/** Formate une note en français, sans décimales inutiles (14 ou 14,5). */
export function formatNote(note) {
  if (note === null || note === undefined || Number.isNaN(Number(note))) {
    return '—'
  }
  return Number(note).toLocaleString('fr-FR', { minimumFractionDigits: 0, maximumFractionDigits: 2 })
}
