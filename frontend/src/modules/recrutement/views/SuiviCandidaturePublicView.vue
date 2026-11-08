<script setup>
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import InputText from 'primevue/inputtext'
import Button from 'primevue/button'
import Card from 'primevue/card'
import Message from 'primevue/message'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import { recrutementApi as api, ErreurApi } from '../api/recrutementApi'
import { formatDateHeure } from '@/shared/utils/format'

const email = ref('')
const candidatures = ref([])
const chargement = ref(false)
const erreur = ref('')

async function chercher() {
  if (!email.value.trim()) {
    erreur.value = 'Saisissez votre email pour retrouver vos candidatures.'
    candidatures.value = []
    return
  }

  chargement.value = true
  erreur.value = ''
  try {
    candidatures.value = await api.get(`/recrutement/candidatures/public?email=${encodeURIComponent(email.value.trim())}`)
    if (!candidatures.value.length) {
      erreur.value = 'Aucune candidature trouvée pour cet email.'
    }
  } catch (cause) {
    erreur.value = cause instanceof ErreurApi ? cause.message : 'Impossible de retrouver votre candidature.'
    candidatures.value = []
  } finally {
    chargement.value = false
  }
}
</script>

<template>
  <main class="min-h-screen bg-surface-50 p-4 text-color md:p-8 dark:bg-surface-950">
    <div class="mx-auto max-w-4xl">
      <header class="mb-6 border-b border-surface pb-6">
        <div class="mb-4 flex items-center justify-between gap-3">
          <p class="text-xs font-semibold uppercase tracking-wider text-primary">ERP TOVO · ESPACE CANDIDAT</p>
          <RouterLink
            :to="{ name: 'accueil-candidat' }"
            class="inline-flex items-center gap-2 rounded-md border border-surface bg-surface-0 px-3 py-2 text-sm font-semibold text-primary transition-colors hover:bg-surface-100"
          >
            <span class="pi pi-arrow-left"></span>
            Retour
          </RouterLink>
        </div>
        <h1 class="mt-2 text-3xl font-bold">Suivi de ma candidature</h1>
        <p class="mt-2 text-muted-color">Saisissez l’email utilisé lors de votre candidature pour voir son statut et les étapes.</p>
      </header>

      <Card>
        <template #content>
          <div class="flex flex-col gap-3 md:flex-row md:items-end">
            <div class="flex-1">
              <label for="email-candidat" class="mb-2 block text-sm font-semibold text-muted-color">Email</label>
              <InputText id="email-candidat" v-model="email" type="email" class="w-full" placeholder="prenom.nom@example.com" @keydown.enter="chercher" />
            </div>
            <Button label="Rechercher" icon="pi pi-search" :loading="chargement" @click="chercher" />
          </div>

          <Message v-if="erreur" severity="warn" class="mt-4" :closable="false">{{ erreur }}</Message>
        </template>
      </Card>

      <div v-if="candidatures.length" class="mt-6 space-y-4">
        <article v-for="c in candidatures" :key="c.idCandidature" class="rounded-xl border border-surface bg-surface-0 p-5 shadow-sm dark:bg-surface-900">
          <div class="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
            <div>
              <p class="text-xs font-semibold uppercase tracking-wider text-primary">{{ c.offre?.reference || 'Offre' }}</p>
              <h2 class="mt-1 text-xl font-bold">{{ c.offre?.titre || 'Poste' }}</h2>
            </div>
            <StatusBadge :valeur="c.statut" />
          </div>

          <div class="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <div class="text-xs font-semibold uppercase text-muted-color">Candidat</div>
              <div>{{ c.candidat?.prenom }} {{ c.candidat?.nom }}</div>
            </div>
            <div>
              <div class="text-xs font-semibold uppercase text-muted-color">Date de candidature</div>
              <div>{{ formatDateHeure(c.dateCandidature) }}</div>
            </div>
          </div>

          <div class="mt-4 rounded-lg border border-surface bg-surface-100 p-3 text-sm text-muted-color dark:bg-surface-800">
            <div class="font-semibold text-color">Étape actuelle</div>
            <div class="mt-1">{{ c.statut }}</div>
          </div>
        </article>
      </div>
    </div>
  </main>
</template>
