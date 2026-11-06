<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Card from 'primevue/card'
import Button from 'primevue/button'
import InputNumber from 'primevue/inputnumber'
import Select from 'primevue/select'
import Textarea from 'primevue/textarea'
import Tag from 'primevue/tag'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import { useNotificationStore } from '@/shared/store/useNotificationStore'
import { recrutementApi as api, ErreurApi } from '../api/recrutementApi'

const route = useRoute()
const router = useRouter()
const notifications = useNotificationStore()
const candidature = ref(null)
const offre = ref(null)
const evaluations = ref([])
const historique = ref([])
const candidats = ref([])
const loading = ref(false)
const saving = ref(false)

const evaluation = reactive({
  typeEvaluation: 'DOSSIER',
  note: null,
  noteMax: 20,
  appreciation: 'MOYENNE',
  commentaire: '',
})
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
    const [liste, historiqueResultat, evaluationsResultat] = await Promise.all([
      api.get(`/recrutement/offres/${route.params.idOffre}/candidatures?statut=${statut.value}`),
      api.get(`/recrutement/candidatures/${idCandidature.value}/historique`),
      api.get(`/recrutement/candidatures/${idCandidature.value}/evaluations`),
    ])
    candidats.value = liste
    historique.value = historiqueResultat
    evaluations.value = evaluationsResultat
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
  saving.value = true
  try {
    await api.post('/recrutement/evaluations', {
      idCandidature: idCandidature.value,
      ...evaluation,
      note: evaluation.note === null ? null : Number(evaluation.note),
      noteMax: Number(evaluation.noteMax),
    })
    notifications.succes('Évaluation enregistrée.')
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

function allerVers(candidat) {
  if (!candidat) return
  router.push({
    name: 'recrutement-candidature-evaluation-fiche',
    params: { idOffre: route.params.idOffre, idCandidature: candidat.idCandidature },
    query: { statut: statut.value },
  })
}

onMounted(charger)
</script>

<template>
  <section>
    <div v-if="loading" class="py-8 text-center text-muted-color">Chargement de l’évaluation...</div>
    <EmptyState v-else-if="!candidature" titre="Candidature introuvable" message="Cette candidature n’est plus disponible." />
    <template v-else>
      <div class="mb-5 flex flex-wrap items-center justify-between gap-3">
        <Button label="Retour à l’évaluation de l’offre" icon="pi pi-arrow-left" text severity="secondary" @click="router.push({ name: 'recrutement-candidature-evaluation-liste' })" />
        <div class="flex gap-2">
          <Button label="Précédent" icon="pi pi-chevron-left" outlined :disabled="!precedent" @click="allerVers(precedent)" />
          <Button label="Suivant" icon="pi pi-chevron-right" icon-pos="right" outlined :disabled="!suivant" @click="allerVers(suivant)" />
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

        <Card>
          <template #title>Nouvelle évaluation</template>
          <template #content>
            <form class="grid gap-3" @submit.prevent="enregistrerEvaluation">
              <Select v-model="evaluation.typeEvaluation" :options="['DOSSIER', 'ENTRETIEN', 'TEST']" placeholder="Type d’évaluation" />
              <div class="grid grid-cols-2 gap-3">
                <InputNumber v-model="evaluation.note" :min="0" :max="evaluation.noteMax" placeholder="Note" />
                <InputNumber v-model="evaluation.noteMax" :min="1" placeholder="Note maximale" />
              </div>
              <Select v-model="evaluation.appreciation" :options="['INSUFFISANTE', 'MOYENNE', 'BONNE', 'EXCELLENTE']" placeholder="Appréciation" />
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
