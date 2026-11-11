export const paieRoutes = [
  {
    path: '/admin/paie',
    component: () => import('@/shared/layouts/AppLayout.vue'),
    children: [
      {
        path: '',
        name: 'paie-variables',
        component: () => import('./views/PaieView.vue'),
        meta: { titre: 'Variables de paie', roles: ['RH', 'FINANCE', 'DIRECTION'] },
      },
    ],
  },
]
