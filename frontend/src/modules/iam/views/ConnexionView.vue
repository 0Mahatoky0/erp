<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/shared/store/useAuthStore'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const email = ref('manager@tovo.mg')
const motDePasse = ref('Manager@2026')
const erreur = ref(null)
const envoi = ref(false)

const COMPTES = [
  { role: 'Manager', email: 'manager@tovo.mg', motDePasse: 'Manager@2026' },
  { role: 'RH', email: 'rh@tovo.mg', motDePasse: 'Rh@2026' },
  { role: 'Finance', email: 'finance@tovo.mg', motDePasse: 'Finance@2026' },
  { role: 'Direction', email: 'direction@tovo.mg', motDePasse: 'Direction@2026' },
]

function remplir(compte) {
  email.value = compte.email
  motDePasse.value = compte.motDePasse
}

async function seConnecter() {
  erreur.value = null
  envoi.value = true
  try {
    await auth.connexion(email.value.trim(), motDePasse.value)
    const suite = typeof route.query.suite === 'string' ? route.query.suite : null
    router.push(suite || { name: 'tableau-bord' })
  } catch (e) {
    erreur.value = e.message || 'Connexion impossible.'
  } finally {
    envoi.value = false
  }
}
</script>

<template>
  <div class="connexion">
    <section class="connexion__presentation">
      <div class="connexion__marque">
        <div class="sidebar__logo">T</div>
        <div>
          <div class="sidebar__titre">ERP TOVO</div>
          <div class="sidebar__sous-titre">Recrutement</div>
        </div>
      </div>

      <h1 class="connexion__titre">Pilotez vos besoins en recrutement, de la demande a la validation.</h1>
      <p class="connexion__texte">
        Demandes multi-lignes, remplacements, circuit de validation par etape et controle budgetaire :
        tout le processus reste trace dans un seul outil.
      </p>

      <div class="connexion__liste">
        <div class="connexion__item">
          <span class="connexion__puce">1</span>
          <span>Le manager saisit sa demande, ligne par ligne, et la soumet.</span>
        </div>
        <div class="connexion__item">
          <span class="connexion__puce">2</span>
          <span>RH, Finance puis Direction valident chaque ligne a leur tour.</span>
        </div>
        <div class="connexion__item">
          <span class="connexion__puce">3</span>
          <span>Le poste officiel est cree automatiquement a l'acceptation.</span>
        </div>
      </div>
    </section>

    <section class="connexion__panneau">
      <div class="connexion__boite">
        <h2 class="connexion__titre-formulaire">Connexion</h2>
        <p class="connexion__sous-titre">Accedez a votre espace de travail.</p>

        <div v-if="erreur" class="alerte alerte--erreur">{{ erreur }}</div>

        <form @submit.prevent="seConnecter">
          <div class="champ">
            <label class="champ__libelle" for="email">Adresse e-mail</label>
            <input
              id="email"
              v-model="email"
              class="saisie"
              type="email"
              autocomplete="username"
              placeholder="prenom.nom@tovo.mg"
              required
            />
          </div>

          <div class="champ">
            <label class="champ__libelle" for="motDePasse">Mot de passe</label>
            <input
              id="motDePasse"
              v-model="motDePasse"
              class="saisie"
              type="password"
              autocomplete="current-password"
              required
            />
          </div>

          <button class="bouton bouton--principal bouton--bloc" type="submit" :disabled="envoi">
            {{ envoi ? 'Connexion...' : 'Se connecter' }}
          </button>
        </form>

        <div class="comptes-demo">
          <div style="margin-bottom: 6px">Comptes de demonstration :</div>
          <div v-for="compte in COMPTES" :key="compte.email" class="comptes-demo__ligne">
            <span>{{ compte.role }}</span>
            <button class="comptes-demo__bouton" type="button" @click="remplir(compte)">
              {{ compte.email }}
            </button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>
