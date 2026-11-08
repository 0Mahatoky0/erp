import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/shared/store/useAuthStore'
import { iamRoutes } from '@/modules/iam/routes'
import { employeRoutes } from '@/modules/employe/routes'
import { besoinRoutes } from '@/modules/besoin/routes'
import { recrutementRoutes } from '@/modules/recrutement/routes'
import { embaucheRoutes } from '@/modules/embauche/routes'

const routes = [
  ...iamRoutes,
  ...employeRoutes,
  ...besoinRoutes,
  ...recrutementRoutes,
  ...embaucheRoutes,
  {
    path: '/:pathMatch(.*)*',
    name: 'introuvable',
    component: () => import('@/shared/components/IntrouvableView.vue'),
    meta: { titre: 'Page introuvable', public: true },
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

router.beforeEach(async (vers) => {
  const auth = useAuthStore()

  if (auth.token && !auth.profil) await auth.rechargerProfil()
  if (!vers.meta.public && !auth.connecte) {
    return { name: 'connexion', query: { suite: vers.fullPath } }
  }
  if (vers.name === 'connexion' && auth.connecte) return { name: 'tableau-bord' }

  const roles = vers.meta.roles
  if (roles && !roles.includes(auth.role)) return { name: 'tableau-bord' }
  return true
})

router.afterEach((vers) => {
  document.title = vers.meta.titre ? `${vers.meta.titre} · ERP TOVO` : 'ERP TOVO'
})

export default router
