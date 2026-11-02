<script setup>
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
  <main class="accueil">
    <header class="hero">
      <div>
        <p class="sur-titre">ERP TOVO · ESPACE CANDIDAT</p>
        <h1>Trouvez le poste qui vous correspond.</h1>
        <p class="hero__texte">Consultez nos opportunites et envoyez votre candidature sans creer de compte.</p>
      </div>
      <RouterLink class="admin-link" :to="{ name: 'connexion' }">Connexion administrateur</RouterLink>
    </header>

    <section v-if="offreSelectionnee" class="candidature-panel">
      <div class="section-heading">
        <div>
          <p class="sur-titre">Candidature</p>
          <h2>{{ offreSelectionnee.titre }}</h2>
          <p>{{ offreSelectionnee.reference }} · {{ offreSelectionnee.lieuTravail || 'Lieu a definir' }}</p>
        </div>
        <button class="button button--secondary" type="button" @click="fermerCandidature">Retour aux offres</button>
      </div>
      <p v-if="message" class="success" role="status">{{ message }}</p>
      <p v-if="erreur" class="error" role="alert">{{ erreur }}</p>
      <form class="form-grid" @submit.prevent="envoyerCandidature">
        <label>Nom<input v-model="formulaire.nom" required maxlength="100" autocomplete="family-name"></label>
        <label>Prénom<input v-model="formulaire.prenom" required maxlength="100" autocomplete="given-name"></label>
        <label>E-mail<input v-model="formulaire.email" required type="email" maxlength="255" autocomplete="email"></label>
        <label>Téléphone<input v-model="formulaire.telephone" maxlength="30" autocomplete="tel"></label>
        <label class="full">Adresse<input v-model="formulaire.adresse" autocomplete="street-address"></label>
        <label class="full">Message de motivation<textarea v-model="formulaire.messageCandidat" rows="5"></textarea></label>
        <div class="full form-actions">
          <button class="button" type="submit" :disabled="envoi">{{ envoi ? 'Envoi...' : 'Envoyer ma candidature' }}</button>
        </div>
      </form>
    </section>

    <section v-else class="offres">
      <div class="section-heading">
        <div>
          <p class="sur-titre">Opportunites ouvertes</p>
          <h2>Nos offres d’emploi</h2>
        </div>
        <button class="button button--secondary" type="button" @click="chargerOffres">Actualiser</button>
      </div>
      <p v-if="erreur" class="error" role="alert">{{ erreur }}</p>
      <p v-if="chargement">Chargement des offres...</p>
      <div v-else class="offres-grid">
        <article v-for="offre in offres" :key="offre.idOffre" class="offre-card">
          <div class="offre-card__reference">{{ offre.reference }}</div>
          <h3>{{ offre.titre }}</h3>
          <p class="offre-card__meta">{{ offre.lieuTravail || 'Lieu a definir' }} · {{ offre.typeContrat || 'Contrat a definir' }}</p>
          <p>{{ offre.description }}</p>
          <div class="offre-card__footer">
            <span>Profil : {{ offre.profilRecherche }}</span>
            <button class="button" type="button" @click="ouvrirCandidature(offre)">Postuler</button>
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
      <RouterLink :to="{ name: 'connexion' }">Accéder à l’espace administrateur</RouterLink>
    </footer>
  </main>
</template>

<style scoped>
.accueil { min-height: 100vh; background: #f8fafc; color: #172033; padding: 2rem clamp(1rem, 5vw, 5rem); }
.hero { display: flex; justify-content: space-between; align-items: end; gap: 2rem; max-width: 1200px; margin: 0 auto 3rem; padding: 2rem 0; }
.sur-titre { color: #4f46e5; font-size: .78rem; font-weight: 800; letter-spacing: .12em; margin: 0 0 .7rem; }
h1 { max-width: 700px; margin: 0; font-size: clamp(2.3rem, 6vw, 4.8rem); line-height: 1.05; } h2 { margin: 0; font-size: 2rem; } h3 { margin: .5rem 0; font-size: 1.3rem; }
.hero__texte { color: #64748b; font-size: 1.1rem; } .admin-link, .footer a { color: #4f46e5; font-weight: 700; text-decoration: none; }
.offres, .candidature-panel { max-width: 1200px; margin: 0 auto; } .candidature-panel { padding: 2rem; border-radius: 1.2rem; background: white; box-shadow: 0 15px 45px rgba(15, 23, 42, .08); }
.section-heading { display: flex; justify-content: space-between; align-items: end; gap: 1rem; margin-bottom: 1.5rem; } .offres-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 1.2rem; }
.offre-card { display: flex; flex-direction: column; min-height: 290px; padding: 1.4rem; border: 1px solid #e2e8f0; border-radius: 1rem; background: white; box-shadow: 0 8px 25px rgba(15, 23, 42, .04); }
.offre-card__reference { color: #4f46e5; font-size: .8rem; font-weight: 800; letter-spacing: .08em; } .offre-card__meta { color: #64748b; font-size: .9rem; } .offre-card__footer { display: flex; align-items: end; gap: 1rem; justify-content: space-between; margin-top: auto; } .offre-card__footer span { color: #475569; font-size: .85rem; }
.button { border: 0; border-radius: .6rem; padding: .75rem 1rem; background: #4f46e5; color: white; cursor: pointer; font: inherit; font-weight: 700; } .button:disabled { cursor: wait; opacity: .6; } .button--secondary { background: #eef2ff; color: #4338ca; }
.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 1rem; } label { display: flex; flex-direction: column; gap: .4rem; font-weight: 700; } input, textarea { width: 100%; box-sizing: border-box; border: 1px solid #cbd5e1; border-radius: .5rem; padding: .7rem; font: inherit; } .full { grid-column: 1 / -1; } .form-actions { display: flex; justify-content: end; }
.error { color: #b91c1c; } .success { padding: .8rem 1rem; border-radius: .5rem; background: #dcfce7; color: #166534; } .empty { grid-column: 1 / -1; padding: 3rem; border: 1px dashed #cbd5e1; border-radius: 1rem; text-align: center; color: #64748b; } .footer { display: flex; justify-content: center; gap: .5rem; margin: 5rem auto 1rem; color: #64748b; }
@media (max-width: 700px) { .hero, .section-heading, .footer { display: block; } .admin-link { display: inline-block; margin-top: 1rem; } .section-heading .button { margin-top: 1rem; } .form-grid { display: block; } label { margin-bottom: 1rem; } .offre-card__footer { align-items: start; flex-direction: column; } }
</style>
