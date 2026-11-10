import { api, ErreurApi } from '@/shared/api/axios'

export const tempsApi = {
  listerPlannings: (idEmploye) => api.get(`/temps/employes/${idEmploye}/plannings`),
  creerPlanning: (demande) => api.post('/temps/plannings', demande),
  pointerEntree: (demande) => api.post('/temps/pointages/entree', demande),
  pointerSortie: (demande) => api.post('/temps/pointages/sortie', demande),
  consulterMois: (idEmploye, mois) => api.get(`/temps/employes/${idEmploye}/pointages?mois=${mois}`),
}

export { ErreurApi }
