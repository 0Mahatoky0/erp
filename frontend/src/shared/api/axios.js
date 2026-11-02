import axios from 'axios'

const TOKEN_KEY = 'erp.token'

export class ErreurApi extends Error {
  constructor(message, { code, statut, chemin } = {}) {
    super(message)
    this.name = 'ErreurApi'
    this.code = code
    this.statut = statut
    this.chemin = chemin
  }
}

const client = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api',
  headers: {
    Accept: 'application/json',
  },
})

let surExpiration = null

client.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

client.interceptors.response.use(
  (response) => response,
  (error) => {
    const response = error.response
    if (!response) {
      return Promise.reject(
        new ErreurApi('Le serveur est injoignable. Verifiez votre connexion.', { statut: 0 }),
      )
    }

    if (response.status === 401 && surExpiration) {
      surExpiration()
    }

    const donnees = response.data
    const message =
      (donnees && typeof donnees === 'object' && (donnees.message || donnees.error)) ||
      `Erreur ${response.status} lors de l'appel ${response.config.url}.`

    return Promise.reject(
      new ErreurApi(message, {
        code: donnees && donnees.code,
        statut: response.status,
        chemin: donnees && donnees.chemin,
      }),
    )
  },
)

export function definirJeton(valeur) {
  if (valeur) {
    localStorage.setItem(TOKEN_KEY, valeur)
  } else {
    localStorage.removeItem(TOKEN_KEY)
  }
}

export function obtenirJeton() {
  return localStorage.getItem(TOKEN_KEY)
}

export function surJetonExpire(callback) {
  surExpiration = callback
}

async function requete(methode, chemin, corps) {
  const response = await client.request({
    method: methode,
    url: chemin,
    data: corps,
  })
  return response.status === 204 ? null : response.data
}

export const api = {
  get: (chemin) => requete('GET', chemin),
  post: (chemin, corps) => requete('POST', chemin, corps),
  put: (chemin, corps) => requete('PUT', chemin, corps),
  patch: (chemin, corps) => requete('PATCH', chemin, corps),
  delete: (chemin) => requete('DELETE', chemin),
}
