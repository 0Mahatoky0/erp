<script setup>
import PrimeSelect from 'primevue/select'
import PrimeButton from 'primevue/button'
import { computed, onMounted, ref } from 'vue'
import { useAuthStore } from '@/shared/store/useAuthStore'
import { recrutementApi as api, ErreurApi } from '@/modules/recrutement/api/recrutementApi'

const auth = useAuthStore()
const offres = ref([])
const candidatures = ref([])
const offreId = ref('')
const erreur = ref('')
const gestionnaire = computed(() => auth.estRh || auth.estDirection)

async function chargerOffres() {
  offres.value = await api.get('/recrutement/offres?statut=PUBLIEE')
}
async function chargerCandidatures() {
  if (!offreId.value) return
  try {
    candidatures.value = await api.get(`/recrutement/offres/${offreId.value}/candidatures`)
  } catch (cause) {
    erreur.value = cause instanceof ErreurApi ? cause.message : 'Impossible de charger les candidatures.'
  }
}
async function changerStatut(candidature, statut) {
  try {
    await api.patch(`/recrutement/candidatures/${candidature.idCandidature}/statut`, {
      statut,
      commentaire: `Statut modifie par ${auth.profil?.nom || 'le recruteur'}`,
    })
    await chargerCandidatures()
  } catch (cause) {
    erreur.value = cause instanceof ErreurApi ? cause.message : 'Impossible de modifier le statut.'
  }
}
onMounted(chargerOffres)
</script>

<template>
  <section class="mx-auto w-full max-w-5xl p-4 md:p-6">
    <p class="mb-1 text-xs font-semibold uppercase tracking-wider text-primary">Recrutement · Etape 3</p>
    <h1>Candidatures</h1>
    <p>Consultez les candidatures recues pour chaque offre publiee.</p>
    <label>Offre
      <PrimeSelect
        v-model="offreId"
        :options="[{ idOffre: '', reference: 'Selectionnez une offre', titre: '' }, ...offres]"
        option-label="reference"
        option-value="idOffre"
        @change="chargerCandidatures"
      />
    </label>
    <p v-if="erreur" class="text-sm text-red-600">{{ erreur }}</p>
    <div class="flex flex-col gap-3">
      <article v-for="candidature in candidatures" :key="candidature.idCandidature" class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
        <strong>{{ candidature.candidat.prenom }} {{ candidature.candidat.nom }}</strong>
        <span>{{ candidature.candidat.email }}</span>
        <b>{{ candidature.statut }}</b>
        <RouterLink :to="{ name: 'recrutement-candidature-detail', params: { id: candidature.idCandidature } }">Voir le parcours</RouterLink>
        <div v-if="gestionnaire" class="flex flex-wrap gap-2">
          <PrimeButton @click="changerStatut(candidature, 'PRESELECTIONNEE')">Présélectionner</PrimeButton>
          <PrimeButton @click="changerStatut(candidature, 'REFUSEE')">Refuser</PrimeButton>
        </div>
      </article>
      <p v-if="offreId && !candidatures.length">Aucune candidature pour cette offre.</p>
    </div>
  </section>
</template>
