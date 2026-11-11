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
  variablesPaie: () => api.get('/conges/variables-paie'),
}

export { ErreurApi }
