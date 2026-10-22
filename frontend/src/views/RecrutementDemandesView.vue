<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { api, ErreurApi } from '@/api/client'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const lignes = ref([])
const chargement = ref(false)
const erreur = ref('')

async function charger() {
  chargement.value = true
  erreur.value = ''
  try {
    lignes.value = await api.get('/demandes/lignes/acceptees')
  } catch (cause) {
    erreur.value = cause instanceof ErreurApi ? cause.message : 'Impossible de charger les demandes acceptees.'
  } finally {
    chargement.value = false
  }
}

onMounted(charger)
</script>

<template>
  <section class="recrutement-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">Recrutement · Etape 1</p>
        <h1>Demandes acceptees</h1>
        <p>Les lignes validees par le circuit du bloc 1 peuvent devenir des offres d’emploi.</p>
      </div>
      <button type="button" @click="charger">Actualiser</button>
    </header>
    <p v-if="erreur" class="error">{{ erreur }}</p>
    <p v-if="chargement">Chargement...</p>
    <div v-else class="cards">
      <article v-for="ligne in lignes" :key="ligne.id" class="card">
        <div class="card__top">
          <strong>{{ ligne.intituleDemande }}</strong>
          <span class="badge">{{ ligne.statut }}</span>
        </div>
        <p>{{ ligne.service }} · {{ ligne.departement }}</p>
        <p>{{ ligne.profilRecherche || 'Profil non precise' }}</p>
        <small>Demande #{{ ligne.idDemande }} · Effectif : {{ ligne.effectif }}</small>
        <RouterLink v-if="auth.estRh || auth.estDirection" class="button" :to="{ name: 'recrutement-offres', query: { idDemandePoste: ligne.id } }">
          Creer une offre
        </RouterLink>
      </article>
      <p v-if="!lignes.length">Aucune demande acceptee disponible.</p>
    </div>
  </section>
</template>

<style scoped>
.recrutement-page { max-width: 1100px; margin: 0 auto; padding: 2rem 1.5rem; }
.page-header { display: flex; justify-content: space-between; gap: 1rem; align-items: end; margin-bottom: 1.5rem; }
.eyebrow { color: #4f46e5; font-weight: 700; text-transform: uppercase; letter-spacing: .08em; }
h1 { margin: 0 0 .5rem; } button, .button { border: 0; border-radius: .6rem; padding: .7rem 1rem; background: #4f46e5; color: white; cursor: pointer; text-decoration: none; }
.cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 1rem; }
.card { padding: 1.2rem; border: 1px solid #e5e7eb; border-radius: 1rem; background: white; }
.card__top { display: flex; justify-content: space-between; gap: .5rem; } .badge { color: #166534; font-size: .8rem; font-weight: 700; }
.card .button { display: inline-block; margin-top: 1rem; } .error { color: #b91c1c; }
@media (max-width: 700px) { .page-header { display: block; } .page-header button { margin-top: 1rem; } }
</style>
