import { api, ErreurApi } from '@/shared/api/axios'

export const congesApi = {
  types: () => api.get('/conges/types'),
  employeCourant: () => api.get('/conges/moi'),
  soldes: (idEmploye, annee) => api.get(`/conges/employes/${idEmploye}/soldes?annee=${annee}`),
  demandes: (statut) => api.get(statut ? `/conges/demandes?statut=${statut}` : '/conges/demandes'),
  demandesEmploye: (idEmploye) => api.get(`/conges/employes/${idEmploye}/demandes`),
  creerDemande: (demande) => api.post('/conges/demandes', demande),
  valider: (id, demande = {}) => api.post(`/conges/demandes/${id}/valider`, demande),
  refuser: (id, demande) => api.post(`/conges/demandes/${id}/refuser`, demande),
  absences: () => api.get('/conges/absences'),
  mesAbsences: () => api.get('/conges/moi/absences'),
  creerAbsence: (absence) => api.post('/conges/absences', absence),
  justifier: (id, demande) => api.post(`/conges/absences/${id}/justifier`, demande),
  planning: () => api.get('/conges/planning'),
  periodesBloquees: () => api.get('/conges/periodes-bloquees'),
  creerPeriodeBloquee: (periode) => api.post('/conges/periodes-bloquees', periode),
  supprimerPeriodeBloquee: (id) => api.delete(`/conges/periodes-bloquees/${id}`),
  matriceRemplacement: () => api.get('/conges/matrice-remplacement'),
  creerMatriceRemplacement: (matrice) => api.post('/conges/matrice-remplacement', matrice),
  proposerSuppleance: (id, demande = {}) => api.post(`/conges/demandes/${id}/suppleance`, demande),
  suppleancesDemande: (id) => api.get(`/conges/demandes/${id}/suppleances`),
  repondreSuppleance: (id, demande) => api.post(`/conges/suppleances/${id}/repondre`, demande),
  terminerSuppleance: (id) => api.post(`/conges/suppleances/${id}/terminer`),
  variablesPaie: () => api.get('/conges/variables-paie'),
}

export { ErreurApi }
