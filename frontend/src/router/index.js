import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/shared/store/useAuthStore'
import { iamRoutes } from '@/modules/iam/routes'
import { employeRoutes } from '@/modules/employe/routes'
import { besoinRoutes } from '@/modules/besoin/routes'
import { recrutementRoutes } from '@/modules/recrutement/routes'
import { embaucheRoutes } from '@/modules/embauche/routes'
import { contratRoutes } from '@/modules/contrat/routes'
import { tempsRoutes } from '@/modules/temps/routes'
import { congesRoutes } from '@/modules/conges/routes'
import { paieRoutes } from '@/modules/paie/routes'

const routes = [
  ...iamRoutes,
  ...employeRoutes,
  ...besoinRoutes,
  ...recrutementRoutes,
  ...embaucheRoutes,
  ...contratRoutes,
  ...tempsRoutes,
  ...congesRoutes,
  ...paieRoutes,
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
  if (['connexion', 'connexion-candidat', 'inscription-candidat'].includes(vers.name) && auth.connecte) {
    return auth.role === 'CANDIDAT' ? { name: 'espace-candidat' } : { name: 'tableau-bord' }
  }

  const roles = vers.meta.roles
  if (roles && !roles.includes(auth.role)) return { name: 'tableau-bord' }
  return true
})

router.afterEach((vers) => {
  document.title = vers.meta.titre ? `${vers.meta.titre} · ERP TOVO` : 'ERP TOVO'
})

export default router
