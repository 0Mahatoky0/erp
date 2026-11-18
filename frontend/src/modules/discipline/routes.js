export const disciplineRoutes = [
  {
    path: '/admin/discipline',
    component: () => import('@/shared/layouts/AppLayout.vue'),
    children: [
      {
        path: '',
        name: 'discipline-dossiers',
        component: () => import('./views/DisciplineView.vue'),
        meta: { titre: 'Discipline et procédures', roles: ['RH', 'DIRECTION', 'MANAGER'] },
      },
    ],
  },
]
