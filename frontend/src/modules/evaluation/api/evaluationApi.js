import { api, ErreurApi } from '@/shared/api/axios'

const base = '/evaluation'

export const evaluationApi = {
  campagnes: () => api.get(`${base}/campagnes`),
  creerCampagne: (donnees) => api.post(`${base}/campagnes`, donnees),
  changerStatutCampagne: (idCampagne, statut) =>
    api.post(`${base}/campagnes/${idCampagne}/statut?statut=${encodeURIComponent(statut)}`),

  objectifs: (idCampagne, idEmploye) =>
    api.get(`${base}/campagnes/${idCampagne}/objectifs?idEmploye=${idEmploye}`),
  creerObjectif: (donnees) => api.post(`${base}/objectifs`, donnees),
  avenantObjectif: (idObjectif, donnees) => api.post(`${base}/objectifs/${idObjectif}/avenant`, donnees),
  validerObjectifs: (idEmploye, idCampagne) =>
    api.post(`${base}/objectifs/validation?idEmploye=${idEmploye}&idCampagne=${idCampagne}`),

  evaluations: (idCampagne) => api.get(`${base}/evaluations?idCampagne=${idCampagne}`),
  mesEvaluations: () => api.get(`${base}/mes-evaluations`),
  evaluation: (idEvaluation) => api.get(`${base}/evaluations/${idEvaluation}`),
  demarrer: (idCampagne, idEmploye) =>
    api.post(`${base}/evaluations/demarrer?idCampagne=${idCampagne}&idEmploye=${idEmploye}`),
  autoEvaluer: (idEvaluation, donnees) => api.post(`${base}/evaluations/${idEvaluation}/auto`, donnees),
  evaluerManager: (idEvaluation, donnees) => api.post(`${base}/evaluations/${idEvaluation}/manager`, donnees),
  entretien: (idEvaluation, donnees) => api.post(`${base}/evaluations/${idEvaluation}/entretien`, donnees),
  validerN2: (idEvaluation, donnees) => api.post(`${base}/evaluations/${idEvaluation}/n2`, donnees),
  signer: (idEvaluation, donnees) => api.post(`${base}/evaluations/${idEvaluation}/signature`, donnees),
  contester: (idEvaluation, donnees) => api.post(`${base}/evaluations/${idEvaluation}/contestation`, donnees),
  nouvelleVersion: (idEvaluation, motif) =>
    api.post(`${base}/evaluations/${idEvaluation}/nouvelle-version?motif=${encodeURIComponent(motif)}`),
  clore: (idEvaluation) => api.post(`${base}/evaluations/${idEvaluation}/cloturer`),

  ecarts: (idEmploye) => api.get(`${base}/employes/${idEmploye}/ecarts`),
  besoins: (idEmploye) => api.get(`${base}/employes/${idEmploye}/besoins`),
  creerBesoin: (donnees) => api.post(`${base}/besoins-formation`, donnees),
  deciderBesoin: (idBesoin, donnees) => api.post(`${base}/besoins-formation/${idBesoin}/decision`, donnees),
  comblerEcart: (idEcart, donnees) => api.post(`${base}/ecarts/${idEcart}/combler`, donnees),

  competences: () => api.get(`${base}/referentiel/competences`),
  creerCompetence: (donnees) => api.post(`${base}/referentiel/competences`, donnees),
  posteCompetences: (idPoste) => api.get(`${base}/referentiel/postes/${idPoste}/competences`),
  enregistrerPosteCompetence: (donnees) => api.post(`${base}/referentiel/postes/competences`, donnees),
  employeCompetences: (idEmploye) => api.get(`${base}/referentiel/employes/${idEmploye}/competences`),
  enregistrerNiveau: (idEmploye, donnees) =>
    api.post(`${base}/referentiel/employes/${idEmploye}/competences`, donnees),

  formations: () => api.get(`${base}/formation/catalogue`),
  creerFormation: (donnees) => api.post(`${base}/formation/catalogue`, donnees),
  sessions: () => api.get(`${base}/formation/sessions`),
  creerSession: (donnees) => api.post(`${base}/formation/sessions`, donnees),
  changerStatutSession: (idSession, statut) =>
    api.post(`${base}/formation/sessions/${idSession}/statut?statut=${encodeURIComponent(statut)}`),
  plans: (annee) => api.get(`${base}/formation/plans?annee=${annee}`),
  enregistrerPlan: (donnees) => api.post(`${base}/formation/plans`, donnees),
  inscriptionsSession: (idSession) => api.get(`${base}/formation/sessions/${idSession}/inscriptions`),
  inscriptionsEmploye: (idEmploye) => api.get(`${base}/formation/employes/${idEmploye}/inscriptions`),
  mesInscriptions: () => api.get(`${base}/formation/mes-inscriptions`),
  demanderInscription: (donnees) => api.post(`${base}/formation/inscriptions`, donnees),
  demanderInscriptionPourMoi: (idSession, idBesoin) =>
    api.post(`${base}/formation/inscriptions/moi?idSession=${idSession}${idBesoin ? `&idBesoin=${idBesoin}` : ''}`),
  deciderInscriptionManager: (idInscription, donnees) =>
    api.post(`${base}/formation/inscriptions/${idInscription}/decision-manager`, donnees),
  confirmerInscription: (idInscription) => api.post(`${base}/formation/inscriptions/${idInscription}/confirmer`),
  annulerInscription: (idInscription) => api.post(`${base}/formation/inscriptions/${idInscription}/annuler`),
  enregistrerPresence: (idInscription, donnees) =>
    api.post(`${base}/formation/inscriptions/${idInscription}/presence`, donnees),
  evaluerFormation: (idInscription, donnees) =>
    api.post(`${base}/formation/inscriptions/${idInscription}/evaluation`, donnees),
  evaluationsInscription: (idInscription) => api.get(`${base}/formation/inscriptions/${idInscription}/evaluations`),
  mesurer90Jours: (idInscription, donnees) =>
    api.post(`${base}/formation/inscriptions/${idInscription}/mesure-90j`, donnees),
  enregistrerConvention: (idInscription, donnees) =>
    api.post(`${base}/formation/inscriptions/${idInscription}/convention`, donnees),
  remboursement: (idInscription, moisRestants) =>
    api.get(`${base}/formation/inscriptions/${idInscription}/remboursement?moisRestants=${moisRestants}`),
  indicateurs: () => api.get(`${base}/formation/indicateurs`),
}

export { ErreurApi }
