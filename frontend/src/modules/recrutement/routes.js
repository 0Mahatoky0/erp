export const recrutementRoutes = [
  {
    path: '/',
    name: 'accueil-candidat',
    component: () => import('./views/AccueilCandidatView.vue'),
    meta: { titre: 'Offres d’emploi', public: true },
  },
  {
    path: '/admin/recrutement',
    component: () => import('@/shared/layouts/AppLayout.vue'),
    children: [
      {
        path: '',
        name: 'recrutement',
        component: () => import('./views/OffresListeView.vue'),
        meta: { titre: 'Recrutement' },
      },
      {
        path: 'demandes-acceptees',
        name: 'recrutement-demandes',
        component: () => import('./views/RecrutementDemandesView.vue'),
        meta: { titre: 'Demandes acceptees · Recrutement', roles: ['RH', 'DIRECTION', 'MANAGER'] },
      },
      {
        path: 'offres',
        name: 'recrutement-offres',
        component: () => import('./views/OffresListeView.vue'),
        meta: { titre: 'Offres d’emploi · Recrutement', roles: ['RH', 'DIRECTION', 'MANAGER'] },
      },
      {
        path: 'recrutement/offres/:idOffre', // ← pas :id
        name: 'recrutement-offre-detail',
        component: () => import('./views/OffreDetailView.vue'),
        meta: { titre: 'Détail de l’offre', menu: 'recrutement-offres' },
      },
      {
        path: 'candidatures',
        name: 'recrutement-candidatures',
        component: () => import('./views/RecrutementCandidaturesView.vue'),
        meta: { titre: 'Candidatures · Recrutement', roles: ['RH', 'DIRECTION', 'MANAGER'] },
      },
      {
        path: 'selection',
        name: 'recrutement-selection',
        component: () => import('./views/RecrutementSelectionView.vue'),
        meta: { titre: 'Présélection · Recrutement', roles: ['RH', 'DIRECTION', 'MANAGER'] },
      },
      {
        path: 'suivi',
        name: 'recrutement-suivi',
        component: () => import('./views/RecrutementSuiviView.vue'),
        meta: { titre: 'Suivi des candidats · Recrutement', roles: ['RH', 'DIRECTION', 'MANAGER'] },
      },
      {
        path: 'candidatures/:id',
        name: 'recrutement-candidature-detail',
        component: () => import('./views/RecrutementCandidatureDetailView.vue'),
        meta: { titre: 'Parcours candidat · Recrutement', roles: ['RH', 'DIRECTION', 'MANAGER'] },
      },
    ],
  },
]
