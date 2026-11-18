import { api, ErreurApi } from '@/shared/api/axios'

const base = ''

export const carriereApi = {
  mouvements: (statut) =>
    api.get(`${base}/mouvements`, statut ? { params: { statut } } : undefined),
  creerMouvement: (donnees) => api.post(`${base}/mouvements`, donnees),
  mouvement: (id) => api.get(`${base}/mouvements/${id}`),
  soumettre: (id) => api.post(`${base}/mouvements/${id}/soumettre`),
  valider: (id, donnees) => api.post(`${base}/mouvements/${id}/valider`, donnees),
  appliquer: (id) => api.post(`${base}/mouvements/${id}/appliquer`),
  annuler: (id, motif) =>
    api.post(`${base}/mouvements/${id}/annuler?motif=${encodeURIComponent(motif)}`),

  carriere: (idEmploye, date, dateConnaissance) =>
    api.get(`${base}/employes/${idEmploye}/carriere`, {
      params: { date, connu_le: dateConnaissance },
    }),
  frise: (idEmploye) => api.get(`${base}/employes/${idEmploye}/frise`),
  integrite: () => api.get(`${base}/carriere/integrite`),

  campagnes: () => api.get(`${base}/campagnes-augmentation`),
  creerCampagne: (donnees) => api.post(`${base}/campagnes-augmentation`, donnees),
  simulerCampagne: (id) => api.post(`${base}/campagnes-augmentation/${id}/simuler`),

  employes: () => api.get('/referentiel/employes'),
}

export { ErreurApi }
