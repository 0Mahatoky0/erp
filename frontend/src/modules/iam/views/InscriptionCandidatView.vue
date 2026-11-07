<script setup>
import PrimeButton from 'primevue/button'
import PrimeInputText from 'primevue/inputtext'
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { useAuthStore } from '@/shared/store/useAuthStore'
import { ErreurApi } from '@/shared/api/axios'

const auth = useAuthStore()
const router = useRouter()
const formulaire = ref({ nom: '', prenom: '', email: '', telephone: '', adresse: '', motDePasse: '' })
const erreur = ref('')
const envoi = ref(false)

async function inscrire() {
  erreur.value = ''
  envoi.value = true
  try {
    await auth.inscriptionCandidat(formulaire.value)
    router.push({ name: 'espace-candidat' })
  } catch (cause) {
    erreur.value = cause instanceof ErreurApi ? cause.message : 'Impossible de créer le compte.'
  } finally {
    envoi.value = false
  }
}
</script>

<template>
  <main class="flex min-h-screen items-center justify-center bg-surface-50 p-6 dark:bg-surface-950">
    <section class="w-full max-w-md rounded-xl border border-surface bg-surface-0 p-8 shadow-sm dark:bg-surface-900">
      <h1 class="mb-2 text-2xl font-semibold">Créer un compte candidat</h1>
      <p class="mb-6 text-sm text-muted-color">Votre profil sera réutilisé pour toutes vos candidatures.</p>
      <p v-if="erreur" class="mb-4 text-sm text-red-600" role="alert">{{ erreur }}</p>
      <form class="grid gap-4" @submit.prevent="inscrire">
        <label class="grid gap-2">Nom<PrimeInputText v-model="formulaire.nom" required maxlength="100" /></label>
        <label class="grid gap-2">Prénom<PrimeInputText v-model="formulaire.prenom" required maxlength="100" /></label>
        <label class="grid gap-2">E-mail<PrimeInputText v-model="formulaire.email" required type="email" maxlength="255" /></label>
        <label class="grid gap-2">Téléphone<PrimeInputText v-model="formulaire.telephone" maxlength="30" /></label>
        <label class="grid gap-2">Adresse<PrimeInputText v-model="formulaire.adresse" /></label>
        <label class="grid gap-2">Mot de passe<PrimeInputText v-model="formulaire.motDePasse" required type="password" minlength="6" /></label>
        <PrimeButton type="submit" :label="envoi ? 'Création...' : 'Créer mon compte'" :disabled="envoi" />
      </form>
      <RouterLink class="mt-4 block text-center text-primary" :to="{ name: 'connexion-candidat' }">J’ai déjà un compte</RouterLink>
    </section>
  </main>
</template>
