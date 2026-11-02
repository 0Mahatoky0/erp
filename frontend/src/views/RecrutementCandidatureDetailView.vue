<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { api, ErreurApi } from '@/api/client'

const route = useRoute()
const auth = useAuthStore()
const id = route.params.id
const candidature = ref(null)
const entretiens = ref([])
const tests = ref([])
const passages = ref([])
const evaluations = ref([])
const historique = ref([])
const documents = ref([])
const erreur = ref('')
const message = ref('')
const chargement = ref(true)
const envoi = ref(false)
const formulaireOuvert = ref(null)

const entretien = reactive({ typeEntretien: 'RH', dateDebut: '', dateFin: '', lieu: '', lienVisio: '' })
const evaluation = reactive({
  typeEvaluation: 'DOSSIER', note: null, noteMax: 20, appreciation: '', commentaire: '',
})
const decision = reactive({ decision: 'RETENUE', motif: '' })
const nouveauTest = reactive({ titre: '', description: '', dureeMinutes: 60, noteMax: 20 })
const passage = reactive({ idTest: '', datePrevue: '', numeroTentative: 1 })

function afficherErreur(cause, defaut) {
  erreur.value = cause instanceof ErreurApi ? cause.message : defaut
}

async function charger() {
  chargement.value = true
  erreur.value = ''
  try {
    const resultat = await Promise.all([
      api.get(`/recrutement/candidatures/${id}`),
      api.get(`/recrutement/candidatures/${id}/entretiens`),
      api.get(`/recrutement/candidatures/${id}/passages-tests`),
      api.get(`/recrutement/candidatures/${id}/evaluations`),
      api.get(`/recrutement/candidatures/${id}/historique`),
      api.get(`/recrutement/candidatures/${id}/documents`),
    ])
    candidature.value = resultat[0]
    entretiens.value = resultat[1]
    passages.value = resultat[2]
    evaluations.value = resultat[3]
    historique.value = resultat[4]
    documents.value = resultat[5]
    tests.value = await api.get(`/recrutement/offres/${candidature.value.offre.idOffre}/tests`)
  } catch (cause) {
    afficherErreur(cause, 'Impossible de charger le parcours du candidat.')
  } finally {
    chargement.value = false
  }
}

async function executer(action, succes) {
  envoi.value = true
  erreur.value = ''
  message.value = ''
  try {
    await action()
    message.value = succes
    formulaireOuvert.value = null
    await charger()
  } catch (cause) {
    afficherErreur(cause, 'Impossible d’enregistrer les données.')
  } finally {
    envoi.value = false
  }
}

function planifierEntretien() {
  return executer(
    () => api.post('/recrutement/entretiens', { idCandidature: Number(id), ...entretien }),
    'Entretien planifié.',
  )
}

function enregistrerEvaluation() {
  return executer(
    () => api.post('/recrutement/evaluations', {
      idCandidature: Number(id),
      ...evaluation,
      note: evaluation.note === '' ? null : Number(evaluation.note),
      noteMax: Number(evaluation.noteMax),
    }),
    'Évaluation enregistrée.',
  )
}

function prendreDecision() {
  return executer(
    () => api.post('/recrutement/decisions', { idCandidature: Number(id), ...decision }),
    'Décision enregistrée.',
  )
}

function creerTest() {
  return executer(
    () => api.post('/recrutement/tests', {
      offre: { idOffre: candidature.value.offre.idOffre },
      ...nouveauTest,
      dureeMinutes: Number(nouveauTest.dureeMinutes),
      noteMax: Number(nouveauTest.noteMax),
    }),
    'Test créé.',
  )
}

function planifierPassage() {
  return executer(
    () => api.post('/recrutement/passages-tests', {
      test: { idTest: Number(passage.idTest) },
      candidature: { idCandidature: Number(id) },
      numeroTentative: Number(passage.numeroTentative),
      datePrevue: passage.datePrevue || null,
    }),
    'Passage de test planifié.',
  )
}

function ouvrirFormulaire(nom) {
  formulaireOuvert.value = formulaireOuvert.value === nom ? null : nom
}

onMounted(charger)
</script>

<template>
  <section class="recrutement-page">
    <RouterLink class="back" :to="{ name: 'recrutement-suivi' }">← Retour au suivi</RouterLink>
    <p class="eyebrow">Recrutement · Parcours candidat</p>
    <p v-if="chargement">Chargement du parcours...</p>
    <p v-else-if="erreur" class="error" role="alert">{{ erreur }}</p>

    <template v-else-if="candidature">
      <header class="header">
        <div>
          <h1>{{ candidature.candidat.prenom }} {{ candidature.candidat.nom }}</h1>
          <p>{{ candidature.candidat.email }} · {{ candidature.offre.reference }} · {{ candidature.offre.titre }}</p>
        </div>
        <b class="status">{{ candidature.statut }}</b>
      </header>
      <p v-if="message" class="success">{{ message }}</p>

      <div class="grid">
        <article class="panel">
          <h2>Entretiens</h2>
          <div v-for="item in entretiens" :key="item.idEntretien" class="item">
            <strong>{{ item.typeEntretien }}</strong>
            <span>{{ item.dateDebut }} → {{ item.dateFin }}</span>
            <small>{{ item.lieu || item.lienVisio || 'Lieu à définir' }} · {{ item.statut }}</small>
          </div>
          <p v-if="!entretiens.length">Aucun entretien planifié.</p>
          <button
            class="action"
            type="button"
            :aria-expanded="formulaireOuvert === 'entretien'"
            @click="ouvrirFormulaire('entretien')"
          >
            {{ formulaireOuvert === 'entretien' ? 'Fermer' : 'Planifier un entretien' }}
          </button>
          <form v-if="formulaireOuvert === 'entretien'" class="form" @submit.prevent="planifierEntretien">
            <h3>Planifier un entretien</h3>
            <label>Type<select v-model="entretien.typeEntretien"><option>RH</option><option>TECHNIQUE</option><option>MANAGER</option><option>AUTRE</option></select></label>
            <div class="form-grid">
              <label>Début<input v-model="entretien.dateDebut" required type="datetime-local"></label>
              <label>Fin<input v-model="entretien.dateFin" required type="datetime-local"></label>
              <label>Lieu<input v-model="entretien.lieu"></label>
              <label>Lien visio<input v-model="entretien.lienVisio"></label>
            </div>
            <button :disabled="envoi" type="submit">Planifier</button>
          </form>
        </article>

        <article class="panel">
          <h2>Tests et passages</h2>
          <div v-for="item in passages" :key="item.idPassage" class="item">
            <strong>Test #{{ item.idTest }}</strong>
            <span>{{ item.datePrevue || 'Date à définir' }} · {{ item.statut }}</span>
          </div>
          <p v-if="!passages.length">Aucun passage planifié.</p>
          <div class="actions">
            <button
              class="action"
              type="button"
              :aria-expanded="formulaireOuvert === 'passage'"
              @click="ouvrirFormulaire('passage')"
            >
              {{ formulaireOuvert === 'passage' ? 'Fermer' : 'Planifier un passage' }}
            </button>
            <button
              class="action action--secondary"
              type="button"
              :aria-expanded="formulaireOuvert === 'test'"
              @click="ouvrirFormulaire('test')"
            >
              {{ formulaireOuvert === 'test' ? 'Fermer' : 'Créer un test' }}
            </button>
          </div>
          <form v-if="formulaireOuvert === 'passage'" class="form" @submit.prevent="planifierPassage">
            <h3>Planifier un passage</h3>
            <label>Test<select v-model="passage.idTest" required><option value="">Sélectionnez un test</option><option v-for="test in tests" :key="test.idTest" :value="test.idTest">{{ test.titre }}</option></select></label>
            <label>Date prévue<input v-model="passage.datePrevue" type="datetime-local"></label>
            <button :disabled="envoi || !tests.length" type="submit">Planifier le test</button>
          </form>
          <form v-if="formulaireOuvert === 'test'" class="form" @submit.prevent="creerTest">
            <h3>Créer un test pour l’offre</h3>
            <label>Titre<input v-model="nouveauTest.titre" required></label>
            <label>Description<textarea v-model="nouveauTest.description"></textarea></label>
            <div class="form-grid"><label>Durée (min)<input v-model="nouveauTest.dureeMinutes" type="number" min="1"></label><label>Note max<input v-model="nouveauTest.noteMax" type="number" min="1"></label></div>
            <button :disabled="envoi" type="submit">Créer le test</button>
          </form>
        </article>

        <article class="panel">
          <h2>Évaluations</h2>
          <div v-for="item in evaluations" :key="item.idEvaluation" class="item">
            <strong>{{ item.typeEvaluation }} · {{ item.note ?? 'Sans note' }}/{{ item.noteMax }}</strong>
            <span>{{ item.appreciation }}</span>
            <small>{{ item.commentaire }}</small>
          </div>
          <p v-if="!evaluations.length">Aucune évaluation.</p>
          <button
            class="action"
            type="button"
            :aria-expanded="formulaireOuvert === 'evaluation'"
            @click="ouvrirFormulaire('evaluation')"
          >
            {{ formulaireOuvert === 'evaluation' ? 'Fermer' : 'Ajouter une évaluation' }}
          </button>
          <form v-if="formulaireOuvert === 'evaluation'" class="form" @submit.prevent="enregistrerEvaluation">
            <h3>Ajouter une évaluation</h3>
            <div class="form-grid">
              <label>Type<select v-model="evaluation.typeEvaluation"><option>DOSSIER</option><option>ENTRETIEN</option><option>TEST</option></select></label>
              <label>Note<input v-model="evaluation.note" type="number" min="0" :max="evaluation.noteMax" step="0.01"></label>
              <label>Note max<input v-model="evaluation.noteMax" type="number" min="1"></label>
            </div>
            <label>Appréciation<input v-model="evaluation.appreciation" required></label>
            <label>Commentaire<textarea v-model="evaluation.commentaire" required></textarea></label>
            <button :disabled="envoi" type="submit">Enregistrer</button>
          </form>
        </article>

        <article class="panel">
          <h2>Décision finale</h2>
          <button
            class="action"
            type="button"
            :aria-expanded="formulaireOuvert === 'decision'"
            @click="ouvrirFormulaire('decision')"
          >
            {{ formulaireOuvert === 'decision' ? 'Fermer' : 'Enregistrer la décision' }}
          </button>
          <form v-if="formulaireOuvert === 'decision'" class="form" @submit.prevent="prendreDecision">
            <label>Décision<select v-model="decision.decision"><option>RETENUE</option><option>REFUSEE</option><option>LISTE_ATTENTE</option></select></label>
            <label>Motif<textarea v-model="decision.motif" required></textarea></label>
            <button :disabled="envoi" type="submit">Enregistrer la décision</button>
          </form>
          <h2>Historique</h2>
          <div v-for="item in historique" :key="item.idHistorique" class="item">
            <strong>{{ item.nouveauStatut }}</strong><span>{{ item.dateAction }}</span><small>{{ item.commentaire || 'Aucun commentaire' }}</small>
          </div>
        </article>
      </div>
    </template>
  </section>
</template>

<style scoped>
.recrutement-page { max-width: 1100px; margin: 0 auto; padding: 2rem 1.5rem; } .back { color: #4f46e5; } .eyebrow { color: #4f46e5; font-weight: 700; text-transform: uppercase; } h1 { margin-bottom: .5rem; } .header { display: flex; justify-content: space-between; align-items: center; gap: 1rem; margin: 1.5rem 0; } .status { color: #4f46e5; } .grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 1rem; } .panel { padding: 1rem; border: 1px solid #e5e7eb; border-radius: .8rem; background: white; } .item { display: grid; gap: .25rem; padding: .7rem 0; border-bottom: 1px solid #e5e7eb; } .item small, .item span { color: #64748b; } .actions { display: flex; flex-wrap: wrap; gap: .5rem; margin-top: 1rem; } .action { background: #4f46e5; } .action--secondary { background: #334155; } .form { display: grid; gap: .6rem; margin-top: 1rem; padding-top: 1rem; border-top: 1px solid #e5e7eb; } label { display: grid; gap: .3rem; font-weight: 600; } input, select, textarea { width: 100%; box-sizing: border-box; padding: .55rem; border: 1px solid #cbd5e1; border-radius: .4rem; font: inherit; } textarea { min-height: 70px; } .form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: .6rem; } button { border: 0; border-radius: .5rem; padding: .6rem .8rem; background: #4f46e5; color: white; cursor: pointer; } button:disabled { opacity: .6; cursor: wait; } .error { color: #b91c1c; } .success { color: #166534; } @media (max-width: 800px) { .grid { grid-template-columns: 1fr; } .header { align-items: flex-start; flex-direction: column; } }
</style>
