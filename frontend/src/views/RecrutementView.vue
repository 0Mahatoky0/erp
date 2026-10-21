<script setup>
import { computed, onMounted, ref } from 'vue'

const api = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/recrutement'
const offres = ref([])
const candidatures = ref([])
const selectedOffer = ref(null)
const loading = ref(false)
const error = ref('')

async function request(path, options) {
  const response = await fetch(`${api}${path}`, {
    headers: { 'Content-Type': 'application/json', ...(options?.headers || {}) },
    ...options,
  })
  if (!response.ok) throw new Error((await response.json()).error || 'La requête a échoué')
  return response.json()
}

async function loadOffers() {
  loading.value = true
  error.value = ''
  try {
    offres.value = await request('/offres')
  } catch (cause) {
    error.value = cause.message
  } finally {
    loading.value = false
  }
}

async function selectOffer(offer) {
  selectedOffer.value = offer
  candidatures.value = await request(`/offres/${offer.idOffre}/candidatures`)
}

const publishedOffers = computed(() => offres.value.filter((offer) => offer.statut === 'PUBLIEE'))
onMounted(loadOffers)
</script>

<template>
  <main class="recruitment">
    <header class="page-header">
      <div>
        <p class="eyebrow">Bloc 2 · Recrutement</p>
        <h1>Offres et candidatures</h1>
        <p>Suivez la publication des offres et la présélection des candidats.</p>
      </div>
      <button type="button" @click="loadOffers">Actualiser</button>
    </header>

    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <p v-if="loading">Chargement des offres...</p>
    <section v-else class="content-grid">
      <article class="panel">
        <h2>Offres publiées ({{ publishedOffers.length }})</h2>
        <button
          v-for="offer in publishedOffers"
          :key="offer.idOffre"
          class="offer"
          :class="{ selected: selectedOffer?.idOffre === offer.idOffre }"
          type="button"
          @click="selectOffer(offer)"
        >
          <strong>{{ offer.titre }}</strong>
          <span>{{ offer.reference }} · {{ offer.lieuTravail || 'Lieu à définir' }}</span>
        </button>
        <p v-if="!publishedOffers.length">Aucune offre publiée.</p>
      </article>

      <article class="panel">
        <h2>
          Candidatures
          <span v-if="selectedOffer">· {{ selectedOffer.titre }}</span>
        </h2>
        <div v-for="application in candidatures" :key="application.idCandidature" class="application">
          <strong>{{ application.candidat.prenom }} {{ application.candidat.nom }}</strong>
          <span>{{ application.candidat.email }}</span>
          <em>{{ application.statut }}</em>
        </div>
        <p v-if="selectedOffer && !candidatures.length">Aucune candidature pour cette offre.</p>
        <p v-if="!selectedOffer">Sélectionnez une offre pour consulter ses candidatures.</p>
      </article>
    </section>
  </main>
</template>

<style scoped>
.recruitment { max-width: 1100px; margin: 0 auto; padding: 3rem 1.5rem; }
.page-header { display: flex; justify-content: space-between; align-items: end; gap: 2rem; margin-bottom: 2rem; }
.eyebrow { color: #6366f1; font-weight: 700; text-transform: uppercase; letter-spacing: .08em; }
h1, h2 { margin: 0 0 .5rem; }
button { border: 0; border-radius: .6rem; padding: .7rem 1rem; cursor: pointer; background: #4f46e5; color: white; }
.content-grid { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1.4fr); gap: 1.25rem; }
.panel { border: 1px solid #e5e7eb; border-radius: 1rem; padding: 1.25rem; background: white; }
.offer, .application { display: flex; flex-direction: column; align-items: flex-start; width: 100%; margin-top: .75rem; }
.offer { background: #f8fafc; color: #111827; border: 1px solid #e5e7eb; text-align: left; }
.offer.selected { border-color: #4f46e5; }
.offer span, .application span { color: #64748b; font-size: .9rem; }
.application { border-bottom: 1px solid #e5e7eb; padding-bottom: .75rem; }
.application em { color: #4f46e5; font-style: normal; font-size: .85rem; }
.error { color: #b91c1c; }
@media (max-width: 760px) { .page-header, .content-grid { display: block; } .page-header button { margin-top: 1rem; } .panel + .panel { margin-top: 1rem; } }
</style>
