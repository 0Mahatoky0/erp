/**
 * Statuts et transitions métier de la candidature (module recrutement).
 * L’ordre suit le parcours : tri, présélection, évaluation, décision, embauche.
 */
export const STATUTS_CANDIDATURE = [
  { valeur: 'RECUE', libelle: 'Reçue', groupe: 'preselection' },
  { valeur: 'EN_ETUDE', libelle: 'En étude', groupe: 'preselection' },
  { valeur: 'PRESELECTIONNEE', libelle: 'Présélectionnée', groupe: 'preselection' },
  { valeur: 'EN_EVALUATION', libelle: 'En évaluation', groupe: 'evaluation' },
  { valeur: 'LISTE_ATTENTE', libelle: 'Liste d’attente', groupe: 'evaluation' },
  { valeur: 'RETENUE', libelle: 'Retenue', groupe: 'decision' },
  { valeur: 'ACCEPTEE', libelle: 'Acceptée', groupe: 'embauche' },
  { valeur: 'EMBAUCHEE', libelle: 'Embauchée', groupe: 'embauche' },
  { valeur: 'REFUSEE', libelle: 'Refusée', groupe: 'cloturee' },
  { valeur: 'REFUSEE_PAR_CANDIDAT', libelle: 'Refusée par le candidat', groupe: 'cloturee' },
  { valeur: 'EXPIREE', libelle: 'Expirée', groupe: 'cloturee' },
  { valeur: 'RETIREE', libelle: 'Retirée', groupe: 'cloturee' },
]

export const STATUTS_PRESELECTION = ['RECUE', 'EN_ETUDE', 'PRESELECTIONNEE']

/** Transitions autorisées côté serveur et libellé de l’action correspondante. */
export const TRANSITIONS_CANDIDATURE = {
  RECUE: [
    { statut: 'EN_ETUDE', libelle: 'Étudier', icone: 'pi pi-search', severite: 'secondary' },
  ],
  EN_ETUDE: [
    { statut: 'PRESELECTIONNEE', libelle: 'Présélectionner', icone: 'pi pi-check', severite: 'success' },
    { statut: 'REFUSEE', libelle: 'Écarter', icone: 'pi pi-times', severite: 'danger' },
  ],
  PRESELECTIONNEE: [
    { statut: 'EN_EVALUATION', libelle: 'Envoyer en évaluation', icone: 'pi pi-arrow-right', severite: 'success' },
    { statut: 'REFUSEE', libelle: 'Écarter', icone: 'pi pi-times', severite: 'danger' },
  ],
  EN_EVALUATION: [
    { statut: 'RETENUE', libelle: 'Retenir', icone: 'pi pi-check', severite: 'success' },
    { statut: 'LISTE_ATTENTE', libelle: 'Mettre en liste d’attente', icone: 'pi pi-clock', severite: 'warn' },
    { statut: 'REFUSEE', libelle: 'Écarter', icone: 'pi pi-times', severite: 'danger' },
  ],
  LISTE_ATTENTE: [
    { statut: 'RETENUE', libelle: 'Retenir', icone: 'pi pi-check', severite: 'success' },
    { statut: 'REFUSEE', libelle: 'Écarter', icone: 'pi pi-times', severite: 'danger' },
  ],
}

export function libelleStatut(valeur) {
  return STATUTS_CANDIDATURE.find((statut) => statut.valeur === valeur)?.libelle || valeur || '—'
}
