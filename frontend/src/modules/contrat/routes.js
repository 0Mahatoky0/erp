export const contratRoutes = [
  {
    path: '/admin/contrats',
    component: () => import('@/shared/layouts/AppLayout.vue'),
    children: [
      {
        path: '',
        name: 'contrat-liste',
        component: () => import('./views/ContratListeView.vue'),
        meta: { titre: 'Contrats · Bloc 4', roles: ['RH', 'DIRECTION', 'MANAGER', 'FINANCE'] },
      },
      {
        path: 'onboarding',
        name: 'contrat-onboarding-liste',
        component: () => import('./views/OnboardingListeView.vue'),
        meta: { titre: 'Onboarding · Bloc 4', roles: ['RH', 'DIRECTION', 'MANAGER', 'FINANCE'] },
      },
      {
        path: ':id',
        name: 'contrat-detail',
        component: () => import('./views/ContratDetailView.vue'),
        meta: { titre: 'Détail du contrat · Bloc 4', roles: ['RH', 'DIRECTION', 'MANAGER', 'FINANCE'] },
      },
    ],
  },
]
