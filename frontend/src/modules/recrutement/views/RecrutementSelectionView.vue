<script setup>
import PrimeSelect from 'primevue/select'
import { onMounted, ref } from 'vue'
import { recrutementApi as api, ErreurApi } from '@/modules/recrutement/api/recrutementApi'

const offres = ref([])
const offreId = ref('')
const candidatures = ref([])
const erreur = ref('')

async function charger() {
  offres.value = await api.get('/recrutement/offres')
}
async function afficher() {
  if (!offreId.value) return
  try {
    const toutes = await api.get(`/recrutement/offres/${offreId.value}/candidatures`)
    candidatures.value = toutes.filter((candidature) => candidature.statut === 'PRESELECTIONNEE')
  } catch (cause) {
    erreur.value = cause instanceof ErreurApi ? cause.message : 'Impossible de charger la présélection.'
  }
}
onMounted(charger)
</script>

<template>
  <section class="mx-auto w-full max-w-5xl p-4 md:p-6">
    <p class="mb-1 text-xs font-semibold uppercase tracking-wider text-primary">Recrutement · Etape 4</p>
    <h1>Présélection et étapes suivantes</h1>
    <p>Les candidats présélectionnés sont prêts pour un entretien ou un test selon le mode de sélection.</p>
    <PrimeSelect
      v-model="offreId"
      :options="[{ idOffre: '', reference: 'Selectionnez une offre', titre: '' }, ...offres]"
      option-label="reference"
      option-value="idOffre"
      @change="afficher"
    />
    <p v-if="erreur" class="text-sm text-red-600">{{ erreur }}</p>
    <div class="flex flex-col gap-3">
      <article v-for="candidature in candidatures" :key="candidature.idCandidature" class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
        <strong>{{ candidature.candidat.prenom }} {{ candidature.candidat.nom }}</strong>
        <span>{{ candidature.candidat.email }}</span>
        <b>PRESELECTIONNEE</b>
        <RouterLink :to="{ name: 'recrutement-candidature-detail', params: { id: candidature.idCandidature } }">Ouvrir le parcours</RouterLink>
      </article>
      <p v-if="offreId && !candidatures.length">Aucun candidat présélectionné.</p>
    </div>
  </section>
</template>
