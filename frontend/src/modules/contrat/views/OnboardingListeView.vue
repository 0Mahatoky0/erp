<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import Button from 'primevue/button'
import Card from 'primevue/card'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import ProgressBar from 'primevue/progressbar'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import { useNotificationStore } from '@/shared/store/useNotificationStore'
import { formatDate } from '@/shared/utils/format'
import { contratApi as api, ErreurApi } from '../api/contratApi'

const router = useRouter()
const notifications = useNotificationStore()

const parcours = ref([])
const employes = ref([])
const chargement = ref(false)

const employesParId = computed(() => {
  const map = new Map()
  for (const employe of employes.value) map.set(employe.id, employe)
  return map
})

const statistiques = computed(() => ({
  total: parcours.value.length,
  termines: parcours.value.filter((p) => p.statut === 'TERMINE').length,
  retard: parcours.value.filter((p) => p.statut !== 'TERMINE' && p.dateCible
    && new Date(p.dateCible) < new Date()).length,
}))

function nomEmploye(id) {
  const employe = employesParId.value.get(id)
  return employe ? `${employe.prenom || ''} ${employe.nom || ''}`.trim() : `Employé #${id}`
}

async function charger() {
  chargement.value = true
  try {
    const [listeParcours, listeEmployes] = await Promise.all([
      api.get('/contrats/onboarding'),
      api.get('/referentiel/employes'),
    ])
    parcours.value = listeParcours
    employes.value = listeEmployes
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Impossible de charger les onboardings.')
  } finally {
    chargement.value = false
  }
}

function ouvrir(parcours) {
  if (parcours.idContrat) {
    router.push({ name: 'contrat-detail', params: { id: parcours.idContrat } })
  }
}

onMounted(charger)
</script>

<template>
  <section>
    <header class="mb-5 flex flex-col justify-between gap-4 md:flex-row md:items-start">
      <div>
        <h1 class="m-0 text-2xl font-bold">Onboarding</h1>
        <p class="mt-1 mb-0 text-muted-color">
          Parcours d’intégration créés à la signature. La validation RH reste bloquée tant que les tâches bloquantes ne sont pas faites.
        </p>
      </div>
      <div class="flex flex-wrap gap-2">
        <Button label="Actualiser" icon="pi pi-refresh" severity="secondary" outlined :loading="chargement" @click="charger" />
        <Button label="Contrats" icon="pi pi-file-check" severity="secondary" outlined
          @click="router.push({ name: 'contrat-liste' })" />
      </div>
    </header>

    <div class="mb-5 grid grid-cols-1 gap-4 sm:grid-cols-3">
      <Card><template #content><div class="text-sm text-muted-color">Parcours</div><div class="text-2xl font-bold">{{ statistiques.total }}</div></template></Card>
      <Card><template #content><div class="text-sm text-muted-color">Terminés</div><div class="text-2xl font-bold">{{ statistiques.termines }}</div></template></Card>
      <Card><template #content><div class="text-sm text-muted-color">En retard</div><div class="text-2xl font-bold text-red-600">{{ statistiques.retard }}</div></template></Card>
    </div>

    <Card>
      <template #content>
        <div v-if="chargement" class="py-8 text-center text-muted-color">Chargement des parcours...</div>
        <EmptyState v-else-if="!parcours.length" titre="Aucun onboarding"
          message="Les parcours sont créés automatiquement à la signature d’un contrat." />

        <DataTable v-else :value="parcours" data-key="id" striped-rows row-hover paginator :rows="10"
          :rows-per-page-options="[10, 25, 50]" responsive-layout="scroll">
          <Column header="Salarié">
            <template #body="{ data }">
              <div class="font-medium">{{ nomEmploye(data.idEmploye) }}</div>
              <div class="text-xs text-muted-color">Contrat #{{ data.idContrat || '—' }}</div>
            </template>
          </Column>
          <Column header="Début">
            <template #body="{ data }">{{ formatDate(data.dateDebut) }}</template>
          </Column>
          <Column header="Cible">
            <template #body="{ data }">{{ formatDate(data.dateCible) }}</template>
          </Column>
          <Column header="Progression">
            <template #body="{ data }">
              <div class="flex min-w-40 items-center gap-2">
                <ProgressBar :value="data.progression || 0" :show-value="false" class="h-2 flex-1" />
                <span class="w-10 text-right text-xs">{{ data.progression || 0 }} %</span>
              </div>
            </template>
          </Column>
          <Column field="statut" header="Statut" sortable>
            <template #body="{ data }"><StatusBadge :valeur="data.statut" /></template>
          </Column>
          <Column header="Action">
            <template #body="{ data }">
              <Button label="Ouvrir" icon="pi pi-eye" size="small" outlined
                :disabled="!data.idContrat" @click="ouvrir(data)" />
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>
  </section>
</template>
