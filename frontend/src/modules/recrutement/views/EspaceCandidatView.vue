<script setup>
import PrimeButton from 'primevue/button'
import Tag from 'primevue/tag'
import { onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { useAuthStore } from '@/shared/store/useAuthStore'
import { recrutementApi as api, ErreurApi } from '@/modules/recrutement/api/recrutementApi'

const auth = useAuthStore()
const router = useRouter()
const offres = ref([])
const chargement = ref(false)
const erreur = ref('')

async function chargerOffres() {
  chargement.value = true
  erreur.value = ''
  try {
    offres.value = await api.get('/recrutement/offres?statut=PUBLIEE')
  } catch (cause) {
    erreur.value = cause instanceof ErreurApi ? cause.message : 'Les offres sont momentanement indisponibles.'
  } finally {
    chargement.value = false
  }
}

function seDeconnecter() {
  auth.deconnexion()
  router.push({ name: 'accueil-candidat' })
}

onMounted(chargerOffres)
</script>

<template>
  <main class="min-h-screen bg-surface-50 p-4 text-color md:p-8 dark:bg-surface-950">
    <header class="mx-auto mb-8 flex w-full max-w-6xl flex-col gap-5 rounded-xl border border-surface bg-surface-0 p-5 shadow-sm dark:bg-surface-900 md:flex-row md:items-center md:justify-between">
      <div>
        <p class="text-xs font-semibold uppercase tracking-wider text-primary">ERP TOVO · ESPACE CANDIDAT</p>
        <h1 class="mt-1 text-2xl font-bold">Bonjour, {{ auth.profil?.nom }}</h1>
        <p class="text-sm text-muted-color">{{ auth.profil?.email }}</p>
      </div>
      <div class="flex flex-wrap items-center gap-3">
        <Tag value="Candidat connecté" severity="success" />
        <PrimeButton label="Se déconnecter" icon="pi pi-sign-out" severity="secondary" outlined @click="seDeconnecter" />
      </div>
    </header>

    <section class="mx-auto grid w-full max-w-6xl gap-5 lg:grid-cols-[minmax(0,2fr)_minmax(280px,1fr)]">
      <div class="rounded-xl border border-surface bg-surface-0 p-6 shadow-sm dark:bg-surface-900">
        <div class="mb-5 flex flex-wrap items-end justify-between gap-3">
          <div>
            <p class="text-xs font-semibold uppercase tracking-wider text-primary">Opportunités</p>
            <h2 class="mt-1 text-2xl font-bold">Offres d’emploi ouvertes</h2>
          </div>
          <PrimeButton label="Actualiser" icon="pi pi-refresh" outlined severity="secondary" @click="chargerOffres" />
        </div>
        <p v-if="erreur" class="mb-4 text-sm text-red-600" role="alert">{{ erreur }}</p>
        <p v-if="chargement" class="text-muted-color">Chargement des offres...</p>
        <div v-else class="grid gap-4 md:grid-cols-2">
          <article v-for="offre in offres" :key="offre.idOffre" class="flex flex-col rounded-lg border border-surface p-4">
            <span class="text-xs font-semibold uppercase tracking-wide text-muted-color">{{ offre.reference }}</span>
            <h3 class="my-2 text-lg font-bold">{{ offre.titre }}</h3>
            <p class="text-sm text-muted-color">{{ offre.lieuTravail || 'Lieu à définir' }} · {{ offre.typeContrat || 'Contrat à définir' }}</p>
            <p class="mt-3 flex-1 text-sm text-muted-color">{{ offre.description }}</p>
            <RouterLink class="mt-4 inline-flex justify-center rounded-md bg-primary px-4 py-2 font-semibold text-primary-contrast" :to="{ name: 'accueil-candidat', query: { offre: offre.idOffre } }">
              Postuler
            </RouterLink>
          </article>
          <p v-if="!offres.length" class="py-8 text-center text-muted-color md:col-span-2">Aucune offre ouverte pour le moment.</p>
        </div>
      </div>

      <aside class="rounded-xl border border-surface bg-surface-0 p-6 shadow-sm dark:bg-surface-900">
        <p class="text-xs font-semibold uppercase tracking-wider text-primary">Suivi</p>
        <h2 class="mt-1 text-2xl font-bold">Notifications</h2>
        <div class="mt-5 rounded-lg border border-dashed border-surface-300 p-5 text-center dark:border-surface-700">
          <i class="pi pi-bell text-2xl text-muted-color" aria-hidden="true"></i>
          <p class="mt-3 font-semibold">Bientôt disponible</p>
          <p class="mt-1 text-sm text-muted-color">Les notifications liées à vos candidatures apparaîtront ici.</p>
          <RouterLink class="mt-4 inline-flex rounded-md bg-primary px-4 py-2 text-sm font-semibold text-primary-contrast no-underline" :to="{ name: 'candidat-proposition-liste' }">
            Voir mes propositions d’embauche
          </RouterLink>
        </div>
      </aside>
    </section>
  </main>
</template>
