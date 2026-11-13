export const tempsRoutes = [
  {
    path: '/admin/temps',
    component: () => import('@/shared/layouts/AppLayout.vue'),
    children: [
      {
        path: '',
        name: 'temps-pointage-liste',
        component: () => import('./views/TempsPointageView.vue'),
        meta: { titre: 'Temps de travail et pointage · Bloc 6', roles: ['RH', 'DIRECTION', 'MANAGER'] },
      },
      {
        path: 'regimes',
        name: 'temps-regimes',
        component: () => import('./views/RegimesTravailView.vue'),
        meta: { titre: 'Régimes et horaires · Bloc 6', roles: ['RH', 'DIRECTION', 'MANAGER'] },
      },
    ],
  },
]
