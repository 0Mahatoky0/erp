export const employeRoutes = [
  {
    path: '/admin/employes',
    component: () => import('@/shared/layouts/AppLayout.vue'),
    children: [
      {
        path: '',
        name: 'employes',
        component: () => import('./views/EmployesView.vue'),
        meta: { titre: 'Employes', roles: ['RH'] },
      },
      {
        path: ':id',
        name: 'employe-dossier',
        component: () => import('./views/DossierSalarieView.vue'),
        meta: { titre: 'Dossier salarie', roles: ['RH'] },
      },
    ],
    meta: { titre: 'Employes' },
  },
]
