export const ruptureRoutes = [
  {
    path: '/admin/rupture',
    component: () => import('@/shared/layouts/AppLayout.vue'),
    children: [
      {
        path: '',
        name: 'rupture-dossiers',
        component: () => import('./views/RuptureView.vue'),
        meta: { titre: 'Rupture et offboarding', roles: ['RH', 'DIRECTION', 'MANAGER', 'FINANCE'] },
      },
    ],
  },
]
