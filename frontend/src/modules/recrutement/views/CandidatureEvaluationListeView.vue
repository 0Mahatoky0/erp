<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Tab from 'primevue/tab'
import TabList from 'primevue/tablist'
import TabPanel from 'primevue/tabpanel'
import TabPanels from 'primevue/tabpanels'
import Tabs from 'primevue/tabs'
import Tag from 'primevue/tag'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import EmptyState from '@/shared/components/EmptyState.vue'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import { useNotificationStore } from '@/shared/store/useNotificationStore'
import { formatDateHeure } from '@/shared/utils/format'
import { recrutementApi as api, ErreurApi } from '../api/recrutementApi'
import { STATUTS_CANDIDATURE } from '../utils/statutsCandidature'

const notifications = useNotificationStore()
const route = useRoute()
const idOffre = computed(() => String(route.params.idOffre || ''))
const offre = ref(null)
const candidatures = ref([])
const statutActif = ref(String(route.query.statut || 'TOUTES'))
const loading = ref(false)

const onglets = [
  { valeur: 'TOUTES', libelle: 'Toutes' },
  ...STATUTS_CANDIDATURE.map((statut) => ({ valeur: statut.valeur, libelle: statut.libelle })),
]

const compter = (valeur) =>
  valeur === 'TOUTES'
    ? candidatures.value.length
    : candidatures.value.filter((candidature) => candidature.statut === valeur).length

const candidaturesPour = (valeur) =>
  valeur === 'TOUTES'
    ? candidatures.value
    : candidatures.value.filter((candidature) => candidature.statut === valeur)

function lienEvaluation(candidature) {
  return {
    name: 'recrutement-candidature-evaluation-fiche',
    params: { idOffre: idOffre.value, idCandidature: candidature.idCandidature },
    query: statutActif.value === 'TOUTES' ? undefined : { statut: statutActif.value },
  }
}

async function chargerCandidatures() {
  if (!idOffre.value) {
    candidatures.value = []
    return
  }
  loading.value = true
  try {
    const [offres, listeCandidatures] = await Promise.all([
      api.get('/recrutement/offres'),
      api.get(`/recrutement/offres/${idOffre.value}/candidatures`),
    ])
    offre.value = offres.find((item) => String(item.idOffre ?? item.id) === idOffre.value) || null
    candidatures.value = listeCandidatures
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Impossible de charger les candidatures.')
  } finally {
    loading.value = false
  }
}

watch([idOffre, () => route.query.statut], () => {
  const statutQuery = String(route.query.statut || 'TOUTES')
  statutActif.value = onglets.some((item) => item.valeur === statutQuery) ? statutQuery : 'TOUTES'
  chargerCandidatures()
}, { immediate: true })
</script>

<template>
  <section>
    <header class="mb-5 flex flex-col justify-between gap-4 md:flex-row md:items-start">
      <div>
        <p class="mb-1 text-xs font-semibold uppercase tracking-wider text-primary">Recrutement</p>
        <h1 class="m-0 text-2xl font-bold">Évaluation des candidatures</h1>
        <p class="mt-1 mb-0 text-muted-color">Évaluez les candidats de cette offre par étape.</p>
      </div>
      <Button
        label="Actualiser"
        icon="pi pi-refresh"
        severity="secondary"
        variant="outlined"
        :loading="loading"
        @click="chargerCandidatures"
      />
    </header>

    <Card>
      <template #content>
        <EmptyState
          v-if="!idOffre"
          titre="Aucune offre sélectionnée"
          message="Choisissez une offre pour afficher ses candidatures."
        />
        <div v-else>
          <div v-if="offre" class="mb-5 rounded-lg border border-surface bg-surface-50 p-4 dark:bg-surface-800">
            <div class="mb-3 flex flex-wrap items-center justify-between gap-2">
              <div>
                <p class="m-0 text-xs font-semibold uppercase tracking-wider text-primary">Offre évaluée</p>
                <h2 class="m-0 text-xl font-semibold">{{ offre.titre }}</h2>
              </div>
              <StatusBadge :valeur="offre.statut" />
            </div>
            <dl class="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
              <div>
                <dt class="text-xs text-muted-color">Référence</dt>
                <dd class="m-0 font-semibold">{{ offre.reference || '—' }}</dd>
              </div>
              <div>
                <dt class="text-xs text-muted-color">Type de contrat</dt>
                <dd class="m-0 font-semibold">{{ offre.typeContrat || '—' }}</dd>
              </div>
              <div>
                <dt class="text-xs text-muted-color">Lieu de travail</dt>
                <dd class="m-0 font-semibold">{{ offre.lieuTravail || '—' }}</dd>
              </div>
              <div>
                <dt class="text-xs text-muted-color">Effectif recherché</dt>
                <dd class="m-0 font-semibold">{{ offre.effectifRecherche ?? '—' }}</dd>
              </div>
            </dl>
          </div>

          <Tabs v-model:value="statutActif">
            <TabList>
              <Tab v-for="onglet in onglets" :key="onglet.valeur" :value="onglet.valeur">
                <span class="flex items-center gap-2">
                  {{ onglet.libelle }}
                  <Tag :value="String(compter(onglet.valeur))" rounded severity="secondary" />
                </span>
              </Tab>
            </TabList>
            <TabPanels>
              <TabPanel v-for="onglet in onglets" :key="onglet.valeur" :value="onglet.valeur">
                <DataTable
                  :value="candidaturesPour(onglet.valeur)"
                  data-key="idCandidature"
                  row-hover
                  paginator
                  :rows="10"
                  :rows-per-page-options="[10, 25, 50]"
                  :loading="loading"
                >
                  <Column header="Candidat" sortable sort-field="candidat.nom">
                    <template #body="{ data }">
                      <strong>{{ data.candidat?.prenom }} {{ data.candidat?.nom }}</strong>
                      <div class="text-sm text-muted-color">{{ data.candidat?.email }}</div>
                    </template>
                  </Column>
                  <Column field="statut" header="Statut" sortable>
                    <template #body="{ data }"><StatusBadge :valeur="data.statut" /></template>
                  </Column>
                  <Column field="dateCandidature" header="Reçue le" sortable>
                    <template #body="{ data }">{{ formatDateHeure(data.dateCandidature) }}</template>
                  </Column>
                  <Column header="Action">
                    <template #body="{ data }">
                      <Button
                        label="Détail"
                        icon="pi pi-eye"
                        size="small"
                        variant="outlined"
                        as="router-link"
                        :to="lienEvaluation(data)"
                      />
                    </template>
                  </Column>
                  <template #empty>
                    <p class="m-0 py-6 text-center text-muted-color">
                      Aucun candidat dans cette étape.
                    </p>
                  </template>
                </DataTable>
              </TabPanel>
            </TabPanels>
          </Tabs>
        </div>
      </template>
    </Card>
  </section>
</template>
