<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
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
const router = useRouter()
const notifications = useNotificationStore()
const contrats = ref([])
const recherche = ref('')
const loading = ref(false)
const estCandidat = computed(() => auth.role === 'CANDIDAT')
const filtres = computed(() => {
  const terme = recherche.value.trim().toLowerCase()
  if (!terme) return contrats.value
  return contrats.value.filter((contrat) =>
    [contrat.typeContrat, contrat.lieuTravail, contrat.statut]
      .filter(Boolean).join(' ').toLowerCase().includes(terme),
  )
})

async function charger() {
  loading.value = true
  try {
    contrats.value = await api.get('/embauche/propositions/contrats')
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Impossible de charger les contrats.')
  } finally {
    loading.value = false
  }
}

function ouvrir(contrat) {
  router.push({
    name: estCandidat.value ? 'candidat-contrat-fiche' : 'embauche-contrat-fiche',
    params: { idContrat: contrat.idContrat },
  })
}

const date = (valeur) => valeur ? new Date(valeur).toLocaleDateString('fr-FR') : '—'
onMounted(charger)
</script>

<template>
  <section>
    <header class="mb-5 flex flex-col justify-between gap-4 md:flex-row md:items-start">
      <div>
        <h1 class="m-0 text-2xl font-bold">{{ estCandidat ? 'Mes contrats proposés' : 'Contrats d’embauche' }}</h1>
        <p class="mt-1 mb-0 text-muted-color">Suivez l’état de chaque contrat proposé et les décisions reçues.</p>
      </div>
      <Button label="Actualiser" icon="pi pi-refresh" severity="secondary" outlined :loading="loading" @click="charger" />
    </header>
    <Card>
      <template #content>
        <InputText v-model="recherche" class="mb-4 w-full md:w-80" placeholder="Rechercher un contrat" />
        <div v-if="loading" class="py-8 text-center text-muted-color">Chargement des contrats...</div>
        <EmptyState v-else-if="!filtres.length" titre="Aucun contrat" message="Aucun contrat proposé ne correspond à votre recherche." />
        <DataTable v-else :value="filtres" data-key="idContrat" row-hover paginator :rows="10">
          <Column v-if="!estCandidat" header="Candidat">
            <template #body="{ data }">Candidat #{{ data.idCandidat }}</template>
          </Column>
          <Column field="typeContrat" header="Type" />
          <Column field="salaire" header="Salaire" />
          <Column field="lieuTravail" header="Lieu" />
          <Column header="Début"><template #body="{ data }">{{ date(data.dateDebut) }}</template></Column>
          <Column header="Statut"><template #body="{ data }"><StatusBadge :valeur="data.statut" /></template></Column>
          <Column header="Action">
            <template #body="{ data }">
              <Button label="Voir le contrat" icon="pi pi-eye" size="small" outlined @click="ouvrir(data)" />
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>
  </section>
</template>
