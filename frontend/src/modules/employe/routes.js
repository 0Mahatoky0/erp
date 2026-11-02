export const employeRoutes = [
  {
    path: '/admin/employes',
    component: () => import('@/shared/layouts/AppLayout.vue'),
    children: [
      {
        path: '',
        name: 'employes',
        component: () => import('./views/EmployesView.vue'),
      },
    ],
    meta: { titre: 'Employes' },
  },
]
