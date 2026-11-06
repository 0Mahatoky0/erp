<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Select from 'primevue/select'
import TabView from 'primevue/tabview'
import TabPanel from 'primevue/tabpanel'
import Tag from 'primevue/tag'
import EmptyState from '@/shared/components/EmptyState.vue'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import { useNotificationStore } from '@/shared/store/useNotificationStore'
import { recrutementApi as api, ErreurApi } from '../api/recrutementApi'

const router = useRouter()
const notifications = useNotificationStore()
const offres = ref([])
const idOffre = ref('')
const candidaturesParStatut = ref({})
const statutActif = ref('RECUE')
const loading = ref(false)

const statuts = [
  { valeur: 'RECUE', libelle: 'Reçues' },
  { valeur: 'EN_ETUDE', libelle: 'En étude' },
  { valeur: 'PRESELECTIONNEE', libelle: 'Présélectionnées' },
  { valeur: 'EN_EVALUATION', libelle: 'En évaluation' },
  { valeur: 'LISTE_ATTENTE', libelle: 'Liste d’attente' },
  { valeur: 'RETENUE', libelle: 'Retenues' },
  { valeur: 'REFUSEE', libelle: 'Refusées' },
  { valeur: 'RETIREE', libelle: 'Retirées' },
]

const offresOptions = computed(() =>
  offres.value.map((offre) => ({
    ...offre,
    libelle: `${offre.reference} · ${offre.titre}`,
  })),
)

const candidaturesActives = computed(() => candidaturesParStatut.value[statutActif.value] || [])
const indexStatutActif = computed({
  get: () => statuts.findIndex((item) => item.valeur === statutActif.value),
  set: (index) => {
    if (statuts[index]) statutActif.value = statuts[index].valeur
  },
})

async function chargerCandidatures() {
  if (!idOffre.value) {
    candidaturesParStatut.value = {}
    return
  }
  loading.value = true
  try {
    const resultats = await Promise.all(
      statuts.map(async (statut) => [
        statut.valeur,
        await api.get(`/recrutement/offres/${idOffre.value}/candidatures?statut=${statut.valeur}`),
      ]),
    )
    candidaturesParStatut.value = Object.fromEntries(resultats)
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Impossible de charger les candidatures.')
  } finally {
    loading.value = false
  }
}

function ouvrirEvaluation(candidature) {
  router.push({
    name: 'recrutement-candidature-evaluation-fiche',
    params: { idOffre: idOffre.value, idCandidature: candidature.idCandidature },
    query: { statut: statutActif.value },
  })
}

watch(idOffre, chargerCandidatures)

onMounted(async () => {
  try {
    offres.value = await api.get('/recrutement/offres')
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Impossible de charger les offres.')
  }
})
</script>

<template>
  <section>
    <header class="mb-5 flex flex-col justify-between gap-4 md:flex-row md:items-start">
      <div>
        <p class="mb-1 text-xs font-semibold uppercase tracking-wider text-primary">Recrutement</p>
        <h1 class="m-0 text-2xl font-bold">Évaluation des candidatures</h1>
        <p class="mt-1 mb-0 text-muted-color">Sélectionnez une offre puis évaluez ses candidats par étape.</p>
      </div>
      <Button label="Actualiser" icon="pi pi-refresh" severity="secondary" outlined :loading="loading" @click="chargerCandidatures" />
    </header>

    <Card>
      <template #content>
        <Select v-model="idOffre" :options="offresOptions" option-label="libelle" option-value="idOffre" placeholder="Sélectionner une offre" filter class="mb-5 w-full md:w-1/2" />

        <EmptyState v-if="!idOffre" titre="Aucune offre sélectionnée" message="Choisissez une offre pour afficher ses candidatures." />
        <div v-else>
          <TabView v-model:activeIndex="indexStatutActif">
            <TabPanel v-for="statut in statuts" :key="statut.valeur">
              <template #header>
                <span class="flex items-center gap-2">
                  {{ statut.libelle }}
                  <Tag :value="String((candidaturesParStatut[statut.valeur] || []).length)" rounded severity="secondary" />
                </span>
              </template>
              <div v-if="loading" class="py-8 text-center text-muted-color">Chargement...</div>
              <EmptyState v-else-if="!candidaturesActives.length" titre="Aucune candidature" message="Aucun candidat dans cette étape." />
              <div v-else class="grid gap-3 md:grid-cols-2">
                <article v-for="candidature in candidaturesActives" :key="candidature.idCandidature" class="rounded-lg border border-surface p-4">
                  <div class="flex items-start justify-between gap-3">
                    <div>
                      <h2 class="m-0 text-lg font-semibold">{{ candidature.candidat?.prenom }} {{ candidature.candidat?.nom }}</h2>
                      <p class="m-0 text-sm text-muted-color">{{ candidature.candidat?.email }}</p>
                    </div>
                    <StatusBadge :valeur="candidature.statut" />
                  </div>
                  <Button class="mt-4" label="Ouvrir l’évaluation" icon="pi pi-file-edit" size="small" @click="ouvrirEvaluation(candidature)" />
                </article>
              </div>
            </TabPanel>
          </TabView>
        </div>
      </template>
    </Card>
  </section>
</template>
