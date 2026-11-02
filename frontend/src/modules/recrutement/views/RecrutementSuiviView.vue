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
    candidatures.value = await api.get(`/recrutement/offres/${offreId.value}/candidatures`)
  } catch (cause) {
    erreur.value = cause instanceof ErreurApi ? cause.message : 'Impossible de charger le suivi.'
  }
}
onMounted(charger)
</script>

<template>
  <section class="mx-auto w-full max-w-5xl p-4 md:p-6">
    <p class="mb-1 text-xs font-semibold uppercase tracking-wider text-primary">Recrutement · Etape 5</p>
    <h1>Suivi des candidats</h1>
    <p>Conservez la trace des tests, entretiens, évaluations et décisions finales.</p>
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
        <span>{{ candidature.statut }}</span>
        <RouterLink :to="{ name: 'recrutement-candidature-detail', params: { id: candidature.idCandidature } }">Voir le parcours</RouterLink>
      </article>
    </div>
  </section>
</template>
