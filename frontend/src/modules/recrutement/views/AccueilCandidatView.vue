<script setup>
import PrimeButton from 'primevue/button'
import PrimeInputText from 'primevue/inputtext'
import PrimeTextarea from 'primevue/textarea'
import Message from 'primevue/message'
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { recrutementApi as api, ErreurApi } from '@/modules/recrutement/api/recrutementApi'

const offres = ref([])
const offreSelectionnee = ref(null)
const chargement = ref(false)
const envoi = ref(false)
const message = ref('')
const erreur = ref('')
const formulaire = ref({
  nom: '',
  prenom: '',
  email: '',
  telephone: '',
  adresse: '',
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
      candidat: {
        nom: formulaire.value.nom,
        prenom: formulaire.value.prenom,
        email: formulaire.value.email,
        telephone: formulaire.value.telephone || null,
        adresse: formulaire.value.adresse || null,
      },
      messageCandidat: formulaire.value.messageCandidat || null,
      source: 'SITE_CARRIERE',
    })
    message.value = 'Votre candidature a bien ete envoyee aux equipes de recrutement.'
    formulaire.value = { nom: '', prenom: '', email: '', telephone: '', adresse: '', messageCandidat: '' }
  } catch (cause) {
    erreur.value = cause instanceof ErreurApi ? cause.message : 'Impossible d’envoyer votre candidature.'
  } finally {
    envoi.value = false
  }
}

onMounted(chargerOffres)
</script>

<template>
  <main class="min-h-screen bg-surface-50 p-4 text-color md:p-8 dark:bg-surface-950">
    <header class="mx-auto mb-10 flex max-w-6xl flex-col items-start justify-between gap-8 py-8 md:flex-row md:items-end">
      <div>
        <p class="text-xs font-semibold uppercase tracking-wider text-primary">ERP TOVO · ESPACE CANDIDAT</p>
        <h1>Trouvez le poste qui vous correspond.</h1>
        <p class="max-w-2xl">Consultez nos opportunites et envoyez votre candidature sans creer de compte.</p>
      </div>
      <RouterLink class="admin-link" :to="{ name: 'grid min-h-screen grid-cols-1 bg-surface-50 lg:grid-cols-2 dark:bg-surface-950' }">Connexion administrateur</RouterLink>
    </header>

    <section v-if="offreSelectionnee" class="rounded-xl border border-surface bg-surface-0 p-6 shadow-sm dark:bg-surface-900">
      <div class="section-heading">
        <div>
          <p class="text-xs font-semibold uppercase tracking-wider text-primary">Candidature</p>
          <h2>{{ offreSelectionnee.titre }}</h2>
          <p>{{ offreSelectionnee.reference }} · {{ offreSelectionnee.lieuTravail || 'Lieu a definir' }}</p>
        </div>
        <PrimeButton class="button border border-surface text-secondary-color" @click="fermerCandidature">Retour aux offres</PrimeButton>
      </div>
      <Message v-if="message" severity="success" :closable="false" role="status">{{ message }}</Message>
      <p v-if="erreur" class="text-sm text-red-600" role="alert">{{ erreur }}</p>
      <form class="form-grid" @submit.prevent="envoyerCandidature">
        <label>Nom<PrimeInputText v-model="formulaire.nom" required maxlength="100" autocomplete="family-name" /></label>
        <label>Prénom<PrimeInputText v-model="formulaire.prenom" required maxlength="100" autocomplete="given-name" /></label>
        <label>E-mail<PrimeInputText v-model="formulaire.email" required type="email" maxlength="255" autocomplete="email" /></label>
        <label>Téléphone<PrimeInputText v-model="formulaire.telephone" maxlength="30" autocomplete="tel" /></label>
        <label class="full">Adresse<PrimeInputText v-model="formulaire.adresse" autocomplete="street-address" /></label>
        <label class="full">Message de motivation<PrimeTextarea v-model="formulaire.messageCandidat" rows="5" /></label>
        <div class="flex w-full flex-wrap gap-2">
          <PrimeButton type="submit" class="button" :disabled="envoi">{{ envoi ? 'Envoi...' : 'Envoyer ma candidature' }}</PrimeButton>
        </div>
      </form>
    </section>

    <section v-else class="offres">
      <div class="section-heading">
        <div>
          <p class="text-xs font-semibold uppercase tracking-wider text-primary">Opportunites ouvertes</p>
          <h2>Nos offres d’emploi</h2>
        </div>
        <PrimeButton class="button border border-surface text-secondary-color" @click="chargerOffres">Actualiser</PrimeButton>
      </div>
      <p v-if="erreur" class="text-sm text-red-600" role="alert">{{ erreur }}</p>
      <p v-if="chargement">Chargement des offres...</p>
      <div v-else class="offres-grid">
        <article v-for="offre in offres" :key="offre.idOffre" class="rounded-xl border border-surface bg-surface-0 p-5 shadow-sm transition-shadow hover:shadow-md dark:bg-surface-900">
          <div class="text-xs font-semibold uppercase tracking-wide text-muted-color">{{ offre.reference }}</div>
          <h3>{{ offre.titre }}</h3>
          <p class="flex flex-wrap gap-3 text-sm text-muted-color">{{ offre.lieuTravail || 'Lieu a definir' }} · {{ offre.typeContrat || 'Contrat a definir' }}</p>
          <p>{{ offre.description }}</p>
          <div class="mt-4 flex items-center justify-between gap-3 border-t border-surface pt-4">
            <span>Profil : {{ offre.profilRecherche }}</span>
            <PrimeButton class="button" @click="ouvrirCandidature(offre)">Postuler</PrimeButton>
          </div>
        </article>
        <div v-if="!offres.length" class="empty">
          <h3>Aucune offre ouverte pour le moment</h3>
          <p>Revenez bientôt pour découvrir nos nouvelles opportunités.</p>
        </div>
      </div>
    </section>

    <footer class="footer">
      <span>Vous êtes membre de l’équipe ?</span>
      <RouterLink :to="{ name: 'grid min-h-screen grid-cols-1 bg-surface-50 lg:grid-cols-2 dark:bg-surface-950' }">Accéder à l’espace administrateur</RouterLink>
    </footer>
  </main>
</template>
