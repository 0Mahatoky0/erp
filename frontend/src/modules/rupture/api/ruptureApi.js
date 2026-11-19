import { api, ErreurApi } from '@/shared/api/axios'

const base = '/rupture'

const query = (params) =>
  new URLSearchParams(
    Object.entries(params || {}).filter(
      ([, valeur]) => valeur !== null && valeur !== undefined && valeur !== '',
    ),
  ).toString()

export function listDossiers(params) {
  const criteres = query(params)
  return api.get(`${base}/dossiers${criteres ? `?${criteres}` : ''}`)
}

export function getDossier(id) {
  return api.get(`${base}/dossiers/${id}`)
}

export function creerDossier(payload) {
  return api.post(`${base}/dossiers`, payload)
}

export function validerDossier(id) {
  return api.post(`${base}/dossiers/${id}/valider`)
}

export function notifierDossier(id) {
  return api.post(`${base}/dossiers/${id}/notifier`)
}

export function modifierDateSortie(id, payload) {
  return api.patch(`${base}/dossiers/${id}/date-sortie`, payload)
}

export function arreterDroits(id) {
  return api.post(`${base}/dossiers/${id}/arreter-droits`)
}

export function preparerSolde(id) {
  return api.post(`${base}/dossiers/${id}/solde/preparer`)
}

export function validerSolde(id) {
  return api.post(`${base}/dossiers/${id}/solde/valider`)
}

export function payerSolde(id, payload) {
  return api.post(`${base}/dossiers/${id}/solde/payer`, payload)
}

export function genererDocuments(id) {
  return api.post(`${base}/dossiers/${id}/documents/generer`)
}

export function remettreDocument(idDoc, payload) {
  return api.post(`${base}/documents/${idDoc}/remettre`, payload)
}

export function gererTache(idDossier, idTache, payload) {
  return api.patch(`${base}/dossiers/${idDossier}/taches/${idTache}`, payload)
}

export function enregistrerEntretien(id, payload) {
  return api.post(`${base}/dossiers/${id}/entretien`, payload)
}

export function cloturerDossier(id) {
  return api.post(`${base}/dossiers/${id}/cloturer`)
}

export function archiverDossier(id) {
  return api.post(`${base}/dossiers/${id}/archiver`)
}

export function indicateurs(params) {
  const criteres = query(params)
  return api.get(`${base}/indicateurs${criteres ? `?${criteres}` : ''}`)
}

export function types() {
  return api.get(`${base}/types`)
}

export const ruptureApi = {
  listDossiers,
  getDossier,
  creerDossier,
  validerDossier,
  notifierDossier,
  modifierDateSortie,
  arreterDroits,
  preparerSolde,
  validerSolde,
  payerSolde,
  genererDocuments,
  remettreDocument,
  gererTache,
  enregistrerEntretien,
  cloturerDossier,
  archiverDossier,
  indicateurs,
  types,
}

export { ErreurApi }
