import { api, ErreurApi } from '@/shared/api/axios'

const base = '/discipline'
const query = (params) => new URLSearchParams(
  Object.entries(params).filter(([, valeur]) => valeur !== null && valeur !== undefined && valeur !== ''),
).toString()

export const disciplineApi = {
  incidents: (motif) => api.get(`${base}/incidents?${query({ motif })}`),
  incident: (id, motif) => api.get(`${base}/incidents/${id}?${query({ motif })}`),
  declarerIncident: (donnees, motif) =>
    api.post(`${base}/incidents?${query({ motif })}`, donnees),
  qualifier: (id, donnees) => api.post(`${base}/incidents/${id}/qualifier`, donnees),

  procedures: (idEmploye) => api.get(`${base}/procedures?${query({ idEmploye })}`),
  procedure: (id, motif) => api.get(`${base}/procedures/${id}?${query({ motif })}`),
  etape: (id, type, donnees, motif) =>
    api.post(`${base}/procedures/${id}/${type}?${query({ motif })}`, donnees),
  action: (id, type, motif) =>
    api.post(`${base}/procedures/${id}/${type}?${query({ motif })}`),
  decision: (id, donnees, motif) =>
    api.post(`${base}/procedures/${id}/decision?${query({ motif })}`, donnees),
  notification: (id, donnees, motif) =>
    api.post(`${base}/procedures/${id}/notification?${query({ motif })}`, donnees),
  recours: (id, donnees, motif) =>
    api.post(`${base}/procedures/${id}/recours?${query({ motif })}`, donnees),

  sanctions: (idEmploye) => api.get(`${base}/sanctions?${query({ idEmploye })}`),
  absences: (idEmploye) => api.get(`${base}/absences?${query({ idEmploye })}`),
  variables: (debut, fin) => api.get(`${base}/sanctions/variables?${query({ debut, fin })}`),
  traiterAbsence: (id) => api.post(`${base}/absences/${id}/traiter`),

  piecesIncident: (id, motif) => api.get(`${base}/pieces/incident/${id}?${query({ motif })}`),
  piecesProcedure: (id, motif) => api.get(`${base}/pieces/procedure/${id}?${query({ motif })}`),
  ajouterPiece: (donnees, motif) =>
    api.post(`${base}/pieces?${query({ motif })}`, donnees),

  acces: (idProcedure) => api.get(`${base}/acces?${query({ idProcedure })}`),
  accorderAcces: (donnees) => api.post(`${base}/acces`, donnees),
  revoquerAcces: (id, motif) =>
    api.post(`${base}/acces/${id}/revoquer?${query({ motif })}`),
  journal: (motif) => api.get(`${base}/journal?${query({ motif })}`),
  journalRessource: (ressource, id, motif) =>
    api.get(`${base}/journal/${ressource}/${id}?${query({ motif })}`),
  indicateurs: (debut, fin) => api.get(`${base}/indicateurs?${query({ debut, fin })}`),

  employes: () => api.get('/referentiel/employes'),
}

export { ErreurApi }
