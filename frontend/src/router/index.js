import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/connexion',
    name: 'connexion',
    component: () => import('@/views/ConnexionView.vue'),
    meta: { titre: 'Connexion', public: true },
  },
  {
    path: '/',
    component: () => import('@/layouts/AppLayout.vue'),
    children: [
      {
        path: '',
        name: 'tableau-bord',
        component: () => import('@/views/TableauBordView.vue'),
        meta: { titre: 'Tableau de bord' },
      },
      {
        path: 'demandes',
        name: 'demandes',
        component: () => import('@/views/DemandesView.vue'),
        meta: { titre: 'Demandes de recrutement' },
      },
      {
        path: 'demandes/nouvelle',
        name: 'demande-nouvelle',
        component: () => import('@/views/DemandeNouvelleView.vue'),
        meta: { titre: 'Nouvelle demande' },
      },
      {
        path: 'demandes/:id',
        name: 'demande-detail',
        component: () => import('@/views/DemandeDetailView.vue'),
        meta: { titre: 'Detail de la demande' },
      },
      {
        path: 'validation',
        name: 'validation',
        component: () => import('@/views/ValidationView.vue'),
        meta: { titre: 'File de validation' },
      },
      {
        path: 'postes',
        name: 'postes',
        component: () => import('@/views/PostesView.vue'),
        meta: { titre: 'Catalogue des postes' },
      },
      {
        path: 'budgets',
        name: 'budgets',
        component: () => import('@/views/BudgetsView.vue'),
        meta: { titre: 'Budgets' },
      },
      {
        path: 'employes',
        name: 'employes',
        component: () => import('@/views/EmployesView.vue'),
        meta: { titre: 'Employes' },
      },
      {
        path: 'utilisateurs',
        name: 'utilisateurs',
        component: () => import('@/views/UtilisateursView.vue'),
        meta: { titre: 'Utilisateurs' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'introuvable',
    component: () => import('@/views/IntrouvableView.vue'),
    meta: { titre: 'Page introuvable', public: true },
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

/**
 * Garde de navigation : les ecrans demandes exigent une session ouverte
 * et, pour certains, un role particulier (RM-067).
 */
router.beforeEach(async (vers) => {
  const auth = useAuthStore()

  if (auth.token && !auth.profil) {
    await auth.rechargerProfil()
  }

  if (!vers.meta.public && !auth.connecte) {
    return { name: 'connexion', query: { suite: vers.fullPath } }
  }

  if (vers.name === 'connexion' && auth.connecte) {
    return { name: 'tableau-bord' }
  }

  const roles = vers.meta.roles
  if (roles && !roles.includes(auth.role)) {
    return { name: 'tableau-bord' }
  }

  return true
})

router.afterEach((vers) => {
  document.title = vers.meta.titre ? `${vers.meta.titre} · ERP TOVO` : 'ERP TOVO'
})

export default router
