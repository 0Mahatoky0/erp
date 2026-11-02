import { defineStore } from 'pinia'

let compteur = 0

/**
 * Notifications temporaires affichees en haut a droite de l'ecran.
 *
 * <p>Chaque message disparait automatiquement apres quelques secondes.
 */
export const useNotificationStore = defineStore('notification', {
  state: () => ({
    messages: [],
  }),

  actions: {
    /** Ajoute une notification et programme sa disparition. */
    ajouter(texte, type = 'info', titre = null, duree = 5000) {
      const id = ++compteur
      this.messages.push({ id, texte, type, titre })
      if (duree > 0) {
        window.setTimeout(() => this.fermer(id), duree)
      }
      return id
    },

    /** Message de succes. */
    succes(texte, titre = 'Operation effectuee') {
      return this.ajouter(texte, 'succes', titre)
    },

    /** Message d'erreur. */
    erreur(texte, titre = 'Operation impossible') {
      return this.ajouter(texte, 'erreur', titre, 8000)
    },

    /** Ferme une notification. */
    fermer(id) {
      this.messages = this.messages.filter((message) => message.id !== id)
    },
  },
})
