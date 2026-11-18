import { api, ErreurApi } from '@/shared/api/axios'

export const paieApi = {
  variables: (debut, fin) => api.get(`/paie/variables?debut=${debut}&fin=${fin}`),
  parametres: () => api.get('/paie/parametres'),
  enregistrerParametres: (parametres) => api.put('/paie/parametres', parametres),
  calculerPeriode: (mois) => api.post(`/paie/periodes/calcul?mois=${mois}-01`),
  periodes: () => api.get('/paie/periodes'),
  periode: (idPeriode) => api.get(`/paie/periodes/${idPeriode}`),
  consolider: (idPeriode) => api.post(`/paie/periodes/${idPeriode}/consolider`),
  valider: (idPeriode) => api.post(`/paie/periodes/${idPeriode}/valider`),
  changerStatut: (idPeriode, statut) => api.post(`/paie/periodes/${idPeriode}/statut?statut=${statut}`),
  cloturer: (idPeriode) => api.post(`/paie/periodes/${idPeriode}/cloturer`),
  detailBulletin: (idBulletin) => api.get(`/paie/bulletins/${idBulletin}`),
  lignesBulletin: (idBulletin) => api.get(`/paie/bulletins/${idBulletin}/lignes`),
  ajuster: (idBulletin, ajustement) => api.put(`/paie/bulletins/${idBulletin}/ajustement`, ajustement),
  enregistrerRetenue: (idBulletin, montant) => api.put(`/paie/bulletins/${idBulletin}/retenue`, { montant }),
  fichesPeriode: (idPeriode) => api.get(`/fiches-paie/periodes/${idPeriode}`),
  fichesEmploye: (idEmploye) => api.get(`/fiches-paie/employes/${idEmploye}`),
  mesFiches: () => api.get('/fiches-paie/miennes'),
}

export { ErreurApi }
