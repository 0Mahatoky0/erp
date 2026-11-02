export const besoinRoutes = [
  {
    path: '/admin',
    component: () => import('@/shared/layouts/AppLayout.vue'),
    children: [
      {
        path: '',
        name: 'tableau-bord',
        component: () => import('./views/TableauBordView.vue'),
        meta: { titre: 'Tableau de bord' },
      },
      {
        path: 'demandes',
        name: 'demandes',
        component: () => import('./views/DemandesView.vue'),
        meta: { titre: 'Demandes de recrutement' },
      },
      {
        path: 'demandes/nouvelle',
        name: 'demande-nouvelle',
        component: () => import('./views/DemandeNouvelleView.vue'),
        meta: { titre: 'Nouvelle demande' },
      },
      {
        path: 'demandes/:id',
        name: 'demande-detail',
        component: () => import('./views/DemandeDetailView.vue'),
        meta: { titre: 'Detail de la demande' },
      },
      {
        path: 'validation',
        name: 'validation',
        component: () => import('./views/ValidationView.vue'),
        meta: { titre: 'File de validation' },
      },
      {
        path: 'postes',
        name: 'postes',
        component: () => import('./views/PostesView.vue'),
        meta: { titre: 'Catalogue des postes' },
      },
      {
        path: 'budgets',
        name: 'budgets',
        component: () => import('./views/BudgetsView.vue'),
        meta: { titre: 'Budgets' },
      },
    ],
  },
]
