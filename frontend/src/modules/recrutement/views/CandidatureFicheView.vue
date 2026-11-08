<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import Message from 'primevue/message'
import Tag from 'primevue/tag'
import Textarea from 'primevue/textarea'
import Timeline from 'primevue/timeline'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import { useNotificationStore } from '@/shared/store/useNotificationStore'
import { recrutementApi as api, ErreurApi } from '../api/recrutementApi'
import { TRANSITIONS_CANDIDATURE, libelleStatut } from '../utils/statutsCandidature'

const route = useRoute()
const notifications = useNotificationStore()
const candidature = ref(null)
const historique = ref([])
const documents = ref([])
const entretiens = ref([])
const passages = ref([])
const evaluations = ref([])
const loading = ref(false)
const motifDialog = ref(false)
const motif = ref('')
const motifManquant = ref(false)

const idCandidature = computed(() => route.params.idCandidature)
const actions = computed(() => TRANSITIONS_CANDIDATURE[candidature.value?.statut] || [])

async function charger() {
  loading.value = true
  try {
    const [detail, historiqueResultat, documentsResultat, entretiensResultat, passagesResultat, evaluationsResultat] = await Promise.all([
      api.get(`/recrutement/candidatures/${idCandidature.value}`),
      api.get(`/recrutement/candidatures/${idCandidature.value}/historique`),
      api.get(`/recrutement/candidatures/${idCandidature.value}/documents`),
      api.get(`/recrutement/candidatures/${idCandidature.value}/entretiens`),
      api.get(`/recrutement/candidatures/${idCandidature.value}/passages-tests`),
      api.get(`/recrutement/candidatures/${idCandidature.value}/evaluations`),
    ])
    candidature.value = detail
    historique.value = historiqueResultat
    documents.value = documentsResultat
    entretiens.value = entretiensResultat
    passages.value = passagesResultat
    evaluations.value = evaluationsResultat
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Impossible de charger la candidature.')
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

onMounted(charger)
</script>

<template>
  <section>
    <Button
      label="Retour aux candidatures"
      icon="pi pi-arrow-left"
      text
      severity="secondary"
      class="mb-2"
      as="router-link"
      :to="{ name: 'recrutement-candidature-liste' }"
    />
    <div v-if="loading" class="py-8 text-center text-muted-color">Chargement de la fiche...</div>
    <EmptyState v-else-if="!candidature" titre="Candidature introuvable" message="Cette candidature n’est plus disponible." />
    <template v-else>
      <header class="mb-5 flex flex-col justify-between gap-4 md:flex-row md:items-start">
        <div>
          <p class="mb-1 text-xs font-semibold uppercase tracking-wider text-primary">Fiche candidature</p>
          <h1 class="m-0 text-2xl font-bold">{{ candidature.candidat?.prenom }} {{ candidature.candidat?.nom }}</h1>
          <p class="mt-1 text-muted-color">{{ candidature.candidat?.email }} · {{ candidature.offre?.titre }}</p>
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

      <div class="grid grid-cols-1 gap-5 lg:grid-cols-2">
        <Card>
          <template #title>Parcours</template>
          <template #content>
            <Timeline v-if="historique.length" :value="historique" align="left">
              <template #content="{ item }">
                <Tag :value="libelleStatut(item.nouveauStatut)" />
                <p class="m-0 mt-1 text-sm text-muted-color">{{ item.commentaire || 'Changement de statut' }}</p>
              </template>
            </Timeline>
            <EmptyState v-else titre="Aucun historique" message="Les étapes apparaîtront ici." />
          </template>
        </Card>

        <Card>
          <template #title>Profil candidat</template>
          <template #content>
            <dl class="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <div><dt class="text-sm text-muted-color">Nom</dt><dd class="font-semibold">{{ candidature.candidat?.nom }}</dd></div>
              <div><dt class="text-sm text-muted-color">Prénom</dt><dd class="font-semibold">{{ candidature.candidat?.prenom }}</dd></div>
              <div><dt class="text-sm text-muted-color">Email</dt><dd class="font-semibold">{{ candidature.candidat?.email }}</dd></div>
              <div><dt class="text-sm text-muted-color">Source</dt><dd class="font-semibold">{{ candidature.source || '—' }}</dd></div>
            </dl>
            <p class="mt-4 whitespace-pre-line">{{ candidature.messageCandidat || 'Aucun message.' }}</p>
          </template>
        </Card>

        <Card>
          <template #title>Entretiens et tests</template>
          <template #content>
            <p class="text-sm">{{ entretiens.length }} entretien(s) · {{ passages.length }} passage(s) de test</p>
            <DataTable v-if="entretiens.length" :value="entretiens" class="mt-3" size="small">
              <Column field="typeEntretien" header="Type" />
              <Column field="statut" header="Statut" />
              <Column field="dateDebut" header="Date" />
            </DataTable>
          </template>
        </Card>

        <Card>
          <template #title>Évaluations et documents</template>
          <template #content>
            <p>{{ evaluations.length }} évaluation(s) · {{ documents.length }} document(s)</p>
          </template>
        </Card>
      </div>

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
