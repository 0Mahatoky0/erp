export const iamRoutes = [
  {
    path: '/connexion',
    name: 'connexion',
    component: () => import('./views/ConnexionView.vue'),
    meta: { titre: 'Connexion', public: true },
  },
  {
    path: '/admin/utilisateurs',
    component: () => import('@/shared/layouts/AppLayout.vue'),
    children: [
      {
        path: '',
        name: 'utilisateurs',
        component: () => import('./views/UtilisateursView.vue'),
      },
    ],
    meta: { titre: 'Utilisateurs' },
  },
]
