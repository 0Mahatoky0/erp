/**
 * Utilitaires de formatage partages par les ecrans.
 */

/** Formate un montant en ariary (Madagascar). */
export function formatMontant(montant) {
  if (montant === null || montant === undefined) {
    return '—'
  }
  const nombre = Number(montant)
  if (Number.isNaN(nombre)) {
    return '—'
  }
  return `${nombre.toLocaleString('fr-FR', { maximumFractionDigits: 0 })} Ar`
}

/** Formate un montant sans la devise (utilise dans les tableaux denses). */
export function formatNombre(montant) {
  if (montant === null || montant === undefined) {
    return '—'
  }
  return Number(montant).toLocaleString('fr-FR', { maximumFractionDigits: 0 })
}

/** Formate une date ISO en date lisible. */
export function formatDate(valeur) {
  if (!valeur) {
    return '—'
  }
  const date = new Date(valeur)
  if (Number.isNaN(date.getTime())) {
    return '—'
  }
  return date.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric' })
}

/** Formate une date et l'heure. */
export function formatDateHeure(valeur) {
  if (!valeur) {
    return '—'
  }
  const date = new Date(valeur)
  if (Number.isNaN(date.getTime())) {
    return '—'
  }
  return date.toLocaleString('fr-FR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

/** Libelle lisible d'un statut, d'une etape ou d'une decision. */
export function libelle(valeur) {
  if (!valeur) {
    return '—'
  }
  const texte = String(valeur).replace(/_/g, ' ').toLowerCase()
  return texte.charAt(0).toUpperCase() + texte.slice(1)
}

/** Construit un libelle a partir d'un dictionnaire de traductions. */
export function libelles(constantes) {
  return (valeur) => constantes[valeur] || libelle(valeur)
}
