import { api, ErreurApi } from '@/shared/api/axios'

export const paieApi = {
  variables: (debut, fin) => api.get(`/paie/variables?debut=${debut}&fin=${fin}`),
  parametres: () => api.get('/paie/parametres'),
  enregistrerParametres: (parametres) => api.put('/paie/parametres', parametres),
  calculerPeriode: (mois) => api.post(`/paie/periodes/calcul?mois=${mois}-01`),
  periodes: () => api.get('/paie/periodes'),
  enregistrerRetenue: (idBulletin, montant) => api.put(`/paie/bulletins/${idBulletin}/retenue`, { montant }),
}

export { ErreurApi }
