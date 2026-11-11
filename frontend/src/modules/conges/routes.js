export const congesRoutes = [
  {
    path: '/admin/conges',
    component: () => import('@/shared/layouts/AppLayout.vue'),
    children: [
      {
        path: '',
        name: 'conges-liste',
        component: () => import('./views/CongesView.vue'),
        meta: { titre: 'Congés et absences · Bloc 7', roles: ['RH', 'DIRECTION', 'MANAGER', 'EMPLOYE'] },
      },
    ],
  },
]
