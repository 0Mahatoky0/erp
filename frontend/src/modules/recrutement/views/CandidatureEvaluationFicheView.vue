<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import Message from 'primevue/message'
import Tag from 'primevue/tag'
import Textarea from 'primevue/textarea'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import { useNotificationStore } from '@/shared/store/useNotificationStore'
import { formatDateHeure } from '@/shared/utils/format'
import { recrutementApi as api, ErreurApi } from '../api/recrutementApi'
import NoteEvaluation from '../components/NoteEvaluation.vue'
import SaisieEvaluationForm from '../components/SaisieEvaluationForm.vue'
import SaisieDecisionForm from '../components/SaisieDecisionForm.vue'
import { TYPES_EVALUATION, appreciation, libelleTypeEvaluation } from '../utils/notes'
import { TRANSITIONS_CANDIDATURE, libelleStatut } from '../utils/statutsCandidature'

const route = useRoute()
const notifications = useNotificationStore()

const candidature = ref(null)
const offre = ref(null)
const evaluations = ref([])
const synthese = ref(null)
const entretiens = ref([])
const passages = ref([])
const tests = ref([])
const historique = ref([])
const candidats = ref([])
const loading = ref(false)

// Gestion du refus via Dialog (comme dans la fiche candidature)
const motifDialog = ref(false)
const motif = ref('')
const motifManquant = ref(false)

const idCandidature = computed(() => Number(route.params.idCandidature))
const statut = computed(() => String(route.query.statut || candidature.value?.statut || 'RECUE'))
const actions = computed(() => TRANSITIONS_CANDIDATURE[candidature.value?.statut] || [])

const index = computed(() => candidats.value.findIndex((item) => item.idCandidature === idCandidature.value))
const precedent = computed(() => (index.value > 0 ? candidats.value[index.value - 1] : null))
const suivant = computed(() => (index.value >= 0 && index.value < candidats.value.length - 1 ? candidats.value[index.value + 1] : null))

const evaluationsParType = computed(() =>
  TYPES_EVALUATION.map((type) => ({
    ...type,
    elements: evaluations.value
      .filter((item) => item.typeEvaluation === type.valeur)
      .sort((a, b) => String(b.dateEvaluation).localeCompare(String(a.dateEvaluation))),
  })).filter((groupe) => groupe.elements.length > 0),
)

const aEvalue = (typeVal) => evaluations.value.some((item) => item.typeEvaluation === typeVal)
const categoriesManquantes = computed(() =>
  TYPES_EVALUATION.filter((type) => !aEvalue(type.valeur)).map((type) => libelleTypeEvaluation(type.valeur)),
)

async function charger() {
  loading.value = true
  try {
    const detail = await api.get(`/recrutement/candidatures/${idCandidature.value}`)
    candidature.value = detail
    offre.value = detail.offre
    const [liste, historiqueResultat, evaluationsResultat, syntheseResultat, entretiensResultat, passagesResultat, testsResultat] =
      await Promise.all([
        api.get(`/recrutement/offres/${route.params.idOffre}/candidatures?statut=${statut.value}`),
        api.get(`/recrutement/candidatures/${idCandidature.value}/historique`),
        api.get(`/recrutement/candidatures/${idCandidature.value}/evaluations`),
        api.get(`/recrutement/candidatures/${idCandidature.value}/evaluations/synthese`),
        api.get(`/recrutement/candidatures/${idCandidature.value}/entretiens`),
        api.get(`/recrutement/candidatures/${idCandidature.value}/passages-tests`),
        api.get(`/recrutement/offres/${route.params.idOffre}/tests`),
      ])
    candidats.value = liste
    historique.value = historiqueResultat
    evaluations.value = evaluationsResultat
    synthese.value = syntheseResultat
    entretiens.value = entretiensResultat
    passages.value = passagesResultat
    tests.value = testsResultat
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Impossible de charger la fiche d’évaluation.')
  } finally {
    loading.value = false
  }
}

async function changerStatut(nouveauStatut, commentaire) {
  try {
    await api.patch(`/recrutement/candidatures/${idCandidature.value}/statut`, {
      statut: nouveauStatut,
      commentaire,
    })
    notifications.succes('Statut de la candidature mis à jour.')
    await charger()
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Impossible de modifier le statut.')
  }
}

function appliquer(action) {
  if (action.statut !== 'REFUSEE') {
    changerStatut(action.statut, action.libelle)
    return
  }
  motif.value = ''
  motifManquant.value = false
  motifDialog.value = true
}

async function confirmerRefus() {
  const texte = motif.value.trim()
  if (!texte) {
    motifManquant.value = true
    return
  }
  await changerStatut('REFUSEE', texte)
  motifDialog.value = false
}

function lienVers(candidat) {
  return {
    name: 'recrutement-candidature-evaluation-fiche',
    params: { idOffre: route.params.idOffre, idCandidature: candidat.idCandidature },
    query: { statut: statut.value },
  }
}

onMounted(charger)
</script>

<template>
  <section>
    <div v-if="loading" class="py-8 text-center text-muted-color">Chargement de l’évaluation...</div>
    <EmptyState v-else-if="!candidature" titre="Candidature introuvable" message="Cette candidature n’est plus disponible." />
    <template v-else>
      <!-- Barre de navigation -->
      <div class="mb-4 flex flex-wrap items-center justify-between gap-3">
        <Button
          label="Retour à l’évaluation de l’offre"
          icon="pi pi-arrow-left"
          text
          severity="secondary"
          as="router-link"
          :to="{ name: 'recrutement-candidature-evaluation-liste', params: { idOffre: route.params.idOffre }, query: { statut } }"
        />
        <div class="flex gap-2">
          <Button
            v-if="precedent"
            label="Précédent"
            icon="pi pi-chevron-left"
            variant="outlined"
            size="small"
            as="router-link"
            :to="lienVers(precedent)"
          />
          <Button v-else label="Précédent" icon="pi pi-chevron-left" variant="outlined" size="small" disabled />
          <Button
            v-if="suivant"
            label="Suivant"
            icon="pi pi-chevron-right"
            icon-pos="right"
            variant="outlined"
            size="small"
            as="router-link"
            :to="lienVers(suivant)"
          />
          <Button v-else label="Suivant" icon="pi pi-chevron-right" icon-pos="right" variant="outlined" size="small" disabled />
        </div>
      </div>

      <!-- En-tête avec statut et boutons d'actions de transition -->
      <header class="mb-5 flex flex-col justify-between gap-4 md:flex-row md:items-start">
        <div>
          <p class="mb-1 text-xs font-semibold uppercase tracking-wider text-primary">Fiche d’évaluation</p>
          <h1 class="m-0 text-2xl font-bold">{{ candidature.candidat?.prenom }} {{ candidature.candidat?.nom }}</h1>
          <p class="mt-1 text-muted-color">{{ candidature.candidat?.email }} · {{ offre?.reference }} · {{ offre?.titre }}</p>
        </div>
        <div class="flex flex-wrap items-center gap-2">
          <StatusBadge :valeur="candidature.statut" />
          <Button
            v-for="action in actions"
            :key="action.statut"
            :label="action.libelle"
            :icon="action.icone"
            :severity="action.severite"
            size="small"
            variant="outlined"
            @click="appliquer(action)"
          />
        </div>
      </header>

      <div class="grid gap-5 lg:grid-cols-2">
        <!-- Synthèse des notes -->
        <Card class="lg:col-span-2">
          <template #title>Synthèse des notes</template>
          <template #content>
            <dl class="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
              <div>
                <dt class="text-sm text-muted-color">Note dossier</dt>
                <dd class="m-0 text-lg"><NoteEvaluation :categorie="synthese?.dossier" /></dd>
              </div>
              <div>
                <dt class="text-sm text-muted-color">Note entretien</dt>
                <dd class="m-0 text-lg"><NoteEvaluation :categorie="synthese?.entretien" /></dd>
              </div>
              <div>
                <dt class="text-sm text-muted-color">Note test</dt>
                <dd class="m-0 text-lg"><NoteEvaluation :categorie="synthese?.test" /></dd>
              </div>
              <div>
                <dt class="text-sm text-muted-color">Moyenne</dt>
                <dd class="m-0 text-lg">
                  <NoteEvaluation
                    :categorie="{ evaluee: synthese?.noteGlobale != null, note: synthese?.noteGlobale ?? null, noteMax: 20 }"
                  />
                </dd>
              </div>
            </dl>
            <p v-if="categoriesManquantes.length" class="mt-4 mb-0 text-sm text-muted-color">
              <i class="pi pi-info-circle mr-1" />
              Pas encore évalué : {{ categoriesManquantes.join(', ') }}.
            </p>
            <p v-else class="mt-4 mb-0 text-sm text-muted-color">Toutes les catégories ont été évaluées.</p>
          </template>
        </Card>

        <!-- Composants spécifiques affichés UNIQUEMENT au statut EN_EVALUATION -->
        <template v-if="candidature.statut === 'EN_EVALUATION'">
          <SaisieEvaluationForm
            :id-candidature="idCandidature"
            :entretiens="entretiens"
            :passages="passages"
            :tests="tests"
            @saved="charger"
          />
          <SaisieDecisionForm :id-candidature="idCandidature" @saved="charger" />
        </template>

        <!-- Détails offre & candidat -->
        <Card>
          <template #title>Offre d’emploi</template>
          <template #content>
            <dl class="grid gap-3 sm:grid-cols-2">
              <div><dt class="text-sm text-muted-color">Référence</dt><dd class="font-semibold">{{ offre?.reference }}</dd></div>
              <div><dt class="text-sm text-muted-color">Poste</dt><dd class="font-semibold">{{ offre?.titre }}</dd></div>
              <div><dt class="text-sm text-muted-color">Lieu</dt><dd class="font-semibold">{{ offre?.lieuTravail || '—' }}</dd></div>
              <div><dt class="text-sm text-muted-color">Contrat</dt><dd class="font-semibold">{{ offre?.typeContrat || '—' }}</dd></div>
            </dl>
          </template>
        </Card>

        <Card>
          <template #title>Candidat</template>
          <template #content>
            <dl class="grid gap-3 sm:grid-cols-2">
              <div><dt class="text-sm text-muted-color">Nom</dt><dd class="font-semibold">{{ candidature.candidat?.nom }}</dd></div>
              <div><dt class="text-sm text-muted-color">Prénom</dt><dd class="font-semibold">{{ candidature.candidat?.prenom }}</dd></div>
              <div><dt class="text-sm text-muted-color">Email</dt><dd class="font-semibold">{{ candidature.candidat?.email }}</dd></div>
              <div><dt class="text-sm text-muted-color">Source</dt><dd class="font-semibold">{{ candidature.source || '—' }}</dd></div>
            </dl>
            <p class="mt-4 whitespace-pre-line">{{ candidature.messageCandidat || 'Aucun message.' }}</p>
          </template>
        </Card>

        <!-- Évaluations enregistrées -->
        <Card>
          <template #title>Évaluations enregistrées</template>
          <template #content>
            <p v-if="!evaluationsParType.length" class="m-0 text-muted-color">
              Aucune évaluation n’a encore été enregistrée pour ce candidat.
            </p>
            <div v-for="groupe in evaluationsParType" :key="groupe.valeur" class="mb-4 last:mb-0">
              <h3 class="mt-0 mb-2 text-base font-semibold">{{ groupe.libelle }}</h3>
              <div
                v-for="item in groupe.elements"
                :key="item.idEvaluation"
                class="border-b border-surface py-3 last:border-0"
              >
                <div class="flex flex-wrap items-center gap-3">
                  <strong v-if="item.note !== null && item.note !== undefined">
                    {{ item.note }} <span class="font-normal text-muted-color">/ {{ item.noteMax }}</span>
                  </strong>
                  <Tag v-else value="Sans note" severity="secondary" />
                  <Tag :value="appreciation(item.appreciation).libelle" :severity="appreciation(item.appreciation).severite" />
                  <span class="text-sm text-muted-color">{{ formatDateHeure(item.dateEvaluation) }}</span>
                </div>
                <p class="m-0 mt-2 whitespace-pre-line text-sm">{{ item.commentaire }}</p>
              </div>
            </div>
          </template>
        </Card>

        <!-- Historique -->
        <Card>
          <template #title>Historique</template>
          <template #content>
            <div v-for="item in historique" :key="item.idHistorique" class="border-b border-surface py-2 last:border-0">
              <Tag :value="libelleStatut(item.nouveauStatut)" />
              <p class="m-0 mt-1 text-sm text-muted-color">{{ item.commentaire || 'Changement de statut' }}</p>
            </div>
            <p v-if="!historique.length" class="m-0 text-muted-color">Aucun historique.</p>
          </template>
        </Card>
      </div>

      <!-- Modale de refus -->
      <Dialog v-model:visible="motifDialog" modal header="Écarter la candidature" :style="{ width: '28rem' }">
        <div class="flex flex-col gap-2">
          <label for="motif-refus" class="text-sm font-semibold text-muted-color">Motif du refus</label>
          <Textarea id="motif-refus" v-model="motif" rows="4" auto-resize />
          <Message v-if="motifManquant" severity="error" size="small" :closable="false">
            Le motif du refus est obligatoire.
          </Message>
        </div>
        <template #footer>
          <Button label="Annuler" severity="secondary" variant="outlined" @click="motifDialog = false" />
          <Button label="Confirmer" icon="pi pi-check" severity="danger" @click="confirmerRefus" />
        </template>
      </Dialog>
    </template>
  </section>
</template>