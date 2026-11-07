export const recrutementRoutes = [
  {
    path: '/',
    name: 'accueil-candidat',
    component: () => import('./views/AccueilCandidatView.vue'),
    meta: { titre: 'Offres d’emploi', public: true },
  },
  {
    path: '/candidat',
    name: 'espace-candidat',
    component: () => import('./views/EspaceCandidatView.vue'),
    meta: { titre: 'Espace candidat', roles: ['CANDIDAT'] },
  },
  {
    path: '/admin/recrutement',
    component: () => import('@/shared/layouts/AppLayout.vue'),
    children: [
      {
        path: '',
        redirect: { name: 'recrutement-offre-liste' },
        meta: { titre: 'Recrutement' },
      },
      {
        path: 'offres',
        name: 'recrutement-offre-liste',
        component: () => import('./views/OffreListeView.vue'),
        meta: { titre: 'Offres d’emploi · Recrutement', roles: ['RH', 'DIRECTION', 'MANAGER'] },
      },
      {
        path: 'offres/nouvelle',
        name: 'recrutement-offre-saisie',
        component: () => import('./views/OffreSaisieView.vue'),
        meta: { titre: 'Créer une offre · Recrutement', roles: ['RH', 'DIRECTION'] },
      },
      {
        path: 'offres/:idOffre/modifier',
        name: 'recrutement-offre-modification',
        component: () => import('./views/OffreSaisieView.vue'),
        meta: { titre: 'Modifier l’offre · Recrutement', roles: ['RH', 'DIRECTION'] },
      },
      {
        path: 'offres/:idOffre',
        name: 'recrutement-offre-fiche',
        component: () => import('./views/OffreFicheView.vue'),
        meta: { titre: 'Fiche offre', menu: 'recrutement-offre-liste' },
      },
      {
        path: 'candidatures',
        name: 'recrutement-candidature-liste',
        component: () => import('./views/CandidatureListeView.vue'),
        meta: { titre: 'Liste des candidatures · Recrutement', roles: ['RH', 'DIRECTION', 'MANAGER'] },
      },
      {
        path: 'evaluation/offres/:idOffre',
        name: 'recrutement-candidature-evaluation-liste',
        component: () => import('./views/CandidatureEvaluationListeView.vue'),
        meta: { titre: 'Évaluation des candidatures · Recrutement', roles: ['RH', 'DIRECTION', 'MANAGER'] },
      },
      {
        path: 'evaluation/offres/:idOffre/candidatures/:idCandidature',
        name: 'recrutement-candidature-evaluation-fiche',
        component: () => import('./views/CandidatureEvaluationFicheView.vue'),
        meta: { titre: 'Fiche d’évaluation · Recrutement', roles: ['RH', 'DIRECTION'] },
      },
      {
        path: 'selection',
        name: 'recrutement-selection',
        redirect: { name: 'recrutement-candidature-liste' },
        meta: { titre: 'Présélection · Recrutement', roles: ['RH', 'DIRECTION', 'MANAGER'] },
      },
      {
        path: 'suivi',
        name: 'recrutement-suivi',
        redirect: { name: 'recrutement-candidature-liste' },
        meta: { titre: 'Suivi des candidats · Recrutement', roles: ['RH', 'DIRECTION', 'MANAGER'] },
      },
      {
        path: 'candidatures/:idCandidature',
        name: 'recrutement-candidature-fiche',
        component: () => import('./views/CandidatureFicheView.vue'),
        meta: { titre: 'Fiche candidature · Recrutement', roles: ['RH', 'DIRECTION', 'MANAGER'] },
      },
    ],
  },
]