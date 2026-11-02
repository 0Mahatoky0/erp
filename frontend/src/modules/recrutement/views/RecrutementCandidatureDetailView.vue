<script setup>
import PrimeButton from 'primevue/button'
import PrimeInputText from 'primevue/inputtext'
import PrimeTextarea from 'primevue/textarea'
import Message from 'primevue/message'
import PrimeSelect from 'primevue/select'
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { recrutementApi as api, ErreurApi } from '@/modules/recrutement/api/recrutementApi'

const route = useRoute()
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
  <section class="mx-auto w-full max-w-5xl p-4 md:p-6">
    <RouterLink class="back" :to="{ name: 'recrutement-suivi' }">← Retour au suivi</RouterLink>
    <p class="mb-1 text-xs font-semibold uppercase tracking-wider text-primary">Recrutement · Parcours candidat</p>
    <p v-if="chargement">Chargement du parcours...</p>
    <p v-else-if="erreur" class="text-sm text-red-600" role="alert">{{ erreur }}</p>

    <template v-else-if="candidature">
      <header class="header">
        <div>
          <h1>{{ candidature.candidat.prenom }} {{ candidature.candidat.nom }}</h1>
          <p>{{ candidature.candidat.email }} · {{ candidature.offre.reference }} · {{ candidature.offre.titre }}</p>
        </div>
        <b class="rounded-full bg-primary-100 px-2 py-1 text-xs font-semibold text-primary">{{ candidature.statut }}</b>
      </header>
      <Message v-if="message" severity="success" :closable="false">{{ message }}</Message>

      <div class="grid">
        <article class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
          <h2>Entretiens</h2>
          <div v-for="item in entretiens" :key="item.idEntretien" class="border-b border-surface py-3 last:border-b-0">
            <strong>{{ item.typeEntretien }}</strong>
            <span>{{ item.dateDebut }} → {{ item.dateFin }}</span>
            <small>{{ item.lieu || item.lienVisio || 'Lieu à définir' }} · {{ item.statut }}</small>
          </div>
          <p v-if="!entretiens.length">Aucun entretien planifié.</p>
          <PrimeButton
            class="inline-flex items-center gap-2"

            :aria-expanded="formulaireOuvert === 'entretien'"
            @click="ouvrirFormulaire('entretien')"
          >
            {{ formulaireOuvert === 'entretien' ? 'Fermer' : 'Planifier un entretien' }}
          </PrimeButton>
          <form v-if="formulaireOuvert === 'entretien'" class="flex flex-col gap-3" @submit.prevent="planifierEntretien">
            <h3>Planifier un entretien</h3>
            <label>Type<PrimeSelect v-model="entretien.typeEntretien" :options="['RH', 'TECHNIQUE', 'MANAGER', 'AUTRE']" /></label>
            <div class="form-grid">
              <label>Début<PrimeInputText v-model="entretien.dateDebut" required type="datetime-local" /></label>
              <label>Fin<PrimeInputText v-model="entretien.dateFin" required type="datetime-local" /></label>
              <label>Lieu<PrimeInputText v-model="entretien.lieu" /></label>
              <label>Lien visio<PrimeInputText v-model="entretien.lienVisio" /></label>
            </div>
            <PrimeButton type="submit" :disabled="envoi">Planifier</PrimeButton>
          </form>
        </article>

        <article class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
          <h2>Tests et passages</h2>
          <div v-for="item in passages" :key="item.idPassage" class="border-b border-surface py-3 last:border-b-0">
            <strong>Test #{{ item.idTest }}</strong>
            <span>{{ item.datePrevue || 'Date à définir' }} · {{ item.statut }}</span>
          </div>
          <p v-if="!passages.length">Aucun passage planifié.</p>
          <div class="flex flex-wrap gap-2">
            <PrimeButton
              class="inline-flex items-center gap-2"

              :aria-expanded="formulaireOuvert === 'passage'"
              @click="ouvrirFormulaire('passage')"
            >
              {{ formulaireOuvert === 'passage' ? 'Fermer' : 'Planifier un passage' }}
            </PrimeButton>
            <PrimeButton
              class="inline-flex items-center gap-2 border border-surface text-secondary-color"

              :aria-expanded="formulaireOuvert === 'test'"
              @click="ouvrirFormulaire('test')"
            >
              {{ formulaireOuvert === 'test' ? 'Fermer' : 'Créer un test' }}
            </PrimeButton>
          </div>
          <form v-if="formulaireOuvert === 'passage'" class="flex flex-col gap-3" @submit.prevent="planifierPassage">
            <h3>Planifier un passage</h3>
            <label>Test<PrimeSelect v-model="passage.idTest" :options="[{ idTest: '', titre: 'Sélectionnez un test' }, ...tests]" option-label="titre" option-value="idTest" required /></label>
            <label>Date prévue<PrimeInputText v-model="passage.datePrevue" type="datetime-local" /></label>
            <PrimeButton type="submit" :disabled="envoi || !tests.length">Planifier le test</PrimeButton>
          </form>
          <form v-if="formulaireOuvert === 'test'" class="flex flex-col gap-3" @submit.prevent="creerTest">
            <h3>Créer un test pour l’offre</h3>
            <label>Titre<PrimeInputText v-model="nouveauTest.titre" required /></label>
            <label>Description<PrimeTextarea v-model="nouveauTest.description" /></label>
            <div class="form-grid"><label>Durée (min)<PrimeInputText v-model="nouveauTest.dureeMinutes" type="number" min="1" /></label><label>Note max<PrimeInputText v-model="nouveauTest.noteMax" type="number" min="1" /></label></div>
            <PrimeButton type="submit" :disabled="envoi">Créer le test</PrimeButton>
          </form>
        </article>

        <article class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
          <h2>Évaluations</h2>
          <div v-for="item in evaluations" :key="item.idEvaluation" class="border-b border-surface py-3 last:border-b-0">
            <strong>{{ item.typeEvaluation }} · {{ item.note ?? 'Sans note' }}/{{ item.noteMax }}</strong>
            <span>{{ item.appreciation }}</span>
            <small>{{ item.commentaire }}</small>
          </div>
          <p v-if="!evaluations.length">Aucune évaluation.</p>
          <PrimeButton
            class="inline-flex items-center gap-2"

            :aria-expanded="formulaireOuvert === 'evaluation'"
            @click="ouvrirFormulaire('evaluation')"
          >
            {{ formulaireOuvert === 'evaluation' ? 'Fermer' : 'Ajouter une évaluation' }}
          </PrimeButton>
          <form v-if="formulaireOuvert === 'evaluation'" class="flex flex-col gap-3" @submit.prevent="enregistrerEvaluation">
            <h3>Ajouter une évaluation</h3>
            <div class="form-grid">
              <label>Type<PrimeSelect v-model="evaluation.typeEvaluation" :options="['DOSSIER', 'ENTRETIEN', 'TEST']" /></label>
              <label>Note<PrimeInputText v-model="evaluation.note" type="number" min="0" :max="evaluation.noteMax" step="0.01" /></label>
              <label>Note max<PrimeInputText v-model="evaluation.noteMax" type="number" min="1" /></label>
            </div>
            <label>Appréciation<PrimeInputText v-model="evaluation.appreciation" required /></label>
            <label>Commentaire<PrimeTextarea v-model="evaluation.commentaire" required /></label>
            <PrimeButton type="submit" :disabled="envoi">Enregistrer</PrimeButton>
          </form>
        </article>

        <article class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
          <h2>Décision finale</h2>
          <PrimeButton
            class="inline-flex items-center gap-2"

            :aria-expanded="formulaireOuvert === 'decision'"
            @click="ouvrirFormulaire('decision')"
          >
            {{ formulaireOuvert === 'decision' ? 'Fermer' : 'Enregistrer la décision' }}
          </PrimeButton>
          <form v-if="formulaireOuvert === 'decision'" class="flex flex-col gap-3" @submit.prevent="prendreDecision">
            <label>Décision<PrimeSelect v-model="decision.decision" :options="['RETENUE', 'REFUSEE', 'LISTE_ATTENTE']" /></label>
            <label>Motif<PrimeTextarea v-model="decision.motif" required /></label>
            <PrimeButton type="submit" :disabled="envoi">Enregistrer la décision</PrimeButton>
          </form>
          <h2>Historique</h2>
          <div v-for="item in historique" :key="item.idHistorique" class="border-b border-surface py-3 last:border-b-0">
            <strong>{{ item.nouveauStatut }}</strong><span>{{ item.dateAction }}</span><small>{{ item.commentaire || 'Aucun commentaire' }}</small>
          </div>
        </article>
      </div>
    </template>
  </section>
</template>
