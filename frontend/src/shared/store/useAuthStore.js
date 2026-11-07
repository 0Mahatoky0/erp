import { defineStore } from 'pinia'
import { api, definirJeton, surJetonExpire } from '@/shared/api/axios'

const CLE_TOKEN = 'erp.token'

/**
 * Etat de l'utilisateur connecte (RM-001 a RM-004).
 *
 * <p>Le jeton est conserve dans le stockage local pour survivre a un rechargement
 * de page ; le profil est recharge au demarrage.
 */
export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(CLE_TOKEN),
    profil: null,
    chargement: false,
  }),

  getters: {
    connecte: (state) => Boolean(state.token && state.profil),
    role: (state) => state.profil?.role ?? null,
    estManager: (state) => state.profil?.role === 'MANAGER',
    estRh: (state) => state.profil?.role === 'RH',
    estFinance: (state) => state.profil?.role === 'FINANCE',
    estDirection: (state) => state.profil?.role === 'DIRECTION',
    initiales: (state) => {
      if (!state.profil?.nom) {
        return '?'
      }
      return state.profil.nom
        .split(' ')
        .filter(Boolean)
        .slice(0, 2)
        .map((part) => part[0].toUpperCase())
        .join('')
    },
  },

  actions: {
    appliquerReponse(reponse) {
      this.token = reponse.token
      this.profil = reponse.profil
      localStorage.setItem(CLE_TOKEN, reponse.token)
      definirJeton(reponse.token)
      return reponse.profil
    },

    /** Connexion avec identifiants (RM-001, RM-002). */
    async connexion(email, motDePasse) {
      this.chargement = true
      surJetonExpire(() => this.deconnexion())
      try {
        const reponse = await api.post('/auth/login', { email, motDePasse })
        return this.appliquerReponse(reponse)
      } finally {
        this.chargement = false
      }
    },

    async inscriptionCandidat(donnees) {
      this.chargement = true
      try {
        return this.appliquerReponse(await api.post('/recrutement/candidats/inscription', donnees))
      } finally {
        this.chargement = false
      }
    },

    /** Recharge le profil depuis le serveur au demarrage de l'application. */
    async rechargerProfil() {
      if (!this.token) {
        return null
      }
      definirJeton(this.token)
      surJetonExpire(() => this.deconnexion())
      try {
        this.profil = await api.get('/auth/me')
        return this.profil
      } catch {
        this.deconnexion()
        return null
      }
    },

    /** Deconnexion : le jeton local est efface. */
    deconnexion() {
      this.token = null
      this.profil = null
      localStorage.removeItem(CLE_TOKEN)
      definirJeton(null)
    },

    /** Verifie qu'un role donne est autorise (RM-067). */
    aRole(...roles) {
      return roles.includes(this.role)
    },
  },
})
