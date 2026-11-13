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
  joursFeries: (annee) => api.get(annee ? `/temps/jours-feries?annee=${annee}` : '/temps/jours-feries'),
  creerJourFerie: (jour) => api.post('/temps/jours-feries', jour),
  supprimerJourFerie: (id) => api.delete(`/temps/jours-feries/${id}`),
  regimes: () => api.get('/temps/regimes'),
  regime: (id) => api.get(`/temps/regimes/${id}`),
  creerRegime: (regime) => api.post('/temps/regimes', regime),
  modifierRegime: (id, regime) => api.put(`/temps/regimes/${id}`, regime),
  supprimerRegime: (id) => api.delete(`/temps/regimes/${id}`),
  horairesRegime: (id) => api.get(`/temps/regimes/${id}/horaires`),
  ajouterHoraire: (id, horaire) => api.post(`/temps/regimes/${id}/horaires`, horaire),
  supprimerHoraire: (id) => api.delete(`/temps/horaires/${id}`),
  affectationsRegime: (idEmploye) => api.get(`/temps/employes/${idEmploye}/affectations-regime`),
  regimeCourant: (idEmploye, date) => api.get(date ? `/temps/employes/${idEmploye}/regime-courant?date=${date}` : `/temps/employes/${idEmploye}/regime-courant`),
  affecterRegime: (idEmploye, affectation) => api.post(`/temps/employes/${idEmploye}/affectations-regime`, affectation),
}

export { ErreurApi }
