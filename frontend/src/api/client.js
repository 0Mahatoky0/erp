/**
 * Client HTTP de l'API ERP.
 *
 * <p>Chaque appel ajoute le jeton JWT de l'utilisateur connecte et traduit les
 * erreurs techniques en exception `ErreurApi` : le message renvoye par le serveur
 * est toujours explicite (RM-068).
 */

const BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080/api'

/** Erreur metier renvoyee par l'API. */
export class ErreurApi extends Error {
  constructor(message, { code, statut, chemin } = {}) {
    super(message)
    this.name = 'ErreurApi'
    this.code = code
    this.statut = statut
    this.chemin = chemin
  }
}

let jeton = null
let surExpiration = null

/** Definit le jeton utilise par les appels suivants. */
export function definirJeton(valeur) {
  jeton = valeur
}

/** Jeton courant. */
export function obtenirJeton() {
  return jeton
}

/** Callback appele lorsque le serveur signale un jeton expire ou invalide. */
export function surJetonExpire(callback) {
  surExpiration = callback
}

async function requete(methode, chemin, corps) {
  const entetes = { Accept: 'application/json' }
  if (corps !== undefined) {
    entetes['Content-Type'] = 'application/json'
  }
  if (jeton) {
    entetes.Authorization = `Bearer ${jeton}`
  }

  const options = { method: methode, headers: entetes }
  if (methode !== 'GET' && methode !== 'DELETE' && corps !== undefined) {
    options.body = JSON.stringify(corps)
  }

  let reponse
  try {
    reponse = await fetch(`${BASE}${chemin}`, options)
  } catch {
    throw new ErreurApi('Le serveur est injoignable. Verifiez votre connexion.', { statut: 0 })
  }

  if (reponse.status === 204) {
    return null
  }

  const texte = await reponse.text()
  let donnees = null
  if (texte) {
    try {
      donnees = JSON.parse(texte)
    } catch {
      donnees = texte
    }
  }

  if (!reponse.ok) {
    if (reponse.status === 401 && surExpiration) {
      surExpiration()
    }
    const message =
      (donnees && donnees.message) || `Erreur ${reponse.status} lors de l'appel ${chemin}.`
    throw new ErreurApi(message, {
      code: donnees && donnees.code,
      statut: reponse.status,
      chemin: donnees && donnees.chemin,
    })
  }

  return donnees
}

/** Point d'entree unique de l'API. */
export const api = {
  get: (chemin) => requete('GET', chemin),
  post: (chemin, corps) => requete('POST', chemin, corps ?? {}),
  put: (chemin, corps) => requete('PUT', chemin, corps ?? {}),
  delete: (chemin) => requete('DELETE', chemin),
}
