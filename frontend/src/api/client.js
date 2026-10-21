const BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080/api'
const TOKEN_KEY = 'erp.token'

export class ErreurApi extends Error {
  constructor(message, details = {}) {
    super(message)
    this.name = 'ErreurApi'
    Object.assign(this, details)
  }
}

let token = localStorage.getItem(TOKEN_KEY)
let onUnauthorized = null

export function definirJeton(value) {
  token = value
  if (value) {
    localStorage.setItem(TOKEN_KEY, value)
  } else {
    localStorage.removeItem(TOKEN_KEY)
  }
}

export function obtenirJeton() {
  return token
}

export function surJetonExpire(callback) {
  onUnauthorized = callback
}

async function request(method, path, body) {
  const headers = { Accept: 'application/json' }
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }
  if (token) {
    headers.Authorization = 'Bearer ' + token
  }

  let response
  try {
    response = await fetch(`${BASE}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  } catch {
    throw new ErreurApi('Le serveur est injoignable. Verifiez votre connexion.', { status: 0 })
  }

  if (response.status === 204) {
    return null
  }

  const text = await response.text()
  let data = null
  if (text) {
    try {
      data = JSON.parse(text)
    } catch {
      data = text
    }
  }

  if (!response.ok) {
    if (response.status === 401 && onUnauthorized) {
      onUnauthorized()
    }
    throw new ErreurApi(
      (data && typeof data === 'object' && (data.message || data.error)) ||
        `Erreur ${response.status} lors de l'appel ${path}.`,
      { status: response.status, path },
    )
  }

  return data
}

export const api = {
  get: (path) => request('GET', path),
  post: (path, body) => request('POST', path, body ?? {}),
  put: (path, body) => request('PUT', path, body ?? {}),
  patch: (path, body) => request('PATCH', path, body ?? {}),
  delete: (path) => request('DELETE', path),
}
