<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import PrimeButton from 'primevue/button'
import PrimeInputText from 'primevue/inputtext'
import Card from 'primevue/card'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'

import StatusBadge from '@/shared/components/StatusBadge.vue'
import { useNotificationStore } from '@/shared/store/useNotificationStore'
import { useAuthStore } from '@/shared/store/useAuthStore'
import { formatDateHeure } from '@/shared/utils/format'
import OffreForm from '../components/OffreForm.vue'
import { recrutementApi as api, ErreurApi } from '../api/recrutementApi.js'

const auth = useAuthStore()
const notifications = useNotificationStore()
const route = useRoute()
const router = useRouter()

const offres = ref([])
const loading = ref(false)
const saving = ref(false)
const recherche = ref('')
const formOuvert = ref(false)

const peutGerer = computed(() => auth.estRh || auth.estDirection)

const offresFiltrees = computed(() => {
  const q = recherche.value.trim().toLowerCase()
  if (!q) return offres.value
  return offres.value.filter((o) =>
    [o.reference, o.titre, o.lieuTravail, o.typeContrat, o.statut]
      .filter(Boolean)
      .some((v) => String(v).toLowerCase().includes(q)),
  )
})

async function chargerOffres() {
  loading.value = true
  try {
    offres.value = await api.get('/recrutement/offres')
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Impossible de charger les offres.')
  } finally {
    loading.value = false
  }
}

function voirDetail(offre) {
  router.push({ name: 'recrutement-offre-detail', params: { idOffre: offre.idOffre ?? offre.id } })
}

async function enregistrer(payload) {
  saving.value = true
  try {
    await api.post('/recrutement/offres', payload)
    formOuvert.value = false
    notifications.succes('Offre créée.')
    await chargerOffres()
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Impossible d’enregistrer l’offre.')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await chargerOffres()
  // Arrivée depuis « Créer une offre » : ouvre directement le formulaire
  if (route.query.idDemandePoste && peutGerer.value) formOuvert.value = true
})
</script>

<template>
  <section>
    <header class="flex flex-col md:flex-row md:items-start justify-between gap-4 mb-6">
      <div>
        <h1 class="m-0 text-2xl font-bold">Offres d’emploi</h1>
        <p class="mt-1 mb-0 text-muted-color">Consultez les offres et ouvrez leur fiche pour les gérer.</p>
      </div>
      <div class="flex flex-wrap gap-2">
        <PrimeButton label="Actualiser" icon="pi pi-refresh" severity="secondary" outlined :loading="loading" @click="chargerOffres" />
        <PrimeButton v-if="peutGerer" label="Nouvelle offre" icon="pi pi-plus" @click="formOuvert = true" />
      </div>
    </header>

    <Card>
      <template #content>
        <div class="flex items-center justify-between gap-3 mb-4">
          <PrimeInputText v-model="recherche" placeholder="Rechercher une offre..." class="w-full md:w-80" />
          <span class="text-muted-color whitespace-nowrap">{{ offresFiltrees.length }} offre(s)</span>
        </div>

        <DataTable :value="offresFiltrees" :loading="loading" dataKey="idOffre" rowHover paginator :rows="10" :rowsPerPageOptions="[10, 25, 50]">
          <template #empty>
            <p class="text-center text-muted-color py-6 m-0">Aucune offre trouvée.</p>
          </template>

          <Column field="reference" header="Référence" sortable class="w-36">
            <template #body="{ data }"><strong>{{ data.reference }}</strong></template>
          </Column>
          <Column field="titre" header="Titre" sortable />
          <Column header="Lieu / Contrat">
            <template #body="{ data }">
              {{ data.lieuTravail || 'Lieu à définir' }}
              <span v-if="data.typeContrat" class="text-muted-color"> · {{ data.typeContrat }}</span>
            </template>
          </Column>
          <Column field="effectifRecherche" header="Effectif" sortable class="w-28" />
          <Column header="Date limite">
            <template #body="{ data }">
              {{ data.dateLimiteCandidature ? formatDateHeure(data.dateLimiteCandidature) : '—' }}
            </template>
          </Column>
          <Column field="statut" header="Statut" sortable>
            <template #body="{ data }"><StatusBadge :valeur="data.statut" /></template>
          </Column>
          <Column class="w-36">
            <template #body="{ data }">
              <PrimeButton label="Détail" icon="pi pi-eye" size="small" outlined @click="voirDetail(data)" />
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>

    <OffreForm
      v-if="formOuvert"
      :id-demande-poste="route.query.idDemandePoste ?? ''"
      :saving="saving"
      @save="enregistrer"
      @fermer="formOuvert = false"
    />
  </section>
</template>