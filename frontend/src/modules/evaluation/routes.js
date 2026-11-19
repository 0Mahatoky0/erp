export const evaluationRoutes = [
  {
    path: '/admin/evaluation',
    component: () => import('@/shared/layouts/AppLayout.vue'),
    children: [
      {
        path: '',
        name: 'evaluation-campagnes',
        component: () => import('./views/EvaluationView.vue'),
        meta: { titre: 'Évaluation et compétences', roles: ['RH', 'DIRECTION', 'MANAGER'] },
      },
      {
        path: 'competences',
        name: 'evaluation-competences',
        component: () => import('./views/EvaluationView.vue'),
        meta: { titre: 'Objectifs et compétences', roles: ['RH', 'DIRECTION', 'MANAGER'] },
      },
    ],
  },
  {
    path: '/admin/formation',
    component: () => import('@/shared/layouts/AppLayout.vue'),
    children: [
      {
        path: '',
        name: 'formation-planification',
        component: () => import('./views/FormationView.vue'),
        meta: { titre: 'Planification des formations', roles: ['RH', 'DIRECTION', 'FINANCE', 'MANAGER'] },
      },
    ],
  },
  {
    path: '/mes-evaluations',
    component: () => import('@/shared/layouts/AppLayout.vue'),
    children: [
      {
        path: '',
        name: 'evaluation-mes-objectifs',
        component: () => import('./views/MesEvaluationsView.vue'),
        meta: { titre: 'Mes évaluations', roles: ['EMPLOYE'] },
      },
    ],
  },
  {
    path: '/mes-formations',
    component: () => import('@/shared/layouts/AppLayout.vue'),
    children: [
      {
        path: '',
        name: 'formation-mes-inscriptions',
        component: () => import('./views/MesFormationsView.vue'),
        meta: { titre: 'Mes formations', roles: ['EMPLOYE'] },
      },
    ],
  },
]
