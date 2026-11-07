<script setup>
import PrimeButton from 'primevue/button'
import PrimeTextarea from 'primevue/textarea'
import Message from 'primevue/message'
import { onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/shared/store/useAuthStore'
import { recrutementApi as api, ErreurApi } from '@/modules/recrutement/api/recrutementApi'

const offres = ref([])
const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const offreSelectionnee = ref(null)
const chargement = ref(false)
const envoi = ref(false)
const message = ref('')
const erreur = ref('')
const formulaire = ref({
  messageCandidat: '',
})

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

function ouvrirCandidature(offre) {
  if (!auth.connecte || auth.role !== 'CANDIDAT') {
    router.push({ name: 'connexion-candidat', query: { suite: `/?offre=${offre.idOffre}` } })
    return
  }
  offreSelectionnee.value = offre
  message.value = ''
  erreur.value = ''
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function fermerCandidature() {
  offreSelectionnee.value = null
  message.value = ''
  erreur.value = ''
}

async function envoyerCandidature() {
  envoi.value = true
  message.value = ''
  erreur.value = ''
  try {
    await api.post('/recrutement/candidatures', {
      idOffre: offreSelectionnee.value.idOffre,
      messageCandidat: formulaire.value.messageCandidat || null,
      source: 'SITE_CARRIERE',
    })
    message.value = 'Votre candidature a bien ete envoyee aux equipes de recrutement.'
    formulaire.value = { messageCandidat: '' }
  } catch (cause) {
    erreur.value = cause instanceof ErreurApi ? cause.message : 'Impossible d’envoyer votre candidature.'
  } finally {
    envoi.value = false
  }
}

onMounted(async () => {
  await chargerOffres()
  const idOffre = Number(route.query.offre)
  const offre = offres.value.find((item) => item.idOffre === idOffre)
  if (offre && auth.role === 'CANDIDAT') ouvrirCandidature(offre)
})
</script>

<template>
  <main class="min-h-screen bg-surface-50 p-4 text-color md:p-8 dark:bg-surface-950">
    <header class="mx-auto mb-10 flex w-full max-w-6xl flex-col items-start justify-between gap-8 border-b border-surface py-8 md:flex-row md:items-end">
      <div>
        <p class="text-xs font-semibold uppercase tracking-wider text-primary">ERP TOVO · ESPACE CANDIDAT</p>
        <h1 class="my-2 max-w-2xl text-4xl font-bold tracking-tight md:text-5xl">Trouvez le poste qui vous correspond.</h1>
        <p class="max-w-2xl text-base text-muted-color">Consultez nos opportunites et postulez avec votre compte candidat.</p>
      </div>
      <div class="flex flex-wrap gap-2">
        <RouterLink class="shrink-0 rounded-md border border-surface bg-surface-0 px-4 py-2 font-semibold text-primary transition-colors hover:bg-surface-100" :to="{ name: 'connexion-candidat' }">Se connecter en tant que candidat</RouterLink>
        <RouterLink class="shrink-0 rounded-md border border-surface bg-surface-0 px-4 py-2 font-semibold text-primary transition-colors hover:bg-surface-100" :to="{ name: 'connexion' }">Espace administrateur</RouterLink>
      </div>
      <div class="flex flex-wrap items-center gap-2">
        <RouterLink class="shrink-0 rounded-md border border-surface bg-surface-0 px-4 py-2 font-semibold text-primary transition-colors hover:bg-surface-100" :to="{ name: 'candidature-suivi-public' }">Suivre ma candidature</RouterLink>
      </div>
    </header>

    <section v-if="offreSelectionnee" class="mx-auto w-full max-w-6xl rounded-xl border border-surface bg-surface-0 p-6 shadow-sm dark:bg-surface-900">
      <div class="mb-5 flex flex-col items-start justify-between gap-4 md:flex-row md:items-end">
        <div>
          <p class="text-xs font-semibold uppercase tracking-wider text-primary">Candidature</p>
          <h2 class="mt-1 text-2xl font-bold">{{ offreSelectionnee.titre }}</h2>
          <p class="text-muted-color">{{ offreSelectionnee.reference }} · {{ offreSelectionnee.lieuTravail || 'Lieu a definir' }}</p>
        </div>
        <PrimeButton label="Retour aux offres" outlined severity="secondary" @click="fermerCandidature" />
      </div>
      <Message v-if="message" severity="success" :closable="false" role="status">{{ message }}</Message>
      <p v-if="erreur" class="text-sm text-red-600" role="alert">{{ erreur }}</p>
      <form class="mt-6 grid grid-cols-1 gap-4 md:grid-cols-2" @submit.prevent="envoyerCandidature">
        <label class="flex flex-col gap-2 font-semibold md:col-span-2">Message de motivation<PrimeTextarea v-model="formulaire.messageCandidat" rows="5" /></label>
        <div class="flex w-full flex-wrap gap-2">
          <PrimeButton type="submit" :label="envoi ? 'Envoi...' : 'Envoyer ma candidature'" :disabled="envoi" />
        </div>
      </form>
    </section>

    <section v-else class="mx-auto w-full max-w-6xl">
      <div class="mb-5 flex flex-col items-start justify-between gap-4 sm:flex-row sm:items-end">
        <div>
          <p class="text-xs font-semibold uppercase tracking-wider text-primary">Opportunites ouvertes</p>
          <h2 class="mt-1 text-2xl font-bold">Nos offres d’emploi</h2>
        </div>
        <PrimeButton label="Actualiser" icon="pi pi-refresh" outlined severity="secondary" @click="chargerOffres" />
      </div>
      <p v-if="erreur" class="text-sm text-red-600" role="alert">{{ erreur }}</p>
      <p v-if="chargement">Chargement des offres...</p>
      <div v-else class="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
        <article v-for="offre in offres" :key="offre.idOffre" class="flex min-h-full flex-col rounded-xl border border-surface bg-surface-0 p-5 shadow-sm transition-shadow hover:shadow-md dark:bg-surface-900">
          <div class="text-xs font-semibold uppercase tracking-wide text-muted-color">{{ offre.reference }}</div>
          <h3 class="my-2 text-lg font-bold">{{ offre.titre }}</h3>
          <p class="flex flex-wrap gap-3 text-sm text-muted-color">{{ offre.lieuTravail || 'Lieu a definir' }} · {{ offre.typeContrat || 'Contrat a definir' }}</p>
          <p class="flex-1 text-muted-color">{{ offre.description }}</p>
          <div class="mt-4 flex items-center justify-between gap-3 border-t border-surface pt-4">
            <span class="text-sm text-muted-color">Profil : {{ offre.profilRecherche }}</span>
            <PrimeButton label="Postuler" @click="ouvrirCandidature(offre)" />
          </div>
        </article>
        <div v-if="!offres.length" class="col-span-full py-12 text-center text-muted-color">
          <h3>Aucune offre ouverte pour le moment</h3>
          <p>Revenez bientôt pour découvrir nos nouvelles opportunités.</p>
        </div>
      </div>
    </section>

    <footer class="mx-auto flex w-full max-w-6xl justify-center gap-1 px-4 py-8 text-sm text-muted-color">
      <span>Vous êtes membre de l’équipe ?</span>
      <RouterLink :to="{ name: 'connexion' }">Accéder à l’espace administrateur</RouterLink>
    </footer>
  </main>
</template>
