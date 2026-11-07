<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Card from 'primevue/card'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import { useAuthStore } from '@/shared/store/useAuthStore'
import { useNotificationStore } from '@/shared/store/useNotificationStore'
import { embaucheApi as api, ErreurApi } from '../api/embaucheApi'

const auth = useAuthStore()
const notifications = useNotificationStore()
const router = useRouter()
const route = useRoute()
const propositions = ref([])
const recherche = ref('')
const loading = ref(false)

const estCandidat = computed(() => auth.role === 'CANDIDAT')
const filtrees = computed(() => {
  const terme = recherche.value.trim().toLowerCase()
  if (!terme) return propositions.value
  return propositions.value.filter((proposition) => [
    proposition.candidat?.nom,
    proposition.candidat?.prenom,
    proposition.offre?.titre,
    proposition.statut,
  ].filter(Boolean).join(' ').toLowerCase().includes(terme))
})

async function charger() {
  loading.value = true
  try {
    propositions.value = await api.get('/embauche/propositions')
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Impossible de charger les propositions.')
  } finally {
    loading.value = false
  }
}

function ouvrir(proposition) {
  router.push({
    name: estCandidat.value ? 'candidat-proposition-fiche' : 'embauche-proposition-fiche',
    params: { idProposition: proposition.idPropositionEmbauche },
  })
}

onMounted(charger)
</script>

<template>
  <section>
    <header class="mb-5 flex flex-col justify-between gap-4 md:flex-row md:items-start">
      <div>
        <h1 class="m-0 text-2xl font-bold">{{ estCandidat ? 'Mes propositions d’embauche' : 'Propositions d’embauche' }}</h1>
        <p class="mt-1 mb-0 text-muted-color">
          {{ estCandidat ? 'Consultez les propositions reçues et répondez depuis leur fiche.' : 'Créez une proposition uniquement pour une candidature retenue.' }}
        </p>
      </div>
      <div class="flex flex-wrap gap-2">
        <Button label="Actualiser" icon="pi pi-refresh" severity="secondary" outlined :loading="loading" @click="charger" />
        <Button v-if="!estCandidat" label="Nouvelle proposition" icon="pi pi-plus" as="router-link" :to="{ name: 'embauche-proposition-saisie' }" />
      </div>
    </header>

    <Card>
      <template #content>
        <InputText v-model="recherche" class="mb-4 w-full md:w-80" placeholder="Rechercher une proposition" />
        <div v-if="loading" class="py-8 text-center text-muted-color">Chargement des propositions...</div>
        <EmptyState v-else-if="!filtrees.length" titre="Aucune proposition" message="Aucune proposition d’embauche ne correspond à votre recherche." />
        <DataTable v-else :value="filtrees" data-key="idPropositionEmbauche" row-hover paginator :rows="10" :rows-per-page-options="[10, 25, 50]">
          <Column v-if="!estCandidat" header="Candidat" sortable>
            <template #body="{ data }">
              <strong>{{ data.candidat?.prenom }} {{ data.candidat?.nom }}</strong>
              <div class="text-sm text-muted-color">{{ data.candidat?.email }}</div>
            </template>
          </Column>
          <Column header="Offre" sortable>
            <template #body="{ data }">{{ data.offre?.reference }} · {{ data.offre?.titre }}</template>
          </Column>
          <Column field="typeContrat" header="Contrat" />
          <Column field="salairePropose" header="Salaire" />
          <Column field="statut" header="Statut">
            <template #body="{ data }"><StatusBadge :valeur="data.statut" /></template>
          </Column>
          <Column header="Action">
            <template #body="{ data }">
              <Button label="Ouvrir la fiche" icon="pi pi-eye" size="small" outlined @click="ouvrir(data)" />
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>
  </section>
</template>
