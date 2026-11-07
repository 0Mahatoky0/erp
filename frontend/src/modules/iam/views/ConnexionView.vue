<script setup>
import PrimeButton from 'primevue/button'
import PrimeInputText from 'primevue/inputtext'
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/shared/store/useAuthStore'
import { RouterLink } from 'vue-router'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const candidat = route.name === 'connexion-candidat'

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
    router.push(suite || (candidat ? { name: 'espace-candidat' } : { name: 'tableau-bord' }))
  } catch (e) {
    erreur.value = e.message || 'Connexion impossible.'
  } finally {
    envoi.value = false
  }
}
</script>

<template>
  <div class="grid min-h-screen grid-cols-1 bg-surface-50 lg:grid-cols-2 dark:bg-surface-950">
    <section class="hidden flex-col justify-center gap-6 bg-primary p-10 text-primary-contrast lg:flex">
      <div class="text-3xl font-bold">
        <div class="flex size-10 items-center justify-center rounded-xl bg-primary text-primary-contrast">T</div>
        <div>
          <div class="text-base font-bold">ERP TOVO</div>
          <div class="text-xs uppercase tracking-wide text-muted-color">Recrutement</div>
        </div>
      </div>

      <h1 class="text-4xl font-bold">Pilotez vos besoins en recrutement, de la demande a la validation.</h1>
      <p class="max-w-lg text-lg opacity-90">
        Demandes multi-lignes, remplacements, circuit de validation par etape et controle budgetaire :
        tout le processus reste trace dans un seul outil.
      </p>

      <div class="flex flex-col gap-3">
        <div class="flex items-center gap-3">
          <span class="flex size-8 shrink-0 items-center justify-center rounded-full bg-primary-100 text-primary dark:bg-primary-900">1</span>
          <span>Le manager saisit sa demande, ligne par ligne, et la soumet.</span>
        </div>
        <div class="flex items-center gap-3">
          <span class="flex size-8 shrink-0 items-center justify-center rounded-full bg-primary-100 text-primary dark:bg-primary-900">2</span>
          <span>RH, Finance puis Direction valident chaque ligne a leur tour.</span>
        </div>
        <div class="flex items-center gap-3">
          <span class="flex size-8 shrink-0 items-center justify-center rounded-full bg-primary-100 text-primary dark:bg-primary-900">3</span>
          <span>Le poste officiel est cree automatiquement a l'acceptation.</span>
        </div>
      </div>
    </section>

    <section class="flex items-center justify-center p-6">
      <div class="w-full max-w-md rounded-xl border border-surface bg-surface-0 p-8 shadow-sm dark:bg-surface-900">
        <h2 class="mb-2 text-2xl font-semibold">{{ candidat ? 'Connexion candidat' : 'Connexion' }}</h2>
        <p class="mb-6 text-sm text-muted-color">{{ candidat ? 'Connectez-vous pour postuler à une offre.' : 'Accedez a votre espace de travail.' }}</p>

        <div v-if="erreur" class="rounded-lg border p-3 text-sm border-red-200 bg-red-50 text-red-700">{{ erreur }}</div>

        <form @submit.prevent="seConnecter">
          <div class="flex flex-col gap-2 mb-4">
            <label class="text-sm font-semibold text-muted-color" for="email">Adresse e-mail</label>
            <PrimeInputText
              id="email"
              v-model="email"
              class="w-full"
              type="email"
              autocomplete="username"
              placeholder="prenom.nom@tovo.mg"
              required
            />
          </div>

          <div class="flex flex-col gap-2 mb-4">
            <label class="text-sm font-semibold text-muted-color" for="motDePasse">Mot de passe</label>
            <PrimeInputText
              id="motDePasse"
              v-model="motDePasse"
              class="w-full"
              type="password"
              autocomplete="current-password"
              required
            />
          </div>

          <PrimeButton type="submit" class="inline-flex items-center justify-center gap-2 bg-primary text-primary-contrast w-full" :disabled="envoi">
            {{ envoi ? 'Connexion...' : 'Se connecter' }}
          </PrimeButton>
        </form>

        <RouterLink v-if="candidat" class="mt-4 block text-center text-primary" :to="{ name: 'inscription-candidat' }">
          Créer un compte candidat
        </RouterLink>
        <div v-if="!candidat" class="comptes-demo">
          <div class="mb-2">Comptes de démonstration :</div>
          <div v-for="compte in COMPTES" :key="compte.email" class="grid gap-2 sm:grid-cols-2">
            <span>{{ compte.role }}</span>
            <PrimeButton class="text-left text-sm" @click="remplir(compte)">
              {{ compte.email }}
            </PrimeButton>
          </div>
        </div>

        <div class="mt-6 border-t border-surface pt-4 dark:border-surface-700">
          <PrimeButton
            label="Accéder à l’espace candidat"
            icon="pi pi-user"
            severity="secondary"
            variant="outlined"
            class="w-full"
            as="router-link"
            :to="{ name: 'accueil-candidat' }"
          />
        </div>
      </div>
    </section>
  </div>
</template>
