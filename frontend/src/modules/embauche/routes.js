export const embaucheRoutes = [
  {
    path: '/admin/embauche',
    component: () => import('@/shared/layouts/AppLayout.vue'),
    children: [
      {
        path: 'propositions',
        name: 'embauche-proposition-liste',
        component: () => import('./views/PropositionEmbaucheListeView.vue'),
        meta: { titre: 'Propositions d’embauche', roles: ['RH', 'DIRECTION'] },
      },
      {
        path: 'propositions/nouvelle',
        name: 'embauche-proposition-saisie',
        component: () => import('./views/PropositionEmbaucheSaisieView.vue'),
        meta: { titre: 'Nouvelle proposition d’embauche', roles: ['RH', 'DIRECTION'] },
      },
      {
        path: 'propositions/:idProposition',
        name: 'embauche-proposition-fiche',
        component: () => import('./views/PropositionEmbaucheFicheView.vue'),
        meta: { titre: 'Fiche proposition d’embauche', roles: ['RH', 'DIRECTION'] },
      },
      {
        path: 'contrats',
        name: 'embauche-contrat-liste',
        component: () => import('./views/ContratEmbaucheListeView.vue'),
        meta: { titre: 'Contrats d’embauche', roles: ['RH', 'DIRECTION'] },
      },
      {
        path: 'contrats/:idContrat',
        name: 'embauche-contrat-fiche',
        component: () => import('./views/ContratEmbaucheFicheView.vue'),
        meta: { titre: 'Fiche contrat d’embauche', roles: ['RH', 'DIRECTION'] },
      },
    ],
  },
  {
    path: '/candidat/propositions',
    name: 'candidat-proposition-liste',
    component: () => import('./views/PropositionEmbaucheListeView.vue'),
    meta: { titre: 'Mes propositions d’embauche', roles: ['CANDIDAT'] },
  },
  {
    path: '/candidat/contrats',
    name: 'candidat-contrat-liste',
    component: () => import('./views/ContratEmbaucheListeView.vue'),
    meta: { titre: 'Mes contrats proposés', roles: ['CANDIDAT'] },
  },
  {
    path: '/candidat/contrats/:idContrat',
    name: 'candidat-contrat-fiche',
    component: () => import('./views/ContratEmbaucheFicheView.vue'),
    meta: { titre: 'Mon contrat proposé', roles: ['CANDIDAT'] },
  },
  {
    path: '/candidat/propositions/:idProposition',
    name: 'candidat-proposition-fiche',
    component: () => import('./views/PropositionEmbaucheFicheView.vue'),
    meta: { titre: 'Ma proposition d’embauche', roles: ['CANDIDAT'] },
  },
]
