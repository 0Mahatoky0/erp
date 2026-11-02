<script setup>
import PrimeButton from 'primevue/button'
import PrimeInputText from 'primevue/inputtext'
import PrimeTextarea from 'primevue/textarea'
import { computed, onMounted, ref } from 'vue'
import { recrutementApi as api, ErreurApi } from '@/modules/recrutement/api/recrutementApi'
import { useAuthStore } from '@/shared/store/useAuthStore'
import { useRoute } from 'vue-router'
const auth = useAuthStore()
const route = useRoute()
const offres = ref([])
const candidatures = ref([])
const selectedOffer = ref(null)
const loading = ref(false)
const error = ref('')
const editing = ref(false)
const saving = ref(false)
const form = ref(emptyOffer())
const peutGerer = computed(() => auth.estRh || auth.estDirection)

function emptyOffer() {
  return {
    idDemandePoste: '',
    reference: '',
    titre: '',
    description: '',
    profilRecherche: '',
    lieuTravail: '',
    typeContrat: '',
    effectifRecherche: 1,
    salaireMin: null,
    salaireMax: null,
    modeSelection: '',
    dateLimiteCandidature: null,
  }
}

async function loadOffers() {
  loading.value = true
  error.value = ''
  try {
    offres.value = await api.get('/recrutement/offres')
  } catch (cause) {
    error.value = cause instanceof ErreurApi ? cause.message : 'Impossible de charger les offres.'
  } finally {
    loading.value = false
  }
}

async function selectOffer(offer) {
  selectedOffer.value = offer
  error.value = ''
  try {
    candidatures.value = await api.get(`/recrutement/offres/${offer.idOffre}/candidatures`)
  } catch (cause) {
    error.value =
      cause instanceof ErreurApi ? cause.message : 'Impossible de charger les candidatures.'
    candidatures.value = []
  }
}

function startCreate() {
  selectedOffer.value = null
  form.value = emptyOffer()
  editing.value = true
  error.value = ''
  if (route.query.idDemandePoste) form.value.idDemandePoste = route.query.idDemandePoste
}

function startEdit(offer) {
  form.value = { ...offer }
  editing.value = true
  error.value = ''
}

function cancelEdit() {
  editing.value = false
}

async function saveOffer() {
  saving.value = true
  error.value = ''
  try {
    const payload = { ...form.value, idDemandePoste: Number(form.value.idDemandePoste) }
    const saved = form.value.idOffre
      ? await api.put(`/recrutement/offres/${form.value.idOffre}`, payload)
      : await api.post('/recrutement/offres', payload)
    editing.value = false
    await loadOffers()
    selectedOffer.value = saved
  } catch (cause) {
    error.value = cause instanceof ErreurApi ? cause.message : 'Impossible d’enregistrer l’offre.'
  } finally {
    saving.value = false
  }
}

async function deleteOffer(offer) {
  if (!window.confirm(`Supprimer l’offre ${offer.reference} ?`)) return
  try {
    await api.delete(`/recrutement/offres/${offer.idOffre}`)
    selectedOffer.value = null
    await loadOffers()
  } catch (cause) {
    error.value = cause instanceof ErreurApi ? cause.message : 'Impossible de supprimer l’offre.'
  }
}

async function changeOfferStatus(offer, action) {
  try {
    const updated = await api.post(
      `/recrutement/offres/${offer.idOffre}/${action}`,
    )
    await loadOffers()
    selectedOffer.value = updated
  } catch (cause) {
    error.value = cause instanceof ErreurApi ? cause.message : 'Impossible de changer le statut de l’offre.'
  }
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
      <div class="actions">
        <PrimeButton v-if="peutGerer" @click="startCreate">Nouvelle offre</PrimeButton>
        <PrimeButton @click="loadOffers">Actualiser</PrimeButton>
      </div>
    </header>

    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <form v-if="editing && peutGerer" class="panel offer-form" @submit.prevent="saveOffer">
      <h2>{{ form.idOffre ? 'Modifier l’offre brouillon' : 'Créer une offre' }}</h2>
      <div class="form-grid">
        <label>Demande validée (ID)<PrimeInputText v-model="form.idDemandePoste" required type="number" min="1" /></label>
        <label>Référence<PrimeInputText v-model="form.reference" required maxlength="50" /></label>
        <label>Titre<PrimeInputText v-model="form.titre" required maxlength="150" /></label>
        <label>Type de contrat<PrimeInputText v-model="form.typeContrat" maxlength="50" /></label>
        <label>Effectif recherché<PrimeInputText v-model="form.effectifRecherche" required type="number" min="1" /></label>
        <label>Salaire min<PrimeInputText v-model="form.salaireMin" type="number" min="0" step="0.01" /></label>
        <label>Salaire max<PrimeInputText v-model="form.salaireMax" type="number" min="0" step="0.01" /></label>
        <label>Mode de sélection<PrimeInputText v-model="form.modeSelection" required placeholder="CV, entretien, test" /></label>
        <label>Date limite<PrimeInputText v-model="form.dateLimiteCandidature" type="datetime-local" /></label>
        <label>Lieu<PrimeInputText v-model="form.lieuTravail" maxlength="255" /></label>
      </div>
      <label>Description<PrimeTextarea v-model="form.description" required /></label>
      <label>Profil recherché<PrimeTextarea v-model="form.profilRecherche" required /></label>
      <div class="actions"><PrimeButton type="submit" :disabled="saving">{{ saving ? 'Enregistrement...' : 'Enregistrer' }}</PrimeButton><PrimeButton type="button" @click="cancelEdit">Annuler</PrimeButton></div>
    </form>
    <p v-if="loading">Chargement des offres...</p>
    <section v-else class="content-grid">
      <article class="panel">
        <h2>Offres publiées ({{ publishedOffers.length }})</h2>
        <PrimeButton
          v-for="offer in publishedOffers"
          :key="offer.idOffre"
          class="offer"
          :class="{ selected: selectedOffer?.idOffre === offer.idOffre }"

          @click="selectOffer(offer)"
        >
          <strong>{{ offer.titre }}</strong>
          <span>{{ offer.reference }} · {{ offer.lieuTravail || 'Lieu à définir' }}</span>
        </PrimeButton>
        <p v-if="!publishedOffers.length">Aucune offre publiée.</p>
        <h3>Toutes les offres</h3>
        <div v-for="offer in offres" :key="`manage-${offer.idOffre}`" class="manage-offer">
          <span><strong>{{ offer.reference }}</strong> · {{ offer.statut }}</span>
          <PrimeButton v-if="peutGerer && offer.statut === 'BROUILLON'" @click="startEdit(offer)">Modifier</PrimeButton>
          <PrimeButton v-if="peutGerer && offer.statut === 'BROUILLON'" @click="changeOfferStatus(offer, 'publier')">Publier</PrimeButton>
          <PrimeButton v-if="peutGerer && offer.statut === 'PUBLIEE'" @click="changeOfferStatus(offer, 'cloturer')">Clôturer</PrimeButton>
          <PrimeButton v-if="peutGerer && !['CLOTUREE', 'ANNULEE'].includes(offer.statut)" @click="changeOfferStatus(offer, 'annuler')">Annuler</PrimeButton>
          <PrimeButton v-if="peutGerer && offer.statut === 'BROUILLON'" @click="deleteOffer(offer)">Supprimer</PrimeButton>
        </div>
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
.actions { display: flex; gap: .5rem; flex-wrap: wrap; }
.eyebrow { color: #6366f1; font-weight: 700; text-transform: uppercase; letter-spacing: .08em; }
h1, h2 { margin: 0 0 .5rem; }
button { border: 0; border-radius: .6rem; padding: .7rem 1rem; cursor: pointer; background: #4f46e5; color: white; }
.offer-form { margin-bottom: 1.25rem; }
.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 1rem; }
label { display: flex; flex-direction: column; gap: .35rem; margin-top: .75rem; font-weight: 600; }
input, textarea { border: 1px solid #cbd5e1; border-radius: .4rem; padding: .6rem; font: inherit; }
textarea { min-height: 6rem; resize: vertical; }
.content-grid { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1.4fr); gap: 1.25rem; }
.panel { border: 1px solid #e5e7eb; border-radius: 1rem; padding: 1.25rem; background: white; }
.offer, .application { display: flex; flex-direction: column; align-items: flex-start; width: 100%; margin-top: .75rem; }
.offer { background: #f8fafc; color: #111827; border: 1px solid #e5e7eb; text-align: left; }
.offer.selected { border-color: #4f46e5; }
.offer span, .application span { color: #64748b; font-size: .9rem; }
.application { border-bottom: 1px solid #e5e7eb; padding-bottom: .75rem; }
.application em { color: #4f46e5; font-style: normal; font-size: .85rem; }
.manage-offer { display: flex; gap: .4rem; align-items: center; flex-wrap: wrap; margin-top: .75rem; }
.manage-offer span { margin-right: auto; }
.manage-offer button { padding: .45rem .65rem; font-size: .8rem; }
.error { color: #b91c1c; }
@media (max-width: 760px) { .page-header, .content-grid, .form-grid { display: block; } .page-header button { margin-top: 1rem; } .panel + .panel { margin-top: 1rem; } }
</style>
