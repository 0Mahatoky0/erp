import { api, ErreurApi } from '@/shared/api/axios'

export const tempsApi = {
  listerPlannings: (idEmploye) => api.get(`/temps/employes/${idEmploye}/plannings`),
  creerPlanning: (demande) => api.post('/temps/plannings', demande),
  pointerEntree: (demande) => api.post('/temps/pointages/entree', demande),
  pointerSortie: (demande) => api.post('/temps/pointages/sortie', demande),
  consulterMois: (idEmploye, mois) => api.get(`/temps/employes/${idEmploye}/pointages?mois=${mois}`),
  calculerJour: (idEmploye, date) => api.post(`/temps/employes/${idEmploye}/jours?date=${date}`),
  validerJour: (idEmploye, date) => api.post(`/temps/employes/${idEmploye}/jours/${date}/valider`),
  traiterAnomalie: (idAnomalie, demande) => api.post(`/temps/anomalies/${idAnomalie}/traitement`, demande),
  calculerSemaine: (idEmploye, semaine) => api.post(`/temps/employes/${idEmploye}/heures-supplementaires/calcul?semaine=${semaine}`),
  heuresSupplementaires: (idEmploye, semaine) => api.get(`/temps/employes/${idEmploye}/heures-supplementaires?semaine=${semaine}`),
  periodesCloturees: () => api.get('/temps/periodes-cloturees'),
  cloturerPeriode: (annee, mois) => api.post('/temps/periodes-cloturees', { annee, mois }),
}

export { ErreurApi }
