<script setup>
import { onMounted, ref } from 'vue'
import { api, ErreurApi } from '@/api/client'

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
  <section class="recrutement-page">
    <p class="eyebrow">Recrutement · Etape 5</p>
    <h1>Suivi des candidats</h1>
    <p>Conservez la trace des tests, entretiens, évaluations et décisions finales.</p>
    <select v-model="offreId" @change="afficher">
      <option value="">Selectionnez une offre</option>
      <option v-for="offre in offres" :key="offre.idOffre" :value="offre.idOffre">{{ offre.reference }} · {{ offre.titre }}</option>
    </select>
    <p v-if="erreur" class="error">{{ erreur }}</p>
    <div class="list">
      <article v-for="candidature in candidatures" :key="candidature.idCandidature" class="card">
        <strong>{{ candidature.candidat.prenom }} {{ candidature.candidat.nom }}</strong>
        <span>{{ candidature.statut }}</span>
        <RouterLink :to="{ name: 'recrutement-selection', query: { candidature: candidature.idCandidature } }">Voir le parcours</RouterLink>
      </article>
    </div>
  </section>
</template>



<style scoped>
.recrutement-page { max-width: 1000px; margin: 0 auto; padding: 2rem 1.5rem; } .eyebrow { color: #4f46e5; font-weight: 700; text-transform: uppercase; } h1 { margin: 0 0 .5rem; } select { padding: .7rem; border: 1px solid #cbd5e1; border-radius: .5rem; margin: 1.5rem 0; min-width: 320px; } .list { display: grid; gap: .8rem; } .card { display: flex; gap: 1rem; align-items: center; padding: 1rem; border: 1px solid #e5e7eb; border-radius: .8rem; background: white; } .card span { color: #4f46e5; margin-left: auto; } .card a { color: #4f46e5; } .error { color: #b91c1c; }
</style>
