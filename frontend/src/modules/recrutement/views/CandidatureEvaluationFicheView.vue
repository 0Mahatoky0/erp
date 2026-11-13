<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import Card from 'primevue/card'
import Button from 'primevue/button'
import InputNumber from 'primevue/inputnumber'
import Select from 'primevue/select'
import Textarea from 'primevue/textarea'
import Tag from 'primevue/tag'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import { useNotificationStore } from '@/shared/store/useNotificationStore'
import { formatDateHeure } from '@/shared/utils/format'
import { recrutementApi as api, ErreurApi } from '../api/recrutementApi'
import NoteEvaluation from '../components/NoteEvaluation.vue'
import { APPRECIATIONS, TYPES_EVALUATION, appreciation, libelleTypeEvaluation } from '../utils/notes'

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
const saving = ref(false)

const evaluation = reactive({
  typeEvaluation: 'DOSSIER',
  idEntretien: null,
  idPassage: null,
  note: null,
  noteMax: 20,
  appreciation: null,
  commentaire: '',
})

watch(
  () => evaluation.typeEvaluation,
  (nouveauType) => {
    if (nouveauType !== 'ENTRETIEN') evaluation.idEntretien = null
    if (nouveauType !== 'TEST') evaluation.idPassage = null
  },
)

const titresTests = computed(() => Object.fromEntries(tests.value.map((test) => [test.idTest, test.titre])))
const optionsEntretiens = computed(() =>
  entretiens.value.map((entretien) => ({
    valeur: entretien.idEntretien,
    libelle: `${entretien.typeEntretien} · ${formatDateHeure(entretien.dateDebut)}`,
  })),
)
const optionsPassages = computed(() =>
  passages.value.map((passage) => ({
    valeur: passage.idPassage,
    libelle: `${titresTests.value[passage.idTest] || `Test n°${passage.idTest}`} · tentative ${passage.numeroTentative}`,
  })),
)

/** Évaluations enregistrées, regroupées par catégorie dans l'ordre dossier, entretien, test. */
const evaluationsParType = computed(() =>
  TYPES_EVALUATION.map((type) => ({
    ...type,
    elements: evaluations.value
      .filter((item) => item.typeEvaluation === type.valeur)
      .sort((a, b) => String(b.dateEvaluation).localeCompare(String(a.dateEvaluation))),
  })).filter((groupe) => groupe.elements.length > 0),
)

// Vérification robuste basée sur le tableau des évaluations réelles reçues de l'API
const aEvalue = (typeVal) => evaluations.value.some((item) => item.typeEvaluation === typeVal)

const categoriesManquantes = computed(() =>
  TYPES_EVALUATION.filter((type) => !aEvalue(type.valeur)).map((type) => libelleTypeEvaluation(type.valeur)),
)

const decision = reactive({ decision: 'RETENUE', motif: '' })
const idCandidature = computed(() => Number(route.params.idCandidature))
const statut = computed(() => String(route.query.statut || candidature.value?.statut || 'RECUE'))
const index = computed(() => candidats.value.findIndex((item) => item.idCandidature === idCandidature.value))
const precedent = computed(() => index.value > 0 ? candidats.value[index.value - 1] : null)
const suivant = computed(() => index.value >= 0 && index.value < candidats.value.length - 1 ? candidats.value[index.value + 1] : null)
const transitions = computed(() => ({
  RECUE: ['EN_ETUDE'],
  EN_ETUDE: ['PRESELECTIONNEE', 'REFUSEE'],
  PRESELECTIONNEE: ['EN_EVALUATION', 'REFUSEE'],
  EN_EVALUATION: ['RETENUE', 'LISTE_ATTENTE', 'REFUSEE'],
  LISTE_ATTENTE: ['RETENUE', 'REFUSEE'],
}[candidature.value?.statut] || []))

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

async function modifierStatut(nouveauStatut) {
  saving.value = true
  try {
    await api.patch(`/recrutement/candidatures/${idCandidature.value}/statut`, {
      statut: nouveauStatut,
      commentaire: `Évaluation : passage au statut ${nouveauStatut}`,
    })
    notifications.succes('Statut mis à jour.')
    await charger()
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Impossible de modifier le statut.')
  } finally {
    saving.value = false
  }
}

async function enregistrerEvaluation() {
  if (!evaluation.appreciation) {
    notifications.erreur('Choisissez une appréciation.')
    return
  }
  if (evaluation.typeEvaluation === 'ENTRETIEN' && !evaluation.idEntretien) {
    notifications.erreur('Choisissez l’entretien évalué.')
    return
  }
  if (evaluation.typeEvaluation === 'TEST' && !evaluation.idPassage) {
    notifications.erreur('Choisissez le passage de test évalué.')
    return
  }
  saving.value = true
  try {
    await api.post('/recrutement/evaluations', {
      idCandidature: idCandidature.value,
      typeEvaluation: evaluation.typeEvaluation,
      idEntretien: evaluation.typeEvaluation === 'ENTRETIEN' ? evaluation.idEntretien : null,
      idPassage: evaluation.typeEvaluation === 'TEST' ? evaluation.idPassage : null,
      note: evaluation.note === null ? null : Number(evaluation.note),
      noteMax: Number(evaluation.noteMax),
      appreciation: evaluation.appreciation,
      commentaire: evaluation.commentaire,
    })
    notifications.succes('Évaluation enregistrée.')
    evaluation.idEntretien = null
    evaluation.idPassage = null
    evaluation.note = null
    evaluation.appreciation = null
    evaluation.commentaire = ''
    await charger()
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Impossible d’enregistrer l’évaluation.')
  } finally {
    saving.value = false
  }
}

async function enregistrerDecision() {
  saving.value = true
  try {
    await api.post('/recrutement/decisions', { idCandidature: idCandidature.value, ...decision })
    notifications.succes('Décision enregistrée.')
    await charger()
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Impossible d’enregistrer la décision.')
  } finally {
    saving.value = false
  }
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
      <div class="mb-5 flex flex-wrap items-center justify-between gap-3">
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
            as="router-link"
            :to="lienVers(precedent)"
          />
          <Button v-else label="Précédent" icon="pi pi-chevron-left" variant="outlined" disabled />
          <Button
            v-if="suivant"
            label="Suivant"
            icon="pi pi-chevron-right"
            icon-pos="right"
            variant="outlined"
            as="router-link"
            :to="lienVers(suivant)"
          />
          <Button v-else label="Suivant" icon="pi pi-chevron-right" icon-pos="right" variant="outlined" disabled />
        </div>
      </div>

      <header class="mb-5 flex flex-col justify-between gap-4 md:flex-row md:items-start">
        <div>
          <p class="mb-1 text-xs font-semibold uppercase tracking-wider text-primary">Fiche d’évaluation</p>
          <h1 class="m-0 text-2xl font-bold">{{ candidature.candidat?.prenom }} {{ candidature.candidat?.nom }}</h1>
          <p class="mt-1 text-muted-color">{{ candidature.candidat?.email }} · {{ offre?.reference }} · {{ offre?.titre }}</p>
        </div>
        <StatusBadge :valeur="candidature.statut" />
      </header>

      <div class="grid gap-5 lg:grid-cols-2">
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

        <Card>
          <template #title>Action sur le statut</template>
          <template #content>
            <div class="flex flex-wrap gap-2">
              <Button v-for="transition in transitions" :key="transition" :label="transition" :disabled="saving" @click="modifierStatut(transition)" />
            </div>
            <p v-if="!transitions.length" class="m-0 text-sm text-muted-color">Ce statut ne permet plus de transition.</p>
          </template>
        </Card>

        <!-- Formulaire affiché UNIQUEMENT si le statut est EN_EVALUATION -->
        <Card v-if="candidature.statut === 'EN_EVALUATION'">
          <template #title>Nouvelle évaluation</template>
          <template #content>
            <form class="grid gap-3" @submit.prevent="enregistrerEvaluation">
              <Select
                v-model="evaluation.typeEvaluation"
                :options="TYPES_EVALUATION"
                option-label="libelle"
                option-value="valeur"
                placeholder="Type d’évaluation"
              />
              <template v-if="evaluation.typeEvaluation === 'ENTRETIEN'">
                <Select
                  v-model="evaluation.idEntretien"
                  :options="optionsEntretiens"
                  option-label="libelle"
                  option-value="valeur"
                  placeholder="Entretien évalué"
                />
                <p v-if="!optionsEntretiens.length" class="m-0 text-sm text-muted-color">
                  Aucun entretien n’est planifié pour ce candidat : planifiez-en un avant de l’évaluer.
                </p>
              </template>
              <template v-if="evaluation.typeEvaluation === 'TEST'">
                <Select
                  v-model="evaluation.idPassage"
                  :options="optionsPassages"
                  option-label="libelle"
                  option-value="valeur"
                  placeholder="Passage de test évalué"
                />
                <p v-if="!optionsPassages.length" class="m-0 text-sm text-muted-color">
                  Aucun passage de test n’est planifié pour ce candidat : planifiez-en un avant de l’évaluer.
                </p>
              </template>
              <div class="grid grid-cols-2 gap-3">
                <InputNumber
                  v-model="evaluation.note"
                  :min="0"
                  :max="evaluation.noteMax"
                  :min-fraction-digits="0"
                  :max-fraction-digits="2"
                  placeholder="Note"
                />
                <InputNumber v-model="evaluation.noteMax" :min="1" :max-fraction-digits="2" placeholder="Note maximale" />
              </div>
              <Select
                v-model="evaluation.appreciation"
                :options="APPRECIATIONS"
                option-label="libelle"
                option-value="valeur"
                placeholder="Appréciation"
              />
              <Textarea v-model="evaluation.commentaire" rows="4" placeholder="Commentaire obligatoire" required />
              <Button type="submit" label="Enregistrer l’évaluation" icon="pi pi-save" :loading="saving" />
            </form>
          </template>
        </Card>

        <Card>
          <template #title>Décision</template>
          <template #content>
            <form class="grid gap-3" @submit.prevent="enregistrerDecision">
              <Select v-model="decision.decision" :options="['RETENUE', 'LISTE_ATTENTE', 'REFUSEE']" />
              <Textarea v-model="decision.motif" rows="4" placeholder="Motif obligatoire" required />
              <Button type="submit" label="Enregistrer la décision" severity="success" :loading="saving" />
            </form>
          </template>
        </Card>

        <Card class="lg:col-span-2">
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

        <Card>
          <template #title>Historique</template>
          <template #content>
            <div v-for="item in historique" :key="item.idHistorique" class="border-b border-surface py-2 last:border-0">
              <Tag :value="item.nouveauStatut" />
              <p class="m-0 mt-1 text-sm text-muted-color">{{ item.commentaire || 'Changement de statut' }}</p>
            </div>
            <p v-if="!historique.length" class="m-0 text-muted-color">Aucun historique.</p>
          </template>
        </Card>
      </div>
    </template>
  </section>
</template>