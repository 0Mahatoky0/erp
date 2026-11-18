export const paieRoutes = [
  {
    path: '/admin/paie',
    component: () => import('@/shared/layouts/AppLayout.vue'),
    children: [
      {
        path: '',
        name: 'paie-variables',
        component: () => import('./views/PaieView.vue'),
        meta: { titre: 'Fiche de paie', roles: ['RH', 'FINANCE', 'DIRECTION'] },
      },
    ],
  },
  {
    path: '/mes-fiches-paie',
    component: () => import('@/shared/layouts/AppLayout.vue'),
    children: [
      {
        path: '',
        name: 'paie-mes-fiches',
        component: () => import('./views/MesFichesView.vue'),
        meta: { titre: 'Mes fiches de paie', roles: ['EMPLOYE'] },
      },
    ],
  },
]
