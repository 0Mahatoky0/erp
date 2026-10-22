<script setup>
import { computed, onMounted, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { api, ErreurApi } from '@/api/client'

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
      statut, idUtilisateur: auth.profil?.id,
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
  <section class="recrutement-page">
    <p class="eyebrow">Recrutement · Etape 3</p>
    <h1>Candidatures</h1>
    <p>Consultez les candidatures recues pour chaque offre publiee.</p>
    <label>Offre
      <select v-model="offreId" @change="chargerCandidatures">
        <option value="">Selectionnez une offre</option>
        <option v-for="offre in offres" :key="offre.idOffre" :value="offre.idOffre">{{ offre.reference }} · {{ offre.titre }}</option>
      </select>
    </label>
    <p v-if="erreur" class="error">{{ erreur }}</p>
    <div class="list">
      <article v-for="candidature in candidatures" :key="candidature.idCandidature" class="card">
        <strong>{{ candidature.candidat.prenom }} {{ candidature.candidat.nom }}</strong>
        <span>{{ candidature.candidat.email }}</span>
        <b>{{ candidature.statut }}</b>
        <div v-if="gestionnaire" class="actions">
          <button type="button" @click="changerStatut(candidature, 'PRESELECTIONNEE')">Présélectionner</button>
          <button type="button" @click="changerStatut(candidature, 'REFUSEE')">Refuser</button>
        </div>
      </article>
      <p v-if="offreId && !candidatures.length">Aucune candidature pour cette offre.</p>
    </div>
  </section>
</template>

<style scoped>
.recrutement-page { max-width: 1000px; margin: 0 auto; padding: 2rem 1.5rem; } .eyebrow { color: #4f46e5; font-weight: 700; text-transform: uppercase; } h1 { margin: 0 0 .5rem; }
label { display: flex; flex-direction: column; gap: .4rem; max-width: 600px; margin: 1.5rem 0; font-weight: 700; } select { padding: .7rem; border: 1px solid #cbd5e1; border-radius: .5rem; }
.list { display: grid; gap: .8rem; } .card { display: flex; align-items: center; gap: 1rem; flex-wrap: wrap; padding: 1rem; border: 1px solid #e5e7eb; border-radius: .8rem; background: white; } .card span { color: #64748b; } .card b { color: #4f46e5; } .actions { display: flex; gap: .4rem; margin-left: auto; } button { border: 0; border-radius: .5rem; padding: .55rem .75rem; background: #4f46e5; color: white; cursor: pointer; } .error { color: #b91c1c; }
</style>
