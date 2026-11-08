export const embaucheRoutes = [
  {
    path: '/embauche/propositions/token/:token',
    name: 'embauche-proposition-publique',
    component: () => import('./views/PropositionPubliqueView.vue'),
    meta: { titre: 'Proposition d’embauche', public: true },
  },
  {
    path: '/admin/embauche',
    component: () => import('@/shared/layouts/AppLayout.vue'),
    children: [
      {
        path: '',
        redirect: { name: 'embauche-proposition-liste' },
        meta: { titre: 'Embauche' },
      },
      {
        path: 'propositions',
        name: 'embauche-proposition-liste',
        component: () => import('./views/PropositionListeView.vue'),
        meta: { titre: 'Propositions d’embauche', roles: ['RH', 'DIRECTION', 'MANAGER', 'FINANCE'] },
      },
      {
        path: 'propositions/nouvelle',
        name: 'embauche-proposition-nouvelle',
        component: () => import('./views/PropositionSaisieView.vue'),
        meta: { titre: 'Nouvelle proposition · Embauche', roles: ['RH', 'DIRECTION'] },
      },
      {
        path: 'propositions/:id/modifier',
        name: 'embauche-proposition-modification',
        component: () => import('./views/PropositionSaisieView.vue'),
        meta: { titre: 'Modifier la proposition · Embauche', roles: ['RH', 'DIRECTION'] },
      },
      {
        path: 'propositions/:id',
        name: 'embauche-proposition-detail',
        component: () => import('./views/PropositionDetailView.vue'),
        meta: { titre: 'Détail de la proposition · Embauche', roles: ['RH', 'DIRECTION', 'MANAGER', 'FINANCE'] },
      },
      {
        path: 'employes/:id',
        name: 'embauche-dossier-employe',
        component: () => import('./views/DossierEmbaucheView.vue'),
        meta: { titre: 'Dossier salarié · Embauche', roles: ['RH', 'DIRECTION', 'MANAGER', 'FINANCE'] },
      },
    ],
  },
]
