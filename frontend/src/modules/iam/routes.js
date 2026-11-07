export const iamRoutes = [
  {
    path: '/connexion',
    name: 'connexion',
    component: () => import('./views/ConnexionView.vue'),
    meta: { titre: 'Connexion', public: true },
  },
  {
    path: '/connexion-candidat',
    name: 'connexion-candidat',
    component: () => import('./views/ConnexionView.vue'),
    meta: { titre: 'Connexion candidat', public: true, candidat: true },
  },
  {
    path: '/inscription-candidat',
    name: 'inscription-candidat',
    component: () => import('./views/InscriptionCandidatView.vue'),
    meta: { titre: 'Créer un compte candidat', public: true },
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
