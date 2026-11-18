export const carriereRoutes = [
  {
    path: '/admin/carriere',
    component: () => import('@/shared/layouts/AppLayout.vue'),
    children: [
      {
        path: '',
        name: 'carriere-mobilite',
        component: () => import('./views/CarriereView.vue'),
        meta: { titre: 'Carrière et mobilité', roles: ['RH', 'DIRECTION', 'MANAGER', 'FINANCE'] },
      },
    ],
  },
]
